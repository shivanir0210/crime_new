package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.public_search.PublicSearchRequest;
import com.crimesphere.backend.dto.public_search.PublicSearchResponse;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PublicSearchHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class PublicSearchService {

    private static final Logger logger = LoggerFactory.getLogger(PublicSearchService.class);

    private final CaseRepository caseRepository;
    private final PublicSearchHistoryRepository searchHistoryRepository;
    private final AuditLogRepository auditLogRepository;
    private final NewsApiSearchProvider newsApiSearchProvider;
    private final MockPublicSearchProvider mockPublicSearchProvider;

    @Value("${crimesphere.public-search.provider:newsapi}")
    private String configuredProvider = "newsapi";

    @Value("${PUBLIC_SEARCH_MOCK_ENABLED:${crimesphere.public-search.mock-enabled:false}}")
    private boolean mockEnabled = false;

    @Value("${crimesphere.public-search.rate-limit-per-minute:20}")
    private int rateLimitPerMinute = 20;

    @Value("${crimesphere.public-search.cache-minutes:5}")
    private int cacheMinutes = 5;

    // In-memory rate limiting map: Officer UUID -> RateLimitBucket
    private final Map<UUID, RateLimitBucket> rateLimitMap = new ConcurrentHashMap<>();

    // In-memory response cache: cacheKey -> CachedResult
    private final Map<String, CachedResult> responseCache = new ConcurrentHashMap<>();

    public PublicSearchService(CaseRepository caseRepository,
                               PublicSearchHistoryRepository searchHistoryRepository,
                               AuditLogRepository auditLogRepository,
                               NewsApiSearchProvider newsApiSearchProvider,
                               MockPublicSearchProvider mockPublicSearchProvider) {
        this.caseRepository = caseRepository;
        this.searchHistoryRepository = searchHistoryRepository;
        this.auditLogRepository = auditLogRepository;
        this.newsApiSearchProvider = newsApiSearchProvider;
        this.mockPublicSearchProvider = mockPublicSearchProvider;
    }

    /**
     * Executes an authenticated public information search.
     * Validates input, checks rate limiting, enforces station authorization if caseId is given,
     * checks short-lived cache, queries provider, and records audit logs & search history.
     */
    @Transactional
    public PublicSearchResponse searchPublicInformation(PublicSearchRequest request, PoliceOfficer officer) {
        // 1. Validate query input
        validateSearchRequest(request);

        // 2. Enforce per-officer rate limiting
        enforceRateLimit(officer.getId());

        // 3. Verify station-level case authorization if caseId is provided
        Case crimeCase = null;
        if (request.getCaseId() != null) {
            crimeCase = caseRepository.findById(request.getCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + request.getCaseId()));
            validateStationAccess(officer, crimeCase);
        }

        // 4. Record audit log: PUBLIC_SEARCH_REQUESTED
        recordAuditLog(officer, "PUBLIC_SEARCH_REQUESTED", "SUCCESS", "Query: " + sanitizeForLog(request.getQuery()));

        // 5. Check cache
        String cacheKey = buildCacheKey(request);
        CachedResult cached = responseCache.get(cacheKey);
        if (cached != null && !cached.isExpired(cacheMinutes)) {
            logger.info("Serving public search from cache for key: {}", cacheKey);
            recordSearchHistory(crimeCase, officer, request, cached.response.getProvider(), cached.response.getTotalResults());
            return cached.response;
        }

        // 6. Select provider
        PublicSearchProvider provider = selectProvider();
        String providerLogName = "newsapi".equalsIgnoreCase(provider.getProviderName()) ? "NEWS_API" : "MOCK";
        logger.info("Executing public search using provider: {}", providerLogName);

        // 7. Execute search
        PublicSearchResponse response;
        try {
            if (request.getSearchType() == PublicSearchType.WEB) {
                if (!provider.supportsSearchType(PublicSearchType.WEB)) {
                    throw new UnsupportedOperationException("Web search is not supported by provider '" + provider.getProviderName() + "'. Please select NEWS search.");
                }
                response = provider.searchWeb(request);
            } else {
                response = provider.searchNews(request);
            }

            // Put in cache
            responseCache.put(cacheKey, new CachedResult(response, Instant.now()));

            // 8. Record search history & audit log: PUBLIC_SEARCH_COMPLETED
            recordSearchHistory(crimeCase, officer, request, provider.getProviderName(), response.getTotalResults());
            recordAuditLog(officer, "PUBLIC_SEARCH_COMPLETED", "SUCCESS",
                    "Provider: " + provider.getProviderName() + ", Results: " + response.getTotalResults());

            return response;

        } catch (Exception ex) {
            recordAuditLog(officer, "PUBLIC_SEARCH_FAILED", "FAILED", "Reason: " + sanitizeForLog(ex.getMessage()));
            throw ex;
        }
    }

    private void validateSearchRequest(PublicSearchRequest request) {
        if (request == null || request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            throw new IllegalArgumentException("Search query is required.");
        }
        String trimmed = request.getQuery().trim();
        if (trimmed.length() < 3) {
            throw new IllegalArgumentException("Search query must be at least 3 characters in length.");
        }
        if (trimmed.length() > 300) {
            throw new IllegalArgumentException("Search query cannot exceed 300 characters.");
        }
        if (request.getDateFrom() != null && !request.getDateFrom().isBlank() &&
            request.getDateTo() != null && !request.getDateTo().isBlank()) {
            try {
                java.time.LocalDate fromDate = java.time.LocalDate.parse(request.getDateFrom().trim());
                java.time.LocalDate toDate = java.time.LocalDate.parse(request.getDateTo().trim());
                if (fromDate.isAfter(toDate)) {
                    throw new IllegalArgumentException("From-date cannot be later than To-date.");
                }
            } catch (java.time.format.DateTimeParseException ex) {
                throw new IllegalArgumentException("Invalid date filter format. Please provide valid YYYY-MM-DD dates.");
            }
        }
    }

    private void enforceRateLimit(UUID officerId) {
        if (officerId == null) return;
        RateLimitBucket bucket = rateLimitMap.compute(officerId, (id, existing) -> {
            Instant now = Instant.now();
            if (existing == null || now.isAfter(existing.resetTime)) {
                return new RateLimitBucket(now.plusSeconds(60), new AtomicInteger(1));
            }
            existing.counter.incrementAndGet();
            return existing;
        });

        if (bucket.counter.get() > rateLimitPerMinute) {
            logger.warn("Officer {} exceeded public search rate limit ({} requests/minute)", officerId, rateLimitPerMinute);
            throw new IllegalStateException("Public search rate limit exceeded (" + rateLimitPerMinute + " requests/minute). Please wait before trying again.");
        }
    }

    private void validateStationAccess(PoliceOfficer officer, Case crimeCase) {
        if ("ADMIN".equalsIgnoreCase(officer.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(officer.getRole())) {
            return;
        }
        if (officer.getStation() == null || crimeCase.getStation() == null ||
                !officer.getStation().getId().equals(crimeCase.getStation().getId())) {
            throw new AccessDeniedException("Access denied: Case record belongs to another police station.");
        }
    }

    public void setMockEnabled(boolean mockEnabled) {
        this.mockEnabled = mockEnabled;
    }

    public void setConfiguredProvider(String configuredProvider) {
        this.configuredProvider = configuredProvider;
    }

    private PublicSearchProvider selectProvider() {
        if (mockEnabled || "mock".equalsIgnoreCase(configuredProvider)) {
            logger.info("Public search provider selected: MOCK");
            return mockPublicSearchProvider;
        }
        if (newsApiSearchProvider != null && newsApiSearchProvider.isConfigured()) {
            logger.info("Public search provider selected: NEWS_API");
            return newsApiSearchProvider;
        }
        logger.error("Public search provider selection failed: NEWS_API_KEY is not configured while mock mode is disabled.");
        throw new IllegalStateException("Public search provider is not configured. Please configure NEWS_API_KEY.");
    }

    private void recordSearchHistory(Case crimeCase, PoliceOfficer officer, PublicSearchRequest request, String provider, int resultCount) {
        try {
            PublicSearchHistory history = new PublicSearchHistory(
                    null,
                    crimeCase,
                    officer,
                    request.getQuery().trim(),
                    request.getSearchType(),
                    provider,
                    resultCount
            );
            searchHistoryRepository.save(history);
        } catch (Exception e) {
            logger.warn("Failed to record public search history: {}", e.getMessage());
        }
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String status, String details) {
        try {
            AuditLog log = new AuditLog(null, officer, action, status, null, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }

    private String buildCacheKey(PublicSearchRequest req) {
        return (mockEnabled ? "mock" : "real") +
                ":" + (req.getSearchType() != null ? req.getSearchType().name() : "NEWS") +
                ":" + req.getQuery().trim().toLowerCase() +
                ":" + req.getPage() +
                ":" + req.getPageSize() +
                ":" + (req.getLanguage() != null ? req.getLanguage() : "en") +
                ":" + (req.getDateFrom() != null ? req.getDateFrom() : "") +
                ":" + (req.getDateTo() != null ? req.getDateTo() : "");
    }

    private String sanitizeForLog(String str) {
        if (str == null) return "";
        return str.length() > 60 ? str.substring(0, 60) + "..." : str;
    }

    // Helper classes for rate-limiting and caching
    private static class RateLimitBucket {
        final Instant resetTime;
        final AtomicInteger counter;

        RateLimitBucket(Instant resetTime, AtomicInteger counter) {
            this.resetTime = resetTime;
            this.counter = counter;
        }
    }

    private static class CachedResult {
        final PublicSearchResponse response;
        final Instant cachedAt;

        CachedResult(PublicSearchResponse response, Instant cachedAt) {
            this.response = response;
            this.cachedAt = cachedAt;
        }

        boolean isExpired(int minutes) {
            return Instant.now().isAfter(cachedAt.plusSeconds(minutes * 60L));
        }
    }
}
