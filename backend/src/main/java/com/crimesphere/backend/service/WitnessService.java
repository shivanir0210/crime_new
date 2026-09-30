package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.witness.CreateWitnessRequest;
import com.crimesphere.backend.dto.witness.UpdateWitnessRequest;
import com.crimesphere.backend.dto.witness.WitnessResponse;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.Witness;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.repository.WitnessRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class WitnessService {

    private static final Logger logger = LoggerFactory.getLogger(WitnessService.class);

    private static final Set<String> ALLOWED_WITNESS_TYPES = Set.of(
            "EYEWITNESS", "MATERIAL_WITNESS", "CHARACTER_WITNESS", "EXPERT_WITNESS", "INFORMANT", "OTHER"
    );

    private static final Set<String> ALLOWED_RELIABILITY = Set.of(
            "HIGH", "MEDIUM", "LOW", "UNVERIFIED"
    );

    private final WitnessRepository witnessRepository;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;

    public WitnessService(WitnessRepository witnessRepository,
                          CaseRepository caseRepository,
                          PoliceOfficerRepository officerRepository,
                          AuditLogRepository auditLogRepository) {
        this.witnessRepository = witnessRepository;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public WitnessResponse createWitness(UUID caseId, CreateWitnessRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (request.getFullName() == null || request.getFullName().trim().isEmpty()) {
            throw new IllegalArgumentException("Witness full name is required");
        }

        String witnessType = request.getWitnessType() != null && !request.getWitnessType().trim().isEmpty()
                ? request.getWitnessType().trim().toUpperCase() : "EYEWITNESS";

        if (!ALLOWED_WITNESS_TYPES.contains(witnessType)) {
            witnessType = "OTHER";
        }

        String reliability = request.getReliabilityScore() != null && !request.getReliabilityScore().trim().isEmpty()
                ? request.getReliabilityScore().trim().toUpperCase() : "MEDIUM";

        if (!ALLOWED_RELIABILITY.contains(reliability)) {
            reliability = "MEDIUM";
        }

        Witness witness = new Witness();
        witness.setCaseEntity(parentCase);
        witness.setFullName(request.getFullName().trim());
        witness.setAge(request.getAge());
        witness.setGender(request.getGender() != null ? request.getGender().trim().toUpperCase() : null);
        witness.setPhone(request.getPhone() != null ? request.getPhone().trim() : null);
        witness.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        witness.setOccupation(request.getOccupation() != null ? request.getOccupation().trim() : null);
        witness.setStatementSummary(request.getStatementSummary() != null ? request.getStatementSummary().trim() : null);
        witness.setWitnessType(witnessType);
        witness.setReliabilityScore(reliability);
        witness.setInterviewDate(request.getInterviewDate() != null ? request.getInterviewDate() : OffsetDateTime.now());
        witness.setCreatedBy(officer);

        Witness savedWitness = witnessRepository.save(witness);

        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "WITNESS_CREATED", "SUCCESS", httpRequest);
        logger.info("Witness record created (ID: {}) for case {} by officer {}", savedWitness.getId(), parentCase.getCaseNumber(), policeId);

        return new WitnessResponse(savedWitness);
    }

    @Transactional(readOnly = true)
    public List<WitnessResponse> getWitnessesByCaseId(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        return witnessRepository.findByCaseEntityIdOrderByCreatedAtAsc(parentCase.getId())
                .stream()
                .map(WitnessResponse::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public WitnessResponse getWitnessById(UUID caseId, UUID witnessId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Witness witness = witnessRepository.findByIdAndCaseEntityId(witnessId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Witness not found in case"));

        return new WitnessResponse(witness);
    }

    @Transactional
    public WitnessResponse updateWitness(UUID caseId, UUID witnessId, UpdateWitnessRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Witness witness = witnessRepository.findByIdAndCaseEntityId(witnessId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Witness not found in case"));

        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            witness.setFullName(request.getFullName().trim());
        }
        if (request.getAge() != null) {
            witness.setAge(request.getAge());
        }
        if (request.getGender() != null) {
            witness.setGender(request.getGender().trim().toUpperCase());
        }
        if (request.getPhone() != null) {
            witness.setPhone(request.getPhone().trim());
        }
        if (request.getAddress() != null) {
            witness.setAddress(request.getAddress().trim());
        }
        if (request.getOccupation() != null) {
            witness.setOccupation(request.getOccupation().trim());
        }
        if (request.getStatementSummary() != null) {
            witness.setStatementSummary(request.getStatementSummary().trim());
        }
        if (request.getWitnessType() != null && !request.getWitnessType().trim().isEmpty()) {
            String wt = request.getWitnessType().trim().toUpperCase();
            if (ALLOWED_WITNESS_TYPES.contains(wt)) {
                witness.setWitnessType(wt);
            }
        }
        if (request.getReliabilityScore() != null && !request.getReliabilityScore().trim().isEmpty()) {
            String rel = request.getReliabilityScore().trim().toUpperCase();
            if (ALLOWED_RELIABILITY.contains(rel)) {
                witness.setReliabilityScore(rel);
            }
        }
        if (request.getInterviewDate() != null) {
            witness.setInterviewDate(request.getInterviewDate());
        }

        Witness updatedWitness = witnessRepository.save(witness);

        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "WITNESS_UPDATED", "SUCCESS", httpRequest);
        logger.info("Witness record updated (ID: {}) for case {} by officer {}", updatedWitness.getId(), parentCase.getCaseNumber(), policeId);

        return new WitnessResponse(updatedWitness);
    }

    @Transactional
    public void deleteWitness(UUID caseId, UUID witnessId, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        Witness witness = witnessRepository.findByIdAndCaseEntityId(witnessId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Witness not found in case"));

        witnessRepository.delete(witness);

        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "WITNESS_DELETED", "SUCCESS", httpRequest);
        logger.info("Witness record deleted (ID: {}) for case {} by officer {}", witnessId, parentCase.getCaseNumber(), policeId);
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

    private void recordAuditLog(PoliceOfficer officer, String action, String status, HttpServletRequest httpRequest) {
        try {
            String ip = httpRequest != null ? httpRequest.getRemoteAddr() : "127.0.0.1";
            String userAgent = httpRequest != null ? httpRequest.getHeader("User-Agent") : "Internal";
            AuditLog log = new AuditLog(null, officer, action, status, ip, userAgent);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }
}
