package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.ai.AiIndexCaseRequest;
import com.crimesphere.backend.dto.ai.AiKnowledgeChunkDto;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.*;

@Service
public class KnowledgeIndexingService {

    private static final Logger logger = LoggerFactory.getLogger(KnowledgeIndexingService.class);

    private final CaseRepository caseRepository;
    private final FIRRepository firRepository;
    private final EvidenceRepository evidenceRepository;
    private final EvidenceGapRepository evidenceGapRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;
    private final AiServiceClient aiServiceClient;

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public KnowledgeIndexingService(
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

    public boolean isPgvectorAvailable() {
        try {
            if (entityManager == null) return false;
            Object result = entityManager.createNativeQuery("SELECT count(1) FROM pg_extension WHERE extname = 'vector'").getSingleResult();
            return result != null && ((Number) result).longValue() > 0;
        } catch (Exception e) {
            return false;
        }
    }

    @Transactional
    public Map<String, Object> indexCase(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (!isPgvectorAvailable()) {
            recordAuditLog(officer, "AI_INDEXING_FAILED", "PostgreSQL pgvector extension unavailable on server.");
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE,
                    "PostgreSQL pgvector extension is not installed or enabled on the server."
            );
        }

        logger.info("Indexing knowledge chunks for case {} (ID: {}) by officer {}",
                parentCase.getCaseNumber(), caseId, policeId);

        List<AiKnowledgeChunkDto> extractedChunks = extractChunks(parentCase);
        List<AiKnowledgeChunkDto> chunksToEmbed = new ArrayList<>();
        int updatedCount = 0;
        int skippedCount = 0;

        for (AiKnowledgeChunkDto dto : extractedChunks) {
            Optional<KnowledgeChunk> existingOpt = knowledgeChunkRepository
                    .findBySourceTypeAndSourceId(dto.getSourceType(), dto.getSourceId());

            if (existingOpt.isPresent()) {
                KnowledgeChunk existing = existingOpt.get();
                if (existing.getContentHash().equals(dto.getContentHash())) {
                    skippedCount++;
                    continue;
                }
            }
            chunksToEmbed.add(dto);
        }

        if (!chunksToEmbed.isEmpty()) {
            AiIndexCaseRequest request = new AiIndexCaseRequest(caseId, chunksToEmbed);
            List<AiKnowledgeChunkDto> embeddedChunks = aiServiceClient.indexChunks(request);

            for (AiKnowledgeChunkDto embedded : embeddedChunks) {
                KnowledgeChunk chunk = knowledgeChunkRepository
                        .findBySourceTypeAndSourceId(embedded.getSourceType(), embedded.getSourceId())
                        .orElseGet(KnowledgeChunk::new);

                chunk.setCaseEntity(parentCase);
                chunk.setSourceType(embedded.getSourceType());
                chunk.setSourceId(embedded.getSourceId());
                chunk.setContent(embedded.getContent());
                chunk.setContentHash(embedded.getContentHash());
                if (embedded.getMetadata() != null) {
                    Map<String, Object> metaMap = new HashMap<>(embedded.getMetadata());
                    Object embObj = metaMap.remove("embedding_json");
                    if (embObj != null) {
                        chunk.setEmbedding(embObj.toString());
                    }
                    try {
                        chunk.setMetadata(objectMapper.writeValueAsString(metaMap));
                    } catch (Exception e) {
                        logger.warn("Failed to serialize metadata for chunk: {}", e.getMessage());
                    }
                }
                chunk.setUpdatedAt(OffsetDateTime.now());

                knowledgeChunkRepository.save(chunk);
                updatedCount++;
            }
        }

        recordAuditLog(officer, "CASE_INDEXED",
                String.format("Indexed knowledge chunks for case %s (Updated: %d, Unchanged: %d)",
                        parentCase.getCaseNumber(), updatedCount, skippedCount));

        return Map.of(
                "case_id", caseId,
                "case_number", parentCase.getCaseNumber(),
                "indexed_count", updatedCount,
                "skipped_count", skippedCount,
                "total_chunks", extractedChunks.size(),
                "status", "INDEXED"
        );
    }

