package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.ai.AiKnowledgeChunkDto;
import com.crimesphere.backend.dto.ai.AiRetrievalRequest;
import com.crimesphere.backend.dto.ai.AiRetrievalResponse;
import com.crimesphere.backend.dto.ai.AiRetrievalResultItem;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AiRetrievalService {

    private static final Logger logger = LoggerFactory.getLogger(AiRetrievalService.class);

    private final CaseRepository caseRepository;
    private final FIRRepository firRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceGapRepository evidenceGapRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiServiceClient aiServiceClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AiRetrievalService(
            CaseRepository caseRepository,
            FIRRepository firRepository,
            EvidenceRepository evidenceRepository,
            EvidenceGapRepository evidenceGapRepository,
            KnowledgeChunkRepository knowledgeChunkRepository,
            PoliceOfficerRepository officerRepository,
            AuditLogRepository auditLogRepository,
            AiServiceClient aiServiceClient
    ) {
        this.caseRepository = caseRepository;
        this.firRepository = firRepository;
        this.evidenceRepository = evidenceRepository;
        this.evidenceGapRepository = evidenceGapRepository;
        this.knowledgeChunkRepository = knowledgeChunkRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
        this.aiServiceClient = aiServiceClient;
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    public boolean isPgvectorAvailable() {
        try {
            if (entityManager == null) return false;
            Object result = entityManager.createNativeQuery("SELECT count(1) FROM pg_extension WHERE extname = 'vector'").getSingleResult();
            return result != null && ((Number) result).longValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional(readOnly = true)
    public AiRetrievalResponse retrieveSimilarKnowledge(UUID caseId, String queryOverride, int topK, double minSimilarity, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (!isPgvectorAvailable()) {
            recordAuditLog(officer, "AI_RETRIEVAL_FAILED", "PostgreSQL pgvector extension unavailable on server.");
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "PostgreSQL pgvector extension is not installed or enabled on the server."
            );
        }

        // 1. Construct or use investigation query
        String query = (queryOverride != null && !queryOverride.trim().isEmpty())
                ? queryOverride.trim()
                : buildRetrievalQuery(parentCase);

        logger.info("Executing vector similarity retrieval for case {} with query: '{}'",
                parentCase.getCaseNumber(), query);

        recordAuditLog(officer, "AI_RETRIEVAL_REQUESTED",
                String.format("Retrieval query initiated for case %s: %s", parentCase.getCaseNumber(), query));

        // 2. Load candidate chunks allowed under retrieval policy and station isolation
        UUID stationId = parentCase.getStation() != null ? parentCase.getStation().getId() : null;
        List<KnowledgeChunk> candidateEntities = knowledgeChunkRepository.findRetrievableChunksForStation(caseId, stationId);

        List<AiKnowledgeChunkDto> candidateDtos = candidateEntities.stream().map(kc -> {
            AiKnowledgeChunkDto dto = new AiKnowledgeChunkDto();
            dto.setId(kc.getId());
            dto.setSourceType(kc.getSourceType());
            dto.setSourceId(kc.getSourceId());
            dto.setCaseId(kc.getCaseEntity().getId());
            dto.setCaseNumber(kc.getCaseEntity().getCaseNumber());
            dto.setCaseType(kc.getCaseEntity().getCaseType());
            dto.setContent(kc.getContent());
            dto.setContentHash(kc.getContentHash());
            Map<String, Object> meta = new HashMap<>();
            meta.put("embedding_json", kc.getEmbedding());
            if (kc.getMetadata() != null && !kc.getMetadata().isBlank()) {
                try {
                    Map<String, Object> parsedMeta = objectMapper.readValue(kc.getMetadata(), new TypeReference<Map<String, Object>>() {});
                    meta.putAll(parsedMeta);
                } catch (Exception e) {
                    logger.warn("Failed to parse metadata JSON for chunk {}: {}", kc.getId(), e.getMessage());
                }
            }
            dto.setMetadata(meta);
            return dto;
        }).collect(Collectors.toList());

        // 3. Delegate semantic ranking to FastAPI
        AiRetrievalRequest request = new AiRetrievalRequest(caseId, query, topK > 0 ? topK : 5, minSimilarity > 0 ? minSimilarity : 0.70);
        AiRetrievalResponse response = aiServiceClient.searchRetrieval(request, candidateDtos);

        if (response == null) {
            response = new AiRetrievalResponse(query, candidateDtos.size(), 0, 0.0, Collections.emptyList());
        }

        // 4. Mark current vs historical case tags and clean metadata
        for (AiRetrievalResultItem item : response.getResults()) {
            boolean isCurrent = item.getCaseId() != null && item.getCaseId().equals(caseId);
            item.setCurrentCase(isCurrent);
            if (item.getMetadata() != null) {
                item.getMetadata().remove("embedding_json");
            }
        }

        recordAuditLog(officer, "RETRIEVAL_COMPLETED",
                String.format("Retrieval completed for case %s: returned %d relevant items (Top Similarity: %.2f)",
                        parentCase.getCaseNumber(), response.getReturnedCount(), response.getTopSimilarity()));

        return response;
    }

    public String buildRetrievalQuery(Case parentCase) {
        StringBuilder sb = new StringBuilder();
        sb.append(parentCase.getCaseType() != null ? parentCase.getCaseType() : "INCIDENT").append(" investigation ");
        sb.append(parentCase.getTitle()).append(". ");

        firRepository.findByCaseEntityId(parentCase.getId()).ifPresent(fir -> {
            if (fir.getOffenceType() != null) sb.append("Offence: ").append(fir.getOffenceType()).append(". ");
            if (fir.getIncidentLocation() != null) sb.append("Location: ").append(fir.getIncidentLocation()).append(". ");
        });

        List<Evidence> evidenceList = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(parentCase.getId());
        if (!evidenceList.isEmpty()) {
            sb.append("Evidence types involved: ");
            String types = evidenceList.stream().map(Evidence::getEvidenceType).distinct().limit(4).collect(Collectors.joining(", "));
            sb.append(types).append(". ");
        }

        return sb.toString().trim();
    }

    public Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (stationId == null) {
            throw new IllegalStateException("Police officer has no assigned station");
        }
        return caseRepository.findByIdAndStationId(caseId, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found or access restricted to your station"));
    }

    public PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated police officer record not found"));
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String details) {
        try {
            AuditLog log = new AuditLog(null, officer, action, "SUCCESS", null, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }
}
