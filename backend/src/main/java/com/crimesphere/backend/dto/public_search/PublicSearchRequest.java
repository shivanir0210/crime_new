package com.crimesphere.backend.dto.public_search;

import com.crimesphere.backend.entity.PublicSearchType;

import java.util.UUID;

public class PublicSearchRequest {

    private String query;
    private UUID caseId;
    private PublicSearchType searchType = PublicSearchType.NEWS;
    private String dateFrom;
    private String dateTo;
    private String language = "en";
    private int page = 1;
    private int pageSize = 10;

    public PublicSearchRequest() {
    }

    public PublicSearchRequest(String query, UUID caseId, PublicSearchType searchType, String dateFrom,
                               String dateTo, String language, int page, int pageSize) {
        this.query = query;
        this.caseId = caseId;
        this.searchType = searchType != null ? searchType : PublicSearchType.NEWS;
        this.dateFrom = dateFrom;
        this.dateTo = dateTo;
        this.language = language != null && !language.isBlank() ? language : "en";
        this.page = page > 0 ? page : 1;
        this.pageSize = pageSize > 0 && pageSize <= 20 ? pageSize : 10;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public void setCaseId(UUID caseId) {
        this.caseId = caseId;
    }

    public PublicSearchType getSearchType() {
        return searchType != null ? searchType : PublicSearchType.NEWS;
    }

    public void setSearchType(PublicSearchType searchType) {
        this.searchType = searchType != null ? searchType : PublicSearchType.NEWS;
    }

    public String getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(String dateFrom) {
        this.dateFrom = dateFrom;
    }

    public String getDateTo() {
        return dateTo;
    }

    public void setDateTo(String dateTo) {
        this.dateTo = dateTo;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public int getPage() {
        return page > 0 ? page : 1;
    }

    public void setPage(int page) {
        this.page = page > 0 ? page : 1;
    }

    public int getPageSize() {
        return pageSize > 0 && pageSize <= 20 ? pageSize : 10;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize > 0 && pageSize <= 20 ? pageSize : 10;
    }
}
