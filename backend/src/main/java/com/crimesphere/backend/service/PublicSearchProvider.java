package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.public_search.PublicSearchRequest;
import com.crimesphere.backend.dto.public_search.PublicSearchResponse;
import com.crimesphere.backend.entity.PublicSearchType;

/**
 * Strategy interface for external public search providers (NewsAPI, GNews, Web, etc.)
 */
public interface PublicSearchProvider {

    String getProviderName();

    boolean supportsSearchType(PublicSearchType searchType);

    PublicSearchResponse searchNews(PublicSearchRequest request);

    PublicSearchResponse searchWeb(PublicSearchRequest request);
}
