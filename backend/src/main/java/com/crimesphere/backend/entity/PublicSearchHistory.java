package com.crimesphere.backend.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "public_search_history")
public class PublicSearchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "case_id")
    private Case crimeCase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "searched_by", nullable = false)
    private PoliceOfficer searchedBy;

    @Column(name = "query", nullable = false, columnDefinition = "TEXT")
    private String query;

    @Enumerated(EnumType.STRING)
    @Column(name = "search_type", nullable = false, length = 50)
    private PublicSearchType searchType;

    @Column(name = "provider", length = 100)
    private String provider;

    @Column(name = "result_count")
    private Integer resultCount = 0;

    @Column(name = "created_at", insertable = false, updatable = false)
    private OffsetDateTime createdAt;

    public PublicSearchHistory() {
    }

    public PublicSearchHistory(UUID id, Case crimeCase, PoliceOfficer searchedBy, String query,
                               PublicSearchType searchType, String provider, Integer resultCount) {
        this.id = id;
        this.crimeCase = crimeCase;
        this.searchedBy = searchedBy;
        this.query = query;
        this.searchType = searchType;
        this.provider = provider;
        this.resultCount = resultCount;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Case getCrimeCase() {
        return crimeCase;
    }

    public void setCrimeCase(Case crimeCase) {
        this.crimeCase = crimeCase;
    }

    public PoliceOfficer getSearchedBy() {
        return searchedBy;
    }

    public void setSearchedBy(PoliceOfficer searchedBy) {
        this.searchedBy = searchedBy;
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

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public void setResultCount(Integer resultCount) {
        this.resultCount = resultCount;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
