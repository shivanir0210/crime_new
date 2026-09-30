package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.public_search.PublicSearchRequest;
import com.crimesphere.backend.dto.public_search.PublicSearchResponse;
import com.crimesphere.backend.dto.public_search.PublicSearchResultDTO;
import com.crimesphere.backend.entity.PublicSearchType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class NewsApiSearchProvider implements PublicSearchProvider {

    private static final Logger logger = LoggerFactory.getLogger(NewsApiSearchProvider.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${crimesphere.public-search.api-key:}")
    private String apiKey;

    @Value("${crimesphere.public-search.base-url:https://newsapi.org/v2}")
    private String baseUrl;

    @Value("${crimesphere.public-search.timeout-seconds:15}")
    private int timeoutSeconds;

    @Value("${crimesphere.public-search.max-results:10}")
    private int maxResults;

    public NewsApiSearchProvider() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(15000);
        factory.setReadTimeout(15000);
        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public boolean isConfigured() {
        if (apiKey == null) return false;
        String trimmed = apiKey.trim();
        return !trimmed.isEmpty() && 
               !"replace-with-your-key".equalsIgnoreCase(trimmed) && 
               !"YOUR_NEWS_API_KEY".equalsIgnoreCase(trimmed) &&
               !"YOUR_REAL_NEWSAPI_KEY_HERE".equalsIgnoreCase(trimmed) &&
               !"REPLACE_WITH_REAL_NEWSAPI_KEY".equalsIgnoreCase(trimmed);
    }

    @Override
    public String getProviderName() {
        return "newsapi";
    }

    @Override
    public boolean supportsSearchType(PublicSearchType searchType) {
        return searchType == PublicSearchType.NEWS;
    }

    @Override
    public PublicSearchResponse searchNews(PublicSearchRequest request) {
        if (!isConfigured()) {
            throw new IllegalStateException("News API request failed. Please check the API key, provider configuration, network connection, or rate limit.");
        }

        int pageSize = Math.min(request.getPageSize() > 0 ? request.getPageSize() : maxResults, 20);
        int page = Math.max(request.getPage(), 1);

        String language = (request.getLanguage() != null && !request.getLanguage().isBlank()) ? request.getLanguage() : "en";

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromHttpUrl(baseUrl.endsWith("/") ? baseUrl + "everything" : baseUrl + "/everything")
                .queryParam("q", request.getQuery())
                .queryParam("language", language)
                .queryParam("sortBy", "relevancy")
                .queryParam("apiKey", apiKey)
                .queryParam("pageSize", pageSize)
                .queryParam("page", page);

        if (request.getDateFrom() != null && !request.getDateFrom().isBlank()) {
            uriBuilder.queryParam("from", request.getDateFrom());
        }
        if (request.getDateTo() != null && !request.getDateTo().isBlank()) {
            uriBuilder.queryParam("to", request.getDateTo());
        }

        String url = uriBuilder.toUriString();

        try {
            logger.info("Public search provider selected: NEWS_API | Mock mode: false | API key present: {}", isConfigured());
            logger.info("News API request started for query: {}", sanitizeQueryForLog(request.getQuery()));

            org.springframework.http.ResponseEntity<String> responseEntity = restClient.get()
                    .uri(url)
                    .retrieve()
                    .toEntity(String.class);

            int statusCode = responseEntity.getStatusCode().value();
            logger.info("News API response status: {}", statusCode);

            String responseBody = responseEntity.getBody();
            PublicSearchResponse response = parseNewsApiResponse(request.getQuery(), responseBody);
            logger.info("News API articles received: {}", response.getResults().size());
            return response;

        } catch (org.springframework.web.client.HttpStatusCodeException httpEx) {
            String errBody = httpEx.getResponseBodyAsString();
            String message = "NewsAPI request failed with status " + httpEx.getStatusCode().value();
            if (httpEx.getStatusCode().value() == 401 || httpEx.getStatusCode().value() == 403) {
                message = "NewsAPI authentication failed. Please verify the configured API key.";
            } else if (errBody != null && !errBody.isBlank()) {
                try {
                    JsonNode errJson = objectMapper.readTree(errBody);
                    if (errJson.has("message") && !errJson.get("message").isNull()) {
                        message = errJson.get("message").asText();
                    }
                } catch (Exception e) {
                    // Ignore JSON parse error on error body
                }
            }
            logger.error("NewsAPI HTTP failure ({}): {}", httpEx.getStatusCode(), sanitizeErrorMessage(message));
            throw new RuntimeException("Public news search failed: " + sanitizeErrorMessage(message));
        } catch (Exception ex) {
            if (ex instanceof RuntimeException re && (re.getMessage() != null && re.getMessage().startsWith("Public"))) {
                throw re;
            }
            logger.error("External NewsAPI search request failed: {}", ex.getMessage());
            throw new RuntimeException("Public information search is temporarily unavailable: " + sanitizeErrorMessage(ex.getMessage()));
        }
    }

    @Override
    public PublicSearchResponse searchWeb(PublicSearchRequest request) {
        throw new UnsupportedOperationException("Web search is not supported by the current provider (NewsAPI). Please select NEWS search.");
    }

    public PublicSearchResponse parseNewsApiResponse(String query, String responseBody) {
        try {
            if (responseBody == null || responseBody.isBlank()) {
                return new PublicSearchResponse(query, PublicSearchType.NEWS, List.of(), 0, OffsetDateTime.now(), "newsapi");
            }

            JsonNode root = objectMapper.readTree(responseBody);
            String status = root.path("status").asText("");

            if (!"ok".equalsIgnoreCase(status)) {
                String errorMsg = root.path("message").asText("Unknown provider error");
                throw new RuntimeException(sanitizeErrorMessage(errorMsg));
            }

            int totalResults = root.path("totalResults").asInt(0);
            JsonNode articlesNode = root.path("articles");
            List<PublicSearchResultDTO> results = new ArrayList<>();

            if (articlesNode != null && articlesNode.isArray()) {
                for (JsonNode item : articlesNode) {
                    String title = (item.path("title").isNull() || item.path("title").asText().isBlank())
                            ? "Untitled Article" : item.path("title").asText().trim();
                    String articleUrl = (item.path("url").isNull()) ? "" : item.path("url").asText().trim();

                    if (articleUrl.isEmpty() || "[Removed]".equalsIgnoreCase(title) || articleUrl.contains("removed.com")) {
                        continue;
                    }

                    PublicSearchResultDTO dto = new PublicSearchResultDTO();
                    dto.setId(UUID.randomUUID().toString());
                    dto.setTitle(title);

                    String desc = (item.path("description").isNull() || item.path("description").asText().isBlank())
                            ? null : item.path("description").asText().trim();
                    dto.setDescription(desc);
                    dto.setUrl(articleUrl);

                    JsonNode sourceNode = item.path("source");
                    String sourceName = (sourceNode.path("name").isNull() || sourceNode.path("name").asText().isBlank())
                            ? "Unknown Source" : sourceNode.path("name").asText().trim();
                    dto.setSourceName(sourceName);

                    String publishedAt = (item.path("publishedAt").isNull() || item.path("publishedAt").asText().isBlank())
                            ? null : item.path("publishedAt").asText().trim();
                    dto.setPublishedAt(publishedAt);

                    String author = (item.path("author").isNull() || item.path("author").asText().isBlank())
                            ? null : item.path("author").asText().trim();
                    dto.setAuthor(author);

                    String imageUrl = (item.path("urlToImage").isNull() || item.path("urlToImage").asText().isBlank())
                            ? null : item.path("urlToImage").asText().trim();
                    dto.setImageUrl(imageUrl);

                    dto.setSearchType(PublicSearchType.NEWS);
                    dto.setQuery(query);
                    dto.setRetrievedAt(OffsetDateTime.now());
                    dto.setRelevanceIndicator("PUBLIC_NEWS");

                    results.add(dto);
                }
            }

            return new PublicSearchResponse(query, PublicSearchType.NEWS, results, totalResults, OffsetDateTime.now(), "newsapi");

        } catch (Exception ex) {
            if (ex instanceof RuntimeException re) throw re;
            logger.error("Failed to parse NewsAPI JSON response: {}", ex.getMessage());
            throw new RuntimeException("Failed to process public news search results: " + sanitizeErrorMessage(ex.getMessage()));
        }
    }

    private String sanitizeQueryForLog(String query) {
        if (query == null) return "";
        return query.length() > 50 ? query.substring(0, 50) + "..." : query;
    }

    private String sanitizeErrorMessage(String msg) {
        if (msg == null) return "Unexpected provider error";
        if (apiKey != null && !apiKey.isBlank() && msg.contains(apiKey)) {
            return msg.replace(apiKey, "[REDACTED]");
        }
        return msg;
    }
}
