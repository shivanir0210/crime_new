package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.ai.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class AiServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(AiServiceClient.class);

    private final RestClient restClient;
    private final String aiServiceUrl;
    private final String aiApiKey;

    public AiServiceClient(
            @Value("${ai.service.url:http://localhost:8000}") String aiServiceUrl,
            @Value("${ai.service.api-key:crime-sphere-internal-ai-key-2026-prod}") String aiApiKey,
            @Value("${ai.service.timeout-ms:30000}") int timeoutMs
    ) {
        this.aiServiceUrl = aiServiceUrl;
        this.aiApiKey = aiApiKey;

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(timeoutMs);
        requestFactory.setReadTimeout(timeoutMs);

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory)
                .defaultHeader("X-AI-Service-Key", aiApiKey)
                .build();

        logger.info("AiServiceClient initialized with URL: {} (Timeout: {}ms)", aiServiceUrl, timeoutMs);
    }

    /**
     * Check FastAPI AI Service health status.
     * Safely returns status without raising unhandled exceptions or crashing Spring Boot.
     */
    public Map<String, Object> checkHealth() {
        try {
            Map<String, Object> response = restClient.get()
                    .uri("/api/health")
                    .accept(MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && "UP".equalsIgnoreCase(String.valueOf(response.get("status")))) {
                return Map.of(
                        "status", "UP",
                        "service", "CrimeSphere AI Integration",
                        "aiService", "UP"
                );
            }
            return Map.of(
                    "status", "PARTIAL",
                    "service", "CrimeSphere AI Integration",
                    "aiService", "DOWN"
            );
        } catch (Exception e) {
            logger.warn("AI Service health check failed against {}: {}", aiServiceUrl, e.getMessage());
            return Map.of(
                    "status", "PARTIAL",
                    "service", "CrimeSphere AI Integration",
                    "aiService", "DOWN",
                    "error", "Unable to connect to AI Service: " + e.getMessage()
            );
        }
    }

    /**
     * Transmit structured case analysis request to FastAPI AI Service.
     */
    public AiCaseAnalysisResponse sendCaseAnalysis(AiCaseAnalysisRequest request) {
        try {
            logger.info("Dispatching case analysis request for case: {} to AI Service", request.getCaseNumber());

            return restClient.post()
                    .uri("/api/analysis/case")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        int statusCode = resp.getStatusCode().value();
                        String statusText = resp.getStatusText();
                        logger.error("AI Service returned HTTP {}: {}", statusCode, statusText);

                        if (statusCode == 401) {
                            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "AI Service authentication failed (401 Unauthorized)");
                        } else if (statusCode == 422) {
                            String responseBody = "";
                            try {
                                responseBody = new String(resp.getBody().readAllBytes());
                            } catch (Exception ignored) {}
                            logger.error("AI Service rejected analysis request (422 Unprocessable Content): {}", responseBody);
                            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "AI service rejected the analysis request.");
                        } else if (statusCode == 504) {
                            throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI service request timed out.");
                        } else {
                            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service returned error: " + statusText);
                        }
                    })
                    .body(AiCaseAnalysisResponse.class);

        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            Throwable rootCause = e.getCause();
            if (rootCause instanceof SocketTimeoutException || (e.getMessage() != null && e.getMessage().contains("timed out"))) {
                logger.error("Timeout connecting to AI Service for case {}: {}", request.getCaseNumber(), e.getMessage());
                throw new ResponseStatusException(HttpStatus.GATEWAY_TIMEOUT, "AI service request timed out.");
            }
            logger.error("Failed to connect to AI Service for case {}: {}", request.getCaseNumber(), e.getMessage());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "AI service is currently unavailable.");
        }
    }

    /**
     * Send text chunks to FastAPI for embedding generation.
     */
    public List<AiKnowledgeChunkDto> indexChunks(AiIndexCaseRequest request) {
        try {
            logger.info("Requesting embedding generation for {} chunks from AI Service", request.getChunks().size());

            return restClient.post()
                    .uri("/api/retrieval/index")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorMsg = String.format("AI Service indexing returned HTTP %s: %s",
                                resp.getStatusCode(), resp.getStatusText());
                        logger.error("AI Service indexing error: {}", errorMsg);
                        throw new IllegalStateException(errorMsg);
                    })
                    .body(new ParameterizedTypeReference<List<AiKnowledgeChunkDto>>() {});

        } catch (Exception e) {
            logger.error("Failed to index chunks with AI Service: {}", e.getMessage());
            throw new IllegalStateException("AI Service indexing failure: " + e.getMessage(), e);
        }
    }

    /**
     * Perform semantic vector search on candidate chunks with query.
     */
    public AiRetrievalResponse searchRetrieval(AiRetrievalRequest request, List<AiKnowledgeChunkDto> candidateChunks) {
        try {
            Map<String, Object> payload = Map.of(
                    "case_id", request.getCaseId(),
                    "query", request.getQuery(),
                    "top_k", request.getTopK(),
                    "min_similarity", request.getMinSimilarity(),
                    "candidates", candidateChunks != null ? candidateChunks : Collections.emptyList()
            );

            return restClient.post()
                    .uri("/api/retrieval/search")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (req, resp) -> {
                        String errorMsg = String.format("AI Service retrieval returned HTTP %s: %s",
                                resp.getStatusCode(), resp.getStatusText());
                        logger.error("AI Service retrieval error: {}", errorMsg);
                        throw new IllegalStateException(errorMsg);
                    })
                    .body(AiRetrievalResponse.class);

        } catch (Exception e) {
            logger.error("Failed to perform vector retrieval with AI Service: {}", e.getMessage());
            throw new IllegalStateException("AI Service retrieval failure: " + e.getMessage(), e);
        }
    }

    /**
     * Send text strings to FastAPI for dense vector embedding generation.
     */
    public List<List<Float>> generateEmbeddings(List<String> texts) {
        try {
            Map<String, Object> payload = Map.of("texts", texts != null ? texts : Collections.emptyList());

            Map<String, Object> resp = restClient.post()
                    .uri("/api/retrieval/embed")
                    .contentType(MediaType.APPLICATION_JSON)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (resp != null && resp.containsKey("embeddings")) {
                @SuppressWarnings("unchecked")
                List<List<Double>> raw = (List<List<Double>>) resp.get("embeddings");
                return raw.stream()
                        .map(list -> list.stream().map(Double::floatValue).toList())
                        .toList();
            }
            return Collections.emptyList();
        } catch (Exception e) {
            logger.error("Failed to generate embeddings from AI Service: {}", e.getMessage());
            throw new IllegalStateException("Embedding generation failure: " + e.getMessage(), e);
        }
    }

    public String getAiServiceUrl() {
        return aiServiceUrl;
    }
}