    public List<AiKnowledgeChunkDto> extractChunks(Case parentCase) {
        List<AiKnowledgeChunkDto> chunks = new ArrayList<>();
        UUID caseId = parentCase.getId();

        // 1. Case Chunk
        String caseText = String.format(
                "Case Number: %s\nClassification: %s\nTitle: %s\nStatus: %s\nNarrative: %s",
                parentCase.getCaseNumber(),
                parentCase.getCaseType() != null ? parentCase.getCaseType() : "OTHER",
                parentCase.getTitle(),
                parentCase.getStatus(),
                parentCase.getDescription() != null ? parentCase.getDescription() : "No incident briefing recorded."
        );
        chunks.add(new AiKnowledgeChunkDto(
                "CASE", caseId, caseId, parentCase.getCaseNumber(), parentCase.getCaseType(),
                caseText, computeHash(caseText), Map.of("entity", "CASE", "title", parentCase.getTitle())
        ));

        // 2. FIR Chunk
        firRepository.findByCaseEntityId(caseId).ifPresent(fir -> {
            String firText = String.format(
                    "First Information Report: %s\nStation: %s\nOffence: %s\nSections: %s\nLocation: %s\nDetails: %s",
                    fir.getFirNumber(),
                    fir.getPoliceStation(),
                    fir.getOffenceType(),
                    fir.getSectionsOfLaw() != null ? fir.getSectionsOfLaw() : "Pending",
                    fir.getIncidentLocation() != null ? fir.getIncidentLocation() : "Scene of crime",
                    fir.getDescription() != null ? fir.getDescription() : "No FIR narrative recorded."
            );
            chunks.add(new AiKnowledgeChunkDto(
                    "FIR", fir.getId(), caseId, parentCase.getCaseNumber(), parentCase.getCaseType(),
                    firText, computeHash(firText), Map.of("entity", "FIR", "firNumber", fir.getFirNumber())
            ));
        });

        // 3. Evidence Chunks
        List<Evidence> evidenceList = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        for (Evidence ev : evidenceList) {
            String evText = String.format(
                    "Evidence Item: %s\nTitle: %s\nType: %s\nStatus: %s\nCollected Location: %s\nHash: %s\nDescription: %s\nNotes: %s",
                    ev.getEvidenceNumber(),
                    ev.getTitle(),
                    ev.getEvidenceType(),
                    ev.getStatus(),
                    ev.getCollectedLocation() != null ? ev.getCollectedLocation() : "Unspecified",
                    ev.getFileHash() != null ? ev.getFileHash() : "N/A",
                    ev.getDescription() != null ? ev.getDescription() : "No description",
                    ev.getNotes() != null ? ev.getNotes() : "None"
            );
            chunks.add(new AiKnowledgeChunkDto(
                    "EVIDENCE", ev.getId(), caseId, parentCase.getCaseNumber(), parentCase.getCaseType(),
                    evText, computeHash(evText), Map.of("entity", "EVIDENCE", "evidenceNumber", ev.getEvidenceNumber(), "type", ev.getEvidenceType())
            ));
        }

        // 4. Evidence Gap Chunks
        List<EvidenceGap> gaps = evidenceGapRepository.findByCaseEntityIdOrderByCreatedAtDesc(caseId);
        for (EvidenceGap gap : gaps) {
            String gapText = String.format(
                    "Evidence Gap: %s\nCategory: %s\nSeverity: %s\nStatus: %s\nReason: %s\nDescription: %s",
                    gap.getTitle(),
                    gap.getCategory(),
                    gap.getSeverity(),
                    gap.getStatus(),
                    gap.getReason() != null ? gap.getReason() : "Procedural requirement",
                    gap.getDescription() != null ? gap.getDescription() : "None"
            );
            chunks.add(new AiKnowledgeChunkDto(
                    "EVIDENCE_GAP", gap.getId(), caseId, parentCase.getCaseNumber(), parentCase.getCaseType(),
                    gapText, computeHash(gapText), Map.of("entity", "EVIDENCE_GAP", "category", gap.getCategory(), "severity", gap.getSeverity())
            ));
        }

        return chunks;
    }

    private String computeHash(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            return UUID.nameUUIDFromBytes(input.getBytes(StandardCharsets.UTF_8)).toString().replace("-", "");
        }
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
