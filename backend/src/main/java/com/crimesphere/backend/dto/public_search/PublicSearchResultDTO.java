package com.crimesphere.backend.dto.public_search;

import com.crimesphere.backend.entity.PublicSearchType;

import java.time.OffsetDateTime;

public class PublicSearchResultDTO {

    private String id;
    private String title;
    private String description;
    private String url;
    private String sourceName;
    private String publishedAt;
    private String author;
    private String imageUrl;
    private PublicSearchType searchType;
    private String query;
    private OffsetDateTime retrievedAt;
    private String relevanceIndicator;

    public PublicSearchResultDTO() {
    }

    public PublicSearchResultDTO(String id, String title, String description, String url, String sourceName,
                                 String publishedAt, String author, String imageUrl, PublicSearchType searchType,
                                 String query, OffsetDateTime retrievedAt, String relevanceIndicator) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.url = url;
        this.sourceName = sourceName;
        this.publishedAt = publishedAt;
        this.author = author;
        this.imageUrl = imageUrl;
        this.searchType = searchType;
        this.query = query;
        this.retrievedAt = retrievedAt;
        this.relevanceIndicator = relevanceIndicator;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(String publishedAt) {
        this.publishedAt = publishedAt;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public PublicSearchType getSearchType() {
        return searchType;
    }

    public void setSearchType(PublicSearchType searchType) {
        this.searchType = searchType;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }

    public OffsetDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(OffsetDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public String getRelevanceIndicator() {
        return relevanceIndicator;
    }

    public void setRelevanceIndicator(String relevanceIndicator) {
        this.relevanceIndicator = relevanceIndicator;
    }
}
