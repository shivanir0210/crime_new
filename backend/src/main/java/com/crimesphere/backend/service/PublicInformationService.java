package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.public_search.PublicInformationResponse;
import com.crimesphere.backend.dto.public_search.SavePublicInformationRequest;
import com.crimesphere.backend.dto.public_search.UpdatePublicInformationRequest;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PublicInformationItem;
import com.crimesphere.backend.entity.PublicInformationVerificationStatus;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PublicInformationItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class PublicInformationService {

    private static final Logger logger = LoggerFactory.getLogger(PublicInformationService.class);

    private final CaseRepository caseRepository;
    private final PublicInformationItemRepository itemRepository;
    private final AuditLogRepository auditLogRepository;

    public PublicInformationService(CaseRepository caseRepository,
                                    PublicInformationItemRepository itemRepository,
                                    AuditLogRepository auditLogRepository) {
        this.caseRepository = caseRepository;
        this.itemRepository = itemRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Bookmarks a public news/web result into a case record for future review.
     * Enforces URL scheme validation (HTTP/HTTPS only), duplicate handling, and audit logging.
     * Note: Saving public information does NOT create evidence or alter case state.
     */
    @Transactional
    public PublicInformationResponse savePublicInformation(UUID caseId, SavePublicInformationRequest request, PoliceOfficer officer) {
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found: " + caseId));
        validateStationAccess(officer, crimeCase);

        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Article title is required.");
        }
        if (request.getSourceUrl() == null || request.getSourceUrl().trim().isEmpty()) {
            throw new IllegalArgumentException("Source URL is required.");
        }

        // URL Security check
        String url = request.getSourceUrl().trim();
        if (!isValidPublicUrl(url)) {
            throw new IllegalArgumentException("Invalid URL format. Only secure HTTP and HTTPS public web links are permitted.");
        }

        // Check for existing duplicate save
        Optional<PublicInformationItem> existing = itemRepository.findByCrimeCaseIdAndSourceUrl(caseId, url);
        if (existing.isPresent()) {
            PublicInformationItem item = existing.get();
            if (request.getNotes() != null && !request.getNotes().isBlank()) {
                item.setNotes((item.getNotes() != null ? item.getNotes() + "\n" : "") + request.getNotes().trim());
                item.setUpdatedAt(OffsetDateTime.now());
                itemRepository.save(item);
            }
            return PublicInformationResponse.fromEntity(item);
        }

        PublicInformationItem item = new PublicInformationItem(
                null,
                crimeCase,
                request.getTitle().trim(),
                request.getDescription() != null ? request.getDescription().trim() : null,
                request.getSourceName() != null ? request.getSourceName().trim() : "Unknown Source",
                url,
                request.getPublishedAt(),
                request.getAuthor() != null ? request.getAuthor().trim() : null,
                request.getImageUrl() != null ? request.getImageUrl().trim() : null,
                request.getOriginalQuery() != null ? request.getOriginalQuery().trim() : null,
                PublicInformationVerificationStatus.UNVERIFIED,
                request.getNotes() != null ? request.getNotes().trim() : null,
                officer
        );

        PublicInformationItem saved = itemRepository.save(item);

        recordAuditLog(officer, "PUBLIC_INFORMATION_SAVED", "SUCCESS",
                "Case: " + crimeCase.getCaseNumber() + ", Item ID: " + saved.getId() + ", Source: " + saved.getSourceName());

        return PublicInformationResponse.fromEntity(saved);
    }

    /**
     * Lists all saved public information items for a case.
     */
    @Transactional(readOnly = true)
    public List<PublicInformationResponse> getCasePublicInformation(UUID caseId, PoliceOfficer officer) {
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found: " + caseId));
        validateStationAccess(officer, crimeCase);

        return itemRepository.findByCrimeCaseIdOrderByCreatedAtDesc(caseId).stream()
                .map(PublicInformationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a single saved public information item.
     */
    @Transactional(readOnly = true)
    public PublicInformationResponse getPublicInformationItem(UUID caseId, UUID itemId, PoliceOfficer officer) {
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found: " + caseId));
        validateStationAccess(officer, crimeCase);

        PublicInformationItem item = itemRepository.findByIdAndCrimeCaseId(itemId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Public information item not found: " + itemId));

        recordAuditLog(officer, "PUBLIC_INFORMATION_VIEWED", "SUCCESS",
                "Case: " + crimeCase.getCaseNumber() + ", Item ID: " + itemId);

        return PublicInformationResponse.fromEntity(item);
    }

    /**
     * Updates verification status and investigator notes for a saved public information item.
     * Enforces mandatory notes requirement for CORROBORATED, CONTRADICTED, and NOT_RELEVANT.
     */
    @Transactional
    public PublicInformationResponse updatePublicInformationItem(UUID caseId, UUID itemId,
                                                                 UpdatePublicInformationRequest request,
                                                                 PoliceOfficer officer) {
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found: " + caseId));
        validateStationAccess(officer, crimeCase);

        PublicInformationItem item = itemRepository.findByIdAndCrimeCaseId(itemId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Public information item not found: " + itemId));

        if (request.getVerificationStatus() == null) {
            throw new IllegalArgumentException("Verification status is required.");
        }

        PublicInformationVerificationStatus newStatus = request.getVerificationStatus();
        String notes = request.getNotes() != null ? request.getNotes().trim() : "";

        // Enforce mandatory notes requirement
        if ((newStatus == PublicInformationVerificationStatus.CORROBORATED ||
                newStatus == PublicInformationVerificationStatus.CONTRADICTED ||
                newStatus == PublicInformationVerificationStatus.NOT_RELEVANT) && notes.isEmpty()) {
            throw new IllegalArgumentException("Investigator verification notes are mandatory when marking public information as " + newStatus);
        }

        PublicInformationVerificationStatus oldStatus = item.getVerificationStatus();
        item.setVerificationStatus(newStatus);
        if (!notes.isEmpty()) {
            item.setNotes(notes);
        }
        item.setUpdatedAt(OffsetDateTime.now());

        PublicInformationItem updated = itemRepository.save(item);

        recordAuditLog(officer, "PUBLIC_INFORMATION_VERIFICATION_CHANGED", "SUCCESS",
                "Case: " + crimeCase.getCaseNumber() + ", Item: " + itemId + ", Old: " + oldStatus + " -> New: " + newStatus);

        return PublicInformationResponse.fromEntity(updated);
    }

    /**
     * Deletes a saved public information item with station authorization and audit logging.
     */
    @Transactional
    public void deletePublicInformationItem(UUID caseId, UUID itemId, PoliceOfficer officer) {
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case record not found: " + caseId));
        validateStationAccess(officer, crimeCase);

        PublicInformationItem item = itemRepository.findByIdAndCrimeCaseId(itemId, caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Public information item not found: " + itemId));

        itemRepository.delete(item);

        recordAuditLog(officer, "PUBLIC_INFORMATION_DELETED", "SUCCESS",
                "Case: " + crimeCase.getCaseNumber() + ", Item ID: " + itemId + ", Title: " + item.getTitle());
    }

    /**
     * Validates that the URL uses exclusively http or https protocols.
     * Prevents javascript:, data:, file:, etc.
     */
    public static boolean isValidPublicUrl(String url) {
        if (url == null || url.trim().isEmpty()) return false;
        String lower = url.trim().toLowerCase();
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return false;
        }
        try {
            String sanitized = url.trim().replaceAll(" ", "%20");
            URI uri = new URI(sanitized);
            String scheme = uri.getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (Exception e) {
            return false;
        }
    }

    private void validateStationAccess(PoliceOfficer officer, Case crimeCase) {
        if ("ADMIN".equalsIgnoreCase(officer.getRole()) || "SUPER_ADMIN".equalsIgnoreCase(officer.getRole())) {
            return;
        }
        if (officer.getStation() == null || crimeCase.getStation() == null ||
                !officer.getStation().getId().equals(crimeCase.getStation().getId())) {
            throw new AccessDeniedException("Access denied: Case record belongs to another police station.");
        }
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String status, String details) {
        try {
            AuditLog log = new AuditLog(null, officer, action, status, null, details);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }
}
