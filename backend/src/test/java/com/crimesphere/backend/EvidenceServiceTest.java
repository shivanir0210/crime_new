package com.crimesphere.backend;

import com.crimesphere.backend.dto.evidence.CreateEvidenceMetadata;
import com.crimesphere.backend.dto.evidence.EvidenceResponse;
import com.crimesphere.backend.dto.evidence.UpdateEvidenceStatusRequest;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.Evidence;
import com.crimesphere.backend.entity.EvidenceAuditLog;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.exception.CrossCaseDuplicateEvidenceException;
import com.crimesphere.backend.exception.DuplicateEvidenceException;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.EvidenceAuditLogRepository;
import com.crimesphere.backend.repository.EvidenceRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.EvidenceService;
import com.crimesphere.backend.service.GeminiImageService;
import com.crimesphere.backend.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class EvidenceServiceTest {

    @Mock
    private EvidenceRepository evidenceRepository;

    @Mock
    private EvidenceAuditLogRepository auditLogRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    private StorageService storageService;
    private EvidenceService evidenceService;

    private PoliceStation cbeStation;
    private PoliceStation egmStation;
    private PoliceOfficer cbeOfficer;
    private PoliceOfficer egmOfficer;
    private Case cbeCase;
    private Case otherCase;

    @BeforeEach
    void setUp() {
        storageService = new StorageService();
        GeminiImageService geminiImageService = new GeminiImageService("", "imagen-3.0-generate-002", 45, true, new com.fasterxml.jackson.databind.ObjectMapper());
        evidenceService = new EvidenceService(
                evidenceRepository,
                auditLogRepository,
                caseRepository,
                officerRepository,
                storageService,
                null,
                geminiImageService
        );

        cbeStation = new PoliceStation();
        cbeStation.setId(UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11"));
        cbeStation.setStationCode("TN-POLICE-CBE");
        cbeStation.setStationName("Coimbatore Central Police Station");

        egmStation = new PoliceStation();
        egmStation.setId(UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22"));
        egmStation.setStationCode("TN-POLICE-EGM");
        egmStation.setStationName("Egmore Police Station");

        cbeOfficer = new PoliceOfficer();
        cbeOfficer.setId(UUID.randomUUID());
        cbeOfficer.setPoliceId("TN-POL-1001");
        cbeOfficer.setFullName("Development Officer One");
        cbeOfficer.setStation(cbeStation);
        cbeOfficer.setRole("OFFICER");
        cbeOfficer.setIsActive(true);

        egmOfficer = new PoliceOfficer();
        egmOfficer.setId(UUID.randomUUID());
        egmOfficer.setPoliceId("TN-POL-1002");
        egmOfficer.setFullName("Development Officer Two");
        egmOfficer.setStation(egmStation);
        egmOfficer.setRole("OFFICER");
        egmOfficer.setIsActive(true);

        cbeCase = new Case();
        cbeCase.setId(UUID.fromString("a1111111-9c0b-4ef8-bb6d-6bb9bd380001"));
        cbeCase.setCaseNumber("CS-2026-0001");
        cbeCase.setTitle("Homicide at Gandhi Park Area");
        cbeCase.setCaseType("MURDER");
        cbeCase.setPriority("CRITICAL");
        cbeCase.setStatus("OPEN");
        cbeCase.setStation(cbeStation);
        cbeCase.setCreatedBy(cbeOfficer);
        cbeCase.setUpdatedAt(OffsetDateTime.now().minusDays(1));

        otherCase = new Case();
        otherCase.setId(UUID.fromString("a2222222-9c0b-4ef8-bb6d-6bb9bd380002"));
        otherCase.setCaseNumber("CS-2026-0002");
        otherCase.setTitle("Cyber Fraud Investigation");
        otherCase.setCaseType("CYBERCRIME");
        otherCase.setPriority("HIGH");
        otherCase.setStatus("OPEN");
        otherCase.setStation(cbeStation);
        otherCase.setCreatedBy(cbeOfficer);
    }

    @Test
    @DisplayName("Upload Evidence: Should upload, calculate SHA-256, generate EVD number, and update parent case timestamp")
    void testUploadEvidence_Success() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.existsByCaseEntityIdAndFileHash(eq(cbeCase.getId()), anyString())).thenReturn(false);
        when(evidenceRepository.findByFileHashAndCaseEntityIdNot(anyString(), eq(cbeCase.getId()))).thenReturn(List.of());
        when(evidenceRepository.getNextEvidenceNumberSequence()).thenReturn(101L);
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "cctv.mp4", "video/mp4", "sample_video_bytes_test_content".getBytes());
        CreateEvidenceMetadata metadata = new CreateEvidenceMetadata();
        metadata.setTitle("North Gate CCTV Footage");
        metadata.setEvidenceType("CCTV");
        metadata.setCollectedLocation("Gandhi Park");

        EvidenceResponse response = evidenceService.uploadEvidence(cbeCase.getId(), file, metadata, "TN-POL-1001");

        assertNotNull(response);
        assertNotNull(response.getEvidenceNumber());
        assertTrue(response.getEvidenceNumber().startsWith("EVD-CBE-2026-"));
        assertEquals("North Gate CCTV Footage", response.getTitle());
        assertEquals("CCTV", response.getEvidenceType());
        assertNotNull(response.getFileHash());
        assertEquals(64, response.getFileHash().length());

        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Upload Evidence: Same file + Same case = Rejected")
    void testUploadEvidence_SameFile_SameCase_Rejected() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.existsByCaseEntityIdAndFileHash(eq(cbeCase.getId()), anyString())).thenReturn(true);

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "pdf_bytes".getBytes());
        CreateEvidenceMetadata metadata = new CreateEvidenceMetadata();
        metadata.setTitle("Duplicate Forensic Report");
        metadata.setEvidenceType("DOCUMENT");

        DuplicateEvidenceException ex = assertThrows(DuplicateEvidenceException.class, () -> {
            evidenceService.uploadEvidence(cbeCase.getId(), file, metadata, "TN-POL-1001");
        });

        assertEquals("This evidence file already exists in this case.", ex.getMessage());
    }

    @Test
    @DisplayName("Upload Evidence: Same file + Different case without confirmation = Warning exception thrown")
    void testUploadEvidence_SameFile_DifferentCase_WithoutConfirmation_ThrowsCrossCaseException() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.existsByCaseEntityIdAndFileHash(eq(cbeCase.getId()), anyString())).thenReturn(false);

        Evidence existingEvidenceInOtherCase = new Evidence();
        existingEvidenceInOtherCase.setCaseEntity(otherCase);
        existingEvidenceInOtherCase.setEvidenceNumber("EVD-CBE-2026-0099");
        when(evidenceRepository.findByFileHashAndCaseEntityIdNot(anyString(), eq(cbeCase.getId())))
                .thenReturn(List.of(existingEvidenceInOtherCase));

        MockMultipartFile file = new MockMultipartFile("file", "shared_report.pdf", "application/pdf", "shared_content_bytes".getBytes());
        CreateEvidenceMetadata metadata = new CreateEvidenceMetadata();
        metadata.setTitle("Cross Case Forensic Report");
        metadata.setEvidenceType("DOCUMENT");
        metadata.setConfirmDuplicate(false);

        CrossCaseDuplicateEvidenceException ex = assertThrows(CrossCaseDuplicateEvidenceException.class, () -> {
            evidenceService.uploadEvidence(cbeCase.getId(), file, metadata, "TN-POL-1001");
        });

        assertEquals("CS-2026-0002", ex.getCaseNumber());
        assertTrue(ex.getMessage().contains("This file already exists in another case: CS-2026-0002. It may be relevant to multiple investigations."));
    }

    @Test
    @DisplayName("Upload Evidence: Same file + Different case with confirmation = Allowed and audit logged")
    void testUploadEvidence_SameFile_DifferentCase_WithConfirmation_Allowed() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.existsByCaseEntityIdAndFileHash(eq(cbeCase.getId()), anyString())).thenReturn(false);

        Evidence existingEvidenceInOtherCase = new Evidence();
        existingEvidenceInOtherCase.setCaseEntity(otherCase);
        existingEvidenceInOtherCase.setEvidenceNumber("EVD-CBE-2026-0099");
        when(evidenceRepository.findByFileHashAndCaseEntityIdNot(anyString(), eq(cbeCase.getId())))
                .thenReturn(List.of(existingEvidenceInOtherCase));
        when(evidenceRepository.getNextEvidenceNumberSequence()).thenReturn(102L);
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "shared_report.pdf", "application/pdf", "shared_content_bytes".getBytes());
        CreateEvidenceMetadata metadata = new CreateEvidenceMetadata();
        metadata.setTitle("Cross Case Forensic Report");
        metadata.setEvidenceType("DOCUMENT");
        metadata.setConfirmDuplicate(true);

        EvidenceResponse response = evidenceService.uploadEvidence(cbeCase.getId(), file, metadata, "TN-POL-1001");

        assertNotNull(response);
        assertEquals("Cross Case Forensic Report", response.getTitle());

        ArgumentCaptor<EvidenceAuditLog> auditCaptor = ArgumentCaptor.forClass(EvidenceAuditLog.class);
        verify(auditLogRepository, times(1)).save(auditCaptor.capture());
        assertTrue(auditCaptor.getValue().getDetails().contains("Cross-case duplicate confirmed from case CS-2026-0002"));
    }

    @Test
    @DisplayName("Upload Evidence: Different file + Same case = Allowed")
    void testUploadEvidence_DifferentFile_SameCase_Allowed() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.existsByCaseEntityIdAndFileHash(eq(cbeCase.getId()), anyString())).thenReturn(false);
        when(evidenceRepository.findByFileHashAndCaseEntityIdNot(anyString(), eq(cbeCase.getId()))).thenReturn(List.of());
        when(evidenceRepository.getNextEvidenceNumberSequence()).thenReturn(103L);
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile file = new MockMultipartFile("file", "unique_photo.jpg", "image/jpeg", "unique_image_data_bytes_987".getBytes());
        CreateEvidenceMetadata metadata = new CreateEvidenceMetadata();
        metadata.setTitle("Crime Scene Photo 01");
        metadata.setEvidenceType("IMAGE");

        EvidenceResponse response = evidenceService.uploadEvidence(cbeCase.getId(), file, metadata, "TN-POL-1001");

        assertNotNull(response);
        assertEquals("Crime Scene Photo 01", response.getTitle());
    }

    @Test
    @DisplayName("Upload Evidence: Cross-station upload attempt is denied (404)")
    void testUploadEvidence_CrossStationDenied() {
        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), egmStation.getId())).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "pdf_bytes".getBytes());
        CreateEvidenceMetadata metadata = new CreateEvidenceMetadata();
        metadata.setTitle("Unauthorized Report");
        metadata.setEvidenceType("DOCUMENT");

        assertThrows(ResourceNotFoundException.class, () -> {
            evidenceService.uploadEvidence(cbeCase.getId(), file, metadata, "TN-POL-1002");
        });
    }

    @Test
    @DisplayName("Status Update: Transition status from UPLOADED to VERIFIED and refresh case timestamp")
    void testUpdateEvidenceStatus_Success() {
        UUID evidenceId = UUID.randomUUID();
        Evidence evidence = new Evidence();
        evidence.setId(evidenceId);
        evidence.setCaseEntity(cbeCase);
        evidence.setEvidenceNumber("EVD-CBE-2026-0001");
        evidence.setStatus("UPLOADED");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.findByIdAndCaseEntityId(evidenceId, cbeCase.getId())).thenReturn(Optional.of(evidence));
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateEvidenceStatusRequest request = new UpdateEvidenceStatusRequest();
        request.setStatus("VERIFIED");
        request.setNotes("Verified by lead forensics officer.");

        EvidenceResponse response = evidenceService.updateEvidenceStatus(cbeCase.getId(), evidenceId, request, "TN-POL-1001");

        assertNotNull(response);
        assertEquals("VERIFIED", response.getStatus());

        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Download Evidence: Successfully retrieves resource and records audit log")
    void testDownloadEvidence_Success() {
        UUID evidenceId = UUID.randomUUID();
        MockMultipartFile testFile = new MockMultipartFile("file", "ballistics.pdf", "application/pdf", "sample ballistics data".getBytes());
        String storagePath = storageService.storeEvidenceFile(cbeCase.getId(), evidenceId, testFile);

        Evidence evidence = new Evidence();
        evidence.setId(evidenceId);
        evidence.setCaseEntity(cbeCase);
        evidence.setEvidenceNumber("EVD-CBE-2026-0001");
        evidence.setOriginalFileName("ballistics.pdf");
        evidence.setMimeType("application/pdf");
        evidence.setFileSize((long) "sample ballistics data".length());
        evidence.setStoragePath(storagePath);

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.findByIdAndCaseEntityId(evidenceId, cbeCase.getId())).thenReturn(Optional.of(evidence));

        EvidenceService.DownloadFileInfo downloadInfo = evidenceService.downloadEvidenceFile(cbeCase.getId(), evidenceId, "TN-POL-1001");

        assertNotNull(downloadInfo);
        assertEquals("ballistics.pdf", downloadInfo.getFilename());
        assertEquals("application/pdf", downloadInfo.getContentType());
        assertNotNull(downloadInfo.getResource());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Update Evidence Status: AI-generated illustration cannot be transitioned to VERIFIED")
    void testUpdateEvidenceStatus_AiIllustrationCannotBeVerified() {
        UUID evidenceId = UUID.randomUUID();
        Evidence aiEvidence = new Evidence();
        aiEvidence.setId(evidenceId);
        aiEvidence.setCaseEntity(cbeCase);
        aiEvidence.setCategory("AI_ILLUSTRATION");
        aiEvidence.setIsAiGenerated(true);
        aiEvidence.setStatus("UPLOADED");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.findByIdAndCaseEntityId(evidenceId, cbeCase.getId())).thenReturn(Optional.of(aiEvidence));

        UpdateEvidenceStatusRequest req = new UpdateEvidenceStatusRequest();
        req.setStatus("VERIFIED");

        assertThrows(IllegalArgumentException.class, () ->
                evidenceService.updateEvidenceStatus(cbeCase.getId(), evidenceId, req, "TN-POL-1001")
        );
    }

    @Test
    @DisplayName("Generate AI Illustration for Evidence: sets category=AI_ILLUSTRATION and isAiGenerated=true")
    void testGenerateAiIllustrationForEvidence_Success() {
        UUID evidenceId = UUID.randomUUID();
        Evidence evidence = new Evidence();
        evidence.setId(evidenceId);
        evidence.setCaseEntity(cbeCase);
        evidence.setEvidenceNumber("EVD-CBE-2026-0002");
        evidence.setTitle("Alleyway Footprint");
        evidence.setEvidenceType("OTHER");
        evidence.setCategory("UPLOADED_EVIDENCE");
        evidence.setIsAiGenerated(false);
        evidence.setStatus("UPLOADED");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.findByIdAndCaseEntityId(evidenceId, cbeCase.getId())).thenReturn(Optional.of(evidence));
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EvidenceResponse res = evidenceService.generateAiIllustrationForEvidence(
                cbeCase.getId(),
                evidenceId,
                "Fictional alleyway mud puddle footprint with boot tread marks",
                "TN-POL-1001"
        );

        assertNotNull(res);
        assertEquals("AI_ILLUSTRATION", res.getCategory());
        assertTrue(res.getIsAiGenerated());
        assertEquals("AI_ILLUSTRATION", res.getStatus());
        assertEquals("image/jpeg", res.getMimeType());
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Upload Evidence without file: automatically creates default contextual AI illustration")
    void testUploadEvidence_WithoutFile_GeneratesDefaultAiIllustration() {
        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.getNextEvidenceNumberSequence()).thenReturn(103L);
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateEvidenceMetadata metadata = new CreateEvidenceMetadata();
        metadata.setTitle("Forensic examination report for Phase 13 notification testing");
        metadata.setEvidenceType("FORENSIC_REPORT");
        metadata.setDescription("Forensic analysis report on lab specimens");
        metadata.setCollectedLocation("Crime Lab");

        EvidenceResponse response = evidenceService.uploadEvidence(cbeCase.getId(), null, metadata, "TN-POL-1001");

        assertNotNull(response);
        assertEquals("AI_ILLUSTRATION", response.getCategory());
        assertTrue(response.getIsAiGenerated());
        assertEquals("AI_ILLUSTRATION", response.getStatus());
        assertEquals("image/jpeg", response.getMimeType());
        assertTrue(response.getOriginalFileName().contains("ai-illustration-"));
        assertNotNull(response.getEvidenceNumber());

        verify(caseRepository, times(1)).save(cbeCase);
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Replace with Real Evidence File: transitions from AI_ILLUSTRATION to UPLOADED_EVIDENCE")
    void testReplaceWithRealEvidenceFile_Success() {
        UUID evidenceId = UUID.randomUUID();
        Evidence defaultIllustrationEvidence = new Evidence();
        defaultIllustrationEvidence.setId(evidenceId);
        defaultIllustrationEvidence.setCaseEntity(cbeCase);
        defaultIllustrationEvidence.setEvidenceNumber("EVD-CBE-2026-0103");
        defaultIllustrationEvidence.setTitle("Perimeter CCTV");
        defaultIllustrationEvidence.setEvidenceType("CCTV");
        defaultIllustrationEvidence.setCategory("AI_ILLUSTRATION");
        defaultIllustrationEvidence.setIsAiGenerated(true);
        defaultIllustrationEvidence.setStatus("AI_ILLUSTRATION");
        defaultIllustrationEvidence.setStoragePath("cbeCaseId/evidence/test.jpg");
        defaultIllustrationEvidence.setOriginalFileName("ai-illustration-evd_cbe_2026_0103.jpg");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findByIdAndStationId(cbeCase.getId(), cbeStation.getId())).thenReturn(Optional.of(cbeCase));
        when(evidenceRepository.findByIdAndCaseEntityId(evidenceId, cbeCase.getId())).thenReturn(Optional.of(defaultIllustrationEvidence));
        when(evidenceRepository.save(any(Evidence.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MockMultipartFile realFile = new MockMultipartFile(
                "file",
                "actual_security_cam_feed.mp4",
                "video/mp4",
                "real_forensic_video_data_bytes_12345".getBytes()
        );

        EvidenceResponse response = evidenceService.replaceWithRealEvidenceFile(
                cbeCase.getId(),
                evidenceId,
                realFile,
                "TN-POL-1001"
        );

        assertNotNull(response);
        assertEquals("UPLOADED_EVIDENCE", response.getCategory());
        assertFalse(response.getIsAiGenerated());
        assertEquals("UPLOADED", response.getStatus());
        assertEquals("actual_security_cam_feed.mp4", response.getOriginalFileName());
        assertEquals("video/mp4", response.getMimeType());
        assertNotNull(response.getFileHash());
        assertEquals(64, response.getFileHash().length());

        verify(auditLogRepository, atLeastOnce()).save(any());
    }
}
