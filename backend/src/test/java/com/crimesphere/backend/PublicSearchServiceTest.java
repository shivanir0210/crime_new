package com.crimesphere.backend;

import com.crimesphere.backend.dto.public_search.*;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.repository.*;
import com.crimesphere.backend.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.OffsetDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PublicSearchServiceTest {

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PublicSearchHistoryRepository searchHistoryRepository;

    @Mock
    private PublicInformationItemRepository itemRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private FIRRepository firRepository;

    private TestNewsApiSearchProvider testNewsApiProvider;
    private MockPublicSearchProvider mockProvider;
    private PublicSearchService publicSearchService;
    private PublicInformationService publicInformationService;
    private CaseSearchQueryService caseSearchQueryService;

    private PoliceStation stationA;
    private PoliceStation stationB;
    private PoliceOfficer officerStationA;
    private PoliceOfficer officerStationB;
    private Case caseStationA;

    // Test double for NewsApiSearchProvider
    private static class TestNewsApiSearchProvider extends NewsApiSearchProvider {
        private boolean configured = true;
        private boolean shouldFail = false;
        private PublicSearchResponse responseToReturn;

        public void setConfigured(boolean configured) {
            this.configured = configured;
        }

        public void setShouldFail(boolean shouldFail) {
            this.shouldFail = shouldFail;
        }

        public void setResponseToReturn(PublicSearchResponse responseToReturn) {
            this.responseToReturn = responseToReturn;
        }

        @Override
        public boolean isConfigured() {
            return configured;
        }

        @Override
        public PublicSearchResponse searchNews(PublicSearchRequest request) {
            if (!configured) {
                throw new IllegalStateException("Public search provider is not configured. Please configure PUBLIC_SEARCH_API_KEY.");
            }
            if (shouldFail) {
                throw new RuntimeException("Public information search is temporarily unavailable: Connection timed out");
            }
            if (responseToReturn != null) {
                return responseToReturn;
            }
            List<PublicSearchResultDTO> items = new ArrayList<>();
            PublicSearchResultDTO item = new PublicSearchResultDTO(
                    UUID.randomUUID().toString(),
                    "Coimbatore Police Probe Unidentified Incident",
                    "A formal investigation has commenced in Coimbatore district.",
                    "https://thehindu.example.com/news/national/coimbatore-police",
                    "The Hindu",
                    OffsetDateTime.now().toString(),
                    "Staff Reporter",
                    "https://thehindu.example.com/thumb.jpg",
                    PublicSearchType.NEWS,
                    request.getQuery(),
                    OffsetDateTime.now(),
                    "PUBLIC_NEWS"
            );
            items.add(item);
            return new PublicSearchResponse(request.getQuery(), PublicSearchType.NEWS, items, 1, OffsetDateTime.now(), "newsapi");
        }
    }

    @BeforeEach
    void setUp() {
        stationA = new PoliceStation();
        stationA.setId(UUID.randomUUID());
        stationA.setStationName("B2 R.S. Puram Police Station");
        stationA.setDistrict("Coimbatore");

        stationB = new PoliceStation();
        stationB.setId(UUID.randomUUID());
        stationB.setStationName("C1 Central Police Station");
        stationB.setDistrict("Chennai");

        officerStationA = new PoliceOfficer();
        officerStationA.setId(UUID.randomUUID());
        officerStationA.setPoliceId("TN-POL-1001");
        officerStationA.setFullName("Inspector K. Raman");
        officerStationA.setStation(stationA);
        officerStationA.setRole("STATION_OFFICER");

        officerStationB = new PoliceOfficer();
        officerStationB.setId(UUID.randomUUID());
        officerStationB.setPoliceId("TN-POL-2002");
        officerStationB.setFullName("Sub-Inspector M. Selvam");
        officerStationB.setStation(stationB);
        officerStationB.setRole("STATION_OFFICER");

        caseStationA = new Case();
        caseStationA.setId(UUID.randomUUID());
        caseStationA.setCaseNumber("CS-2026-0001");
        caseStationA.setTitle("R.S. Puram Jewelry Heist Investigation");
        caseStationA.setCaseType("THEFT");
        caseStationA.setStation(stationA);
        caseStationA.setPriority("HIGH");
        caseStationA.setStatus("OPEN");
        caseStationA.setCreatedAt(OffsetDateTime.now());

        testNewsApiProvider = new TestNewsApiSearchProvider();
        mockProvider = new MockPublicSearchProvider();

        publicSearchService = new PublicSearchService(
                caseRepository,
                searchHistoryRepository,
                auditLogRepository,
                testNewsApiProvider,
                mockProvider
        );
        publicSearchService.setMockEnabled(false);

        publicInformationService = new PublicInformationService(
                caseRepository,
                itemRepository,
                auditLogRepository
        );

        caseSearchQueryService = new CaseSearchQueryService(firRepository);
    }

    @Test
    @DisplayName("1. Public News Search Success — Normalizes results with mandatory disclaimer")
    void testSearchNews_Success_ProviderNormalization() {
        PublicSearchRequest req = new PublicSearchRequest();
        req.setQuery("Coimbatore robbery 2026");
        req.setSearchType(PublicSearchType.NEWS);

        PublicSearchResponse response = publicSearchService.searchPublicInformation(req, officerStationA);

        assertNotNull(response);
        assertEquals(PublicSearchType.NEWS, response.getSearchType());
        assertEquals("newsapi", response.getProvider());
        assertFalse(response.getResults().isEmpty());

        PublicSearchResultDTO item = response.getResults().get(0);
        assertEquals("Coimbatore Police Probe Unidentified Incident", item.getTitle());
        assertEquals("The Hindu", item.getSourceName());
        assertNotNull(item.getRetrievedAt());
        assertNotNull(response.getWarningDisclaimer());
        assertTrue(response.getWarningDisclaimer().contains("Requires Verification"));

        verify(searchHistoryRepository, times(1)).save(any(PublicSearchHistory.class));
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("2. Search Validation — Rejects query less than 3 chars and over 300 chars")
    void testSearchValidation_QueryLengthLimits() {
        PublicSearchRequest shortReq = new PublicSearchRequest();
        shortReq.setQuery("ab");

        IllegalArgumentException shortEx = assertThrows(IllegalArgumentException.class, () ->
                publicSearchService.searchPublicInformation(shortReq, officerStationA));
        assertTrue(shortEx.getMessage().contains("at least 3 characters"));

        PublicSearchRequest longReq = new PublicSearchRequest();
        longReq.setQuery("a".repeat(301));

        IllegalArgumentException longEx = assertThrows(IllegalArgumentException.class, () ->
                publicSearchService.searchPublicInformation(longReq, officerStationA));
        assertTrue(longEx.getMessage().contains("cannot exceed 300 characters"));
    }

    @Test
    @DisplayName("3. Search Provider Failure — Throws controlled error when provider is unconfigured")
    void testSearchProviderFailure_UnconfiguredGracefulError() {
        testNewsApiProvider.setConfigured(false);

        PublicSearchRequest req = new PublicSearchRequest();
        req.setQuery("Coimbatore investigation");

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                publicSearchService.searchPublicInformation(req, officerStationA));

        assertTrue(ex.getMessage().contains("Public search provider is not configured"));
        verify(auditLogRepository, atLeastOnce()).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("4. Search Timeout — Returns controlled error without leaking internal secrets")
    void testSearchProviderFailure_TimeoutControlled() {
        testNewsApiProvider.setShouldFail(true);

        PublicSearchRequest req = new PublicSearchRequest();
        req.setQuery("Coimbatore missing person");

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                publicSearchService.searchPublicInformation(req, officerStationA));

        assertTrue(ex.getMessage().contains("Public information search is temporarily unavailable"));
        assertFalse(ex.getMessage().contains("secret"));
    }

    @Test
    @DisplayName("5. Station Authorization — Denies search on Case A by officer from Station B")
    void testStationAuthorization_CaseAccessDenied() {
        when(caseRepository.findById(caseStationA.getId())).thenReturn(Optional.of(caseStationA));

        PublicSearchRequest req = new PublicSearchRequest();
        req.setQuery("Jewelry shop Coimbatore");
        req.setCaseId(caseStationA.getId());

        assertThrows(AccessDeniedException.class, () ->
                publicSearchService.searchPublicInformation(req, officerStationB));

        verify(searchHistoryRepository, never()).save(any(PublicSearchHistory.class));
    }

    @Test
    @DisplayName("6. Save Public Information — Successfully bookmarks item with default status UNVERIFIED")
    void testSavePublicInformation_DefaultUnverified() {
        when(caseRepository.findById(caseStationA.getId())).thenReturn(Optional.of(caseStationA));
        when(itemRepository.findByCrimeCaseIdAndSourceUrl(eq(caseStationA.getId()), anyString())).thenReturn(Optional.empty());
        when(itemRepository.save(any(PublicInformationItem.class))).thenAnswer(inv -> {
            PublicInformationItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        SavePublicInformationRequest saveReq = new SavePublicInformationRequest(
                "Gold ornament shop burglary suspect spotted in CCTV",
                "Local report detailing burglary timing and escape direction.",
                "Coimbatore Mail",
                "https://coimbatoremail.example.com/news/burglary-heist",
                OffsetDateTime.now().minusDays(1),
                "City Correspondent",
                "https://coimbatoremail.example.com/cctv.jpg",
                "Coimbatore jewelry robbery",
                "Investigator noted location matches FIR scene."
        );

        PublicInformationResponse response = publicInformationService.savePublicInformation(caseStationA.getId(), saveReq, officerStationA);

        assertNotNull(response);
        assertEquals(PublicInformationVerificationStatus.UNVERIFIED, response.getVerificationStatus());
        assertEquals("Gold ornament shop burglary suspect spotted in CCTV", response.getTitle());
        assertEquals("Coimbatore Mail", response.getSourceName());
        assertNotNull(response.getWarningDisclaimer());

        verify(itemRepository, times(1)).save(any(PublicInformationItem.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("7. Dangerous URL Rejection — Rejects javascript: and data: schemes")
    void testDangerousUrl_Rejected() {
        when(caseRepository.findById(caseStationA.getId())).thenReturn(Optional.of(caseStationA));

        SavePublicInformationRequest jsReq = new SavePublicInformationRequest(
                "Suspicious Malicious Link", "desc", "Evil News",
                "javascript:alert('pwned')", OffsetDateTime.now(), "Hacker", null, "q", null
        );

        IllegalArgumentException exJs = assertThrows(IllegalArgumentException.class, () ->
                publicInformationService.savePublicInformation(caseStationA.getId(), jsReq, officerStationA));
        assertTrue(exJs.getMessage().contains("Only secure HTTP and HTTPS"));

        SavePublicInformationRequest dataReq = new SavePublicInformationRequest(
                "Data URI Link", "desc", "Evil News",
                "data:text/html,<script>alert(1)</script>", OffsetDateTime.now(), "Hacker", null, "q", null
        );

        IllegalArgumentException exData = assertThrows(IllegalArgumentException.class, () ->
                publicInformationService.savePublicInformation(caseStationA.getId(), dataReq, officerStationA));
        assertTrue(exData.getMessage().contains("Only secure HTTP and HTTPS"));

        verify(itemRepository, never()).save(any(PublicInformationItem.class));
    }

    @Test
    @DisplayName("8. Verification Workflow — Enforces mandatory notes for CORROBORATED, CONTRADICTED, NOT_RELEVANT")
    void testVerificationWorkflow_NotesMandatoryForCorroborated() {
        when(caseRepository.findById(caseStationA.getId())).thenReturn(Optional.of(caseStationA));

        PublicInformationItem item = new PublicInformationItem(
                UUID.randomUUID(), caseStationA, "Sample News", "Desc", "News",
                "https://example.com/news", OffsetDateTime.now(), "Author", null, "q",
                PublicInformationVerificationStatus.UNVERIFIED, null, officerStationA
        );

        when(itemRepository.findByIdAndCrimeCaseId(item.getId(), caseStationA.getId()))
                .thenReturn(Optional.of(item));

        // Attempt CORROBORATED without notes -> must fail
        UpdatePublicInformationRequest emptyNotesReq = new UpdatePublicInformationRequest(
                PublicInformationVerificationStatus.CORROBORATED, "   "
        );

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                publicInformationService.updatePublicInformationItem(caseStationA.getId(), item.getId(), emptyNotesReq, officerStationA));
        assertTrue(ex.getMessage().contains("mandatory when marking public information as CORROBORATED"));

        // Attempt CORROBORATED with valid notes -> succeeds
        UpdatePublicInformationRequest validReq = new UpdatePublicInformationRequest(
                PublicInformationVerificationStatus.CORROBORATED, "Verified with official station log and timestamp."
        );
        when(itemRepository.save(any(PublicInformationItem.class))).thenAnswer(inv -> inv.getArgument(0));

        PublicInformationResponse updated = publicInformationService.updatePublicInformationItem(
                caseStationA.getId(), item.getId(), validReq, officerStationA);

        assertEquals(PublicInformationVerificationStatus.CORROBORATED, updated.getVerificationStatus());
        assertEquals("Verified with official station log and timestamp.", updated.getNotes());
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("9. Deletion — Removes item and records PUBLIC_INFORMATION_DELETED audit log")
    void testDeletePublicInformation_SuccessWithAuditLog() {
        when(caseRepository.findById(caseStationA.getId())).thenReturn(Optional.of(caseStationA));

        PublicInformationItem item = new PublicInformationItem(
                UUID.randomUUID(), caseStationA, "News To Delete", "Desc", "News",
                "https://example.com/delete", OffsetDateTime.now(), "Author", null, "q",
                PublicInformationVerificationStatus.UNVERIFIED, null, officerStationA
        );

        when(itemRepository.findByIdAndCrimeCaseId(item.getId(), caseStationA.getId()))
                .thenReturn(Optional.of(item));

        publicInformationService.deletePublicInformationItem(caseStationA.getId(), item.getId(), officerStationA);

        verify(itemRepository, times(1)).delete(item);
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("10. Case Search Query Suggestions — Adheres strictly to data minimization")
    void testCaseSearchQuerySuggestions_DataMinimization() {
        FIR fir = new FIR();
        fir.setId(UUID.randomUUID());
        fir.setIncidentLocation("Cross Cut Road, Gandhipuram, Coimbatore");
        fir.setOffenceType("Armed Robbery");
        fir.setIncidentDate(OffsetDateTime.now().minusDays(10));
        fir.setComplainantName("Secret Informant Name");
        fir.setComplainantContact("9876543210");

        when(firRepository.findByCaseEntityId(caseStationA.getId())).thenReturn(Optional.of(fir));

        SuggestedQueriesResponse response = caseSearchQueryService.generateSuggestedQueries(caseStationA);

        assertNotNull(response);
        assertFalse(response.getSuggestedQueries().isEmpty());

        for (String query : response.getSuggestedQueries()) {
            assertFalse(query.contains("Secret Informant Name"), "Must not leak complainant name!");
            assertFalse(query.contains("9876543210"), "Must not leak phone number!");
            assertFalse(query.contains("Cross Cut Road"), "Must not leak specific street address!");
            assertTrue(query.toLowerCase().contains("coimbatore"));
        }
    }

    @Test
    @DisplayName("11. NewsAPI Response Parsing — Handles empty results cleanly")
    void testNewsApiResponseParsing_EmptyResults() {
        NewsApiSearchProvider provider = new NewsApiSearchProvider();
        String json = """
        {
          "status": "ok",
          "totalResults": 0,
          "articles": []
        }
        """;
        PublicSearchResponse res = provider.parseNewsApiResponse("test query", json);
        assertNotNull(res);
        assertEquals(0, res.getTotalResults());
        assertTrue(res.getResults().isEmpty());
    }

    @Test
    @DisplayName("12. NewsAPI Response Parsing — Throws controlled exception on provider error status")
    void testNewsApiResponseParsing_ErrorStatus() {
        NewsApiSearchProvider provider = new NewsApiSearchProvider();
        String json = """
        {
          "status": "error",
          "code": "apiKeyInvalid",
          "message": "Your API key is invalid or has expired."
        }
        """;
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                provider.parseNewsApiResponse("test query", json));
        assertTrue(ex.getMessage().contains("API key is invalid"));
    }

    @Test
    @DisplayName("13. NewsAPI Response Parsing — Safely handles missing fields and filters removed articles")
    void testNewsApiResponseParsing_MissingFieldsAndRemovedFilter() {
        NewsApiSearchProvider provider = new NewsApiSearchProvider();
        String json = """
        {
          "status": "ok",
          "totalResults": 2,
          "articles": [
            {
              "source": { "id": null, "name": "The Hindu" },
              "author": null,
              "title": null,
              "description": null,
              "url": "https://thehindu.example.com/news/1",
              "urlToImage": null,
              "publishedAt": "2026-09-14T10:00:00Z"
            },
            {
              "source": { "name": "Removed Source" },
              "title": "[Removed]",
              "url": "https://removed.com"
            }
          ]
        }
        """;
        PublicSearchResponse res = provider.parseNewsApiResponse("coimbatore", json);
        assertNotNull(res);
        assertEquals(1, res.getResults().size());
        PublicSearchResultDTO item = res.getResults().get(0);
        assertEquals("Untitled Article", item.getTitle());
        assertEquals("The Hindu", item.getSourceName());
        assertNull(item.getDescription());
        assertNull(item.getImageUrl());
    }

    @Test
    @DisplayName("14. Mock Mode — Selects MockPublicSearchProvider when PUBLIC_SEARCH_MOCK_ENABLED=true")
    void testMockMode_Enabled() {
        publicSearchService.setMockEnabled(true);

        PublicSearchRequest req = new PublicSearchRequest();
        req.setQuery("Mock test query");

        PublicSearchResponse res = publicSearchService.searchPublicInformation(req, officerStationA);
        assertNotNull(res);
        assertEquals("mock", res.getProvider());
        assertFalse(res.getResults().isEmpty());
        assertTrue(res.getResults().get(0).getTitle().contains("[DEVELOPMENT MOCK DATA]"));
    }

    @Test
    @DisplayName("15. Save Mock Public Information — Bookmarks mock search result with UNVERIFIED status and audit log")
    void testSaveMockPublicInformation_Success() {
        when(caseRepository.findById(caseStationA.getId())).thenReturn(Optional.of(caseStationA));
        when(itemRepository.findByCrimeCaseIdAndSourceUrl(eq(caseStationA.getId()), anyString())).thenReturn(Optional.empty());
        when(itemRepository.save(any(PublicInformationItem.class))).thenAnswer(inv -> {
            PublicInformationItem item = inv.getArgument(0);
            item.setId(UUID.randomUUID());
            return item;
        });

        SavePublicInformationRequest mockSaveReq = new SavePublicInformationRequest(
                "[DEVELOPMENT MOCK DATA] Local Daily Reports Police Investigation Into Theft",
                "[DEVELOPMENT MOCK DATA] Simulated article generated in development mock mode.",
                "Local Daily Gazette [MOCK]",
                "https://example-news.internal/articles/simulated-87654321",
                OffsetDateTime.now().minusDays(2),
                "Staff Reporter [MOCK]",
                "https://example-news.internal/images/mock-thumbnail.jpg",
                "coimbatore jewelry heist",
                "Saved for investigator evaluation."
        );

        PublicInformationResponse response = publicInformationService.savePublicInformation(caseStationA.getId(), mockSaveReq, officerStationA);

        assertNotNull(response);
        assertEquals(PublicInformationVerificationStatus.UNVERIFIED, response.getVerificationStatus());
        assertEquals("Local Daily Gazette [MOCK]", response.getSourceName());
        assertEquals("https://example-news.internal/articles/simulated-87654321", response.getSourceUrl());

        verify(itemRepository, times(1)).save(any(PublicInformationItem.class));
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }
}
