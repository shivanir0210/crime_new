package com.crimesphere.backend.dto.public_search;

import com.crimesphere.backend.entity.PublicSearchType;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

public class PublicSearchResponse {

    private String query;
    private PublicSearchType searchType;
    private List<PublicSearchResultDTO> results = new ArrayList<>();
    private int totalResults;
    private OffsetDateTime retrievedAt;
    private String provider;
    private boolean mocked;
    private String mockMessage;
    private String warningDisclaimer = "Public Information — Requires Verification. External public news/web results are NOT evidence, do not establish guilt or innocence, and must be independently verified before being relied upon in an investigation.";

    public PublicSearchResponse() {
        this.retrievedAt = OffsetDateTime.now();
    }

    public PublicSearchResponse(String query, PublicSearchType searchType, List<PublicSearchResultDTO> results,
                                int totalResults, OffsetDateTime retrievedAt, String provider) {
        this.query = query;
        this.searchType = searchType;
        this.results = results != null ? results : new ArrayList<>();
        this.totalResults = totalResults;
        this.retrievedAt = retrievedAt != null ? retrievedAt : OffsetDateTime.now();
        this.provider = provider;
        this.mocked = "mock".equalsIgnoreCase(provider);
        if (this.mocked) {
            this.mockMessage = "DEVELOPMENT MOCK — PUBLIC NEWS NOT FETCHED";
        }
    }

    public boolean isMocked() {
        return mocked || "mock".equalsIgnoreCase(provider);
    }

    public void setMocked(boolean mocked) {
        this.mocked = mocked;
    }

    public String getMockMessage() {
        return mockMessage;
    }

    public void setMockMessage(String mockMessage) {
        this.mockMessage = mockMessage;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public PublicSearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(PublicSearchType searchType) {
        this.searchType = searchType;
    }

    public List<PublicSearchResultDTO> getResults() {
        return results;
    }

    public void setResults(List<PublicSearchResultDTO> results) {
        this.results = results != null ? results : new ArrayList<>();
    }

    public int getTotalResults() {
        return totalResults;
    }

    public void setTotalResults(int totalResults) {
        this.totalResults = totalResults;
    }

    public OffsetDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(OffsetDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getWarningDisclaimer() {
        return warningDisclaimer;
    }

    public void setWarningDisclaimer(String warningDisclaimer) {
        this.warningDisclaimer = warningDisclaimer;
    }
}
