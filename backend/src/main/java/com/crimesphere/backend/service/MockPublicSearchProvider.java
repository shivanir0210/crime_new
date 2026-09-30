package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.public_search.PublicSearchRequest;
import com.crimesphere.backend.dto.public_search.PublicSearchResponse;
import com.crimesphere.backend.dto.public_search.PublicSearchResultDTO;
import com.crimesphere.backend.entity.PublicSearchType;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MockPublicSearchProvider implements PublicSearchProvider {

    @Override
    public String getProviderName() {
        return "mock";
    }

    @Override
    public boolean supportsSearchType(PublicSearchType searchType) {
        return true;
    }

    @Override
    public PublicSearchResponse searchNews(PublicSearchRequest request) {
        return generateMockResults(request, PublicSearchType.NEWS);
    }

    @Override
    public PublicSearchResponse searchWeb(PublicSearchRequest request) {
        return generateMockResults(request, PublicSearchType.WEB);
    }

    private PublicSearchResponse generateMockResults(PublicSearchRequest request, PublicSearchType type) {
        List<PublicSearchResultDTO> results = new ArrayList<>();
        String q = request.getQuery();

        PublicSearchResultDTO item1 = new PublicSearchResultDTO();
        item1.setId(UUID.randomUUID().toString());
        item1.setTitle("[DEVELOPMENT MOCK DATA] Local Daily Reports Police Investigation Into " + q);
        item1.setDescription("[DEVELOPMENT MOCK DATA] This is a simulated article generated in development mock mode for testing query: " + q);
        item1.setUrl("https://example-news.internal/articles/simulated-" + UUID.randomUUID().toString().substring(0, 8));
        item1.setSourceName("Local Daily Gazette [MOCK]");
        item1.setPublishedAt(OffsetDateTime.now().minusDays(2).toString());
        item1.setAuthor("Staff Reporter [MOCK]");
        item1.setImageUrl("https://example-news.internal/images/mock-thumbnail.jpg");
        item1.setSearchType(type);
        item1.setQuery(q);
        item1.setRetrievedAt(OffsetDateTime.now());
        item1.setRelevanceIndicator("DEVELOPMENT MOCK DATA");
        results.add(item1);

        PublicSearchResultDTO item2 = new PublicSearchResultDTO();
        item2.setId(UUID.randomUUID().toString());
        item2.setTitle("[DEVELOPMENT MOCK DATA] Regional Media Briefing on " + q);
        item2.setDescription("[DEVELOPMENT MOCK DATA] Simulated public press release mentioning incident proceedings and public safety advisories.");
        item2.setUrl("https://regional-tribune.internal/news/mock-" + UUID.randomUUID().toString().substring(0, 8));
        item2.setSourceName("Regional Tribune [MOCK]");
        item2.setPublishedAt(OffsetDateTime.now().minusDays(5).toString());
        item2.setAuthor(null);
        item2.setImageUrl(null);
        item2.setSearchType(type);
        item2.setQuery(q);
        item2.setRetrievedAt(OffsetDateTime.now());
        item2.setRelevanceIndicator("DEVELOPMENT MOCK DATA");
        results.add(item2);

        return new PublicSearchResponse(q, type, results, results.size(), OffsetDateTime.now(), "mock");
    }
}
