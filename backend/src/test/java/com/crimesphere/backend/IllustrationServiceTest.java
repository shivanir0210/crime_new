package com.crimesphere.backend;

import com.crimesphere.backend.dto.illustration.CaseIllustrationResponse;
import com.crimesphere.backend.dto.illustration.GenerateIllustrationRequest;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseIllustrationRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.GeminiImageService;
import com.crimesphere.backend.service.IllustrationService;
import com.crimesphere.backend.service.StorageService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class IllustrationServiceTest {

    @Mock
    private CaseIllustrationRepository illustrationRepository;

    @Mock
    private CaseRepository caseRepository;

    @Mock
    private PoliceOfficerRepository officerRepository;

    @Mock
    private AuditLogRepository auditLogRepository;

    private GeminiImageService geminiImageService;
    private StorageService storageService;
    private IllustrationService illustrationService;

    private PoliceStation cbeStation;
    private PoliceStation egmStation;
    private PoliceOfficer cbeOfficer;
    private PoliceOfficer egmOfficer;
    private Case cbeCase;

    @BeforeEach
    void setUp() {
        geminiImageService = new GeminiImageService("", "imagen-3.0-generate-002", 45, true, new ObjectMapper());
        storageService = new StorageService();

        illustrationService = new IllustrationService(
                illustrationRepository,
                caseRepository,
                officerRepository,
                geminiImageService,
                storageService,
                auditLogRepository
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
        cbeOfficer.setFullName("Inspector K. Vijay");
        cbeOfficer.setStation(cbeStation);
        cbeOfficer.setRole("OFFICER");
        cbeOfficer.setIsActive(true);

        egmOfficer = new PoliceOfficer();
        egmOfficer.setId(UUID.randomUUID());
        egmOfficer.setPoliceId("TN-POL-1002");
        egmOfficer.setFullName("Inspector R. Murugan");
        egmOfficer.setStation(egmStation);
        egmOfficer.setRole("OFFICER");
        egmOfficer.setIsActive(true);

        cbeCase = new Case();
        cbeCase.setId(UUID.fromString("a1111111-9c0b-4ef8-bb6d-6bb9bd380001"));
        cbeCase.setCaseNumber("CS-2026-0001");
        cbeCase.setTitle("Gandhi Park Investigation");
        cbeCase.setStation(cbeStation);
    }

    @Test
    @DisplayName("Should successfully generate AI illustration and enforce NOT EVIDENCE label")
    void testGenerateIllustrationSuccess() {
        String prompt = "Nighttime exterior alleyway with parked dark sedan near warehouse loading bay";
        GenerateIllustrationRequest req = new GenerateIllustrationRequest(prompt, "1:1");

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(illustrationRepository.save(any(CaseIllustration.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CaseIllustrationResponse res = illustrationService.generateIllustration(cbeCase.getId(), req, "TN-POL-1001", null);

        assertNotNull(res);
        assertEquals(prompt, res.getPrompt());
        assertTrue(res.getIsAiGenerated());
        assertEquals("AI_ILLUSTRATION", res.getStatus());
        assertTrue(res.getDisclaimer().contains("NOT EVIDENCE"));
        assertTrue(res.getModelName().contains("imagen-3.0-generate-002"));
        verify(illustrationRepository, times(1)).save(any(CaseIllustration.class));
        verify(auditLogRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("Should enforce station isolation and deny officer from another precinct")
    void testStationIsolationDenied() {
        GenerateIllustrationRequest req = new GenerateIllustrationRequest("Crime scene layout diagram", "1:1");

        when(officerRepository.findByPoliceId("TN-POL-1002")).thenReturn(Optional.of(egmOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));

        assertThrows(AccessDeniedException.class, () ->
                illustrationService.generateIllustration(cbeCase.getId(), req, "TN-POL-1002", null)
        );
    }

    @Test
    @DisplayName("Should reject blank prompt")
    void testBlankPromptRejected() {
        GenerateIllustrationRequest req = new GenerateIllustrationRequest("   ");

        assertThrows(IllegalArgumentException.class, () ->
                illustrationService.generateIllustration(cbeCase.getId(), req, "TN-POL-1001", null)
        );
    }

    @Test
    @DisplayName("Should list illustrations for authorized case")
    void testGetIllustrationsByCase() {
        CaseIllustration ill = new CaseIllustration();
        ill.setId(UUID.randomUUID());
        ill.setCaseEntity(cbeCase);
        ill.setPrompt("Sample sketch");
        ill.setIsAiGenerated(true);
        ill.setStatus("AI_ILLUSTRATION");
        ill.setCreatedBy(cbeOfficer);

        when(officerRepository.findByPoliceId("TN-POL-1001")).thenReturn(Optional.of(cbeOfficer));
        when(caseRepository.findById(cbeCase.getId())).thenReturn(Optional.of(cbeCase));
        when(illustrationRepository.findByCaseEntityIdOrderByCreatedAtDesc(cbeCase.getId())).thenReturn(List.of(ill));

        List<CaseIllustrationResponse> list = illustrationService.getIllustrationsByCase(cbeCase.getId(), "TN-POL-1001");
        assertEquals(1, list.size());
        assertEquals("Sample sketch", list.get(0).getPrompt());
        assertTrue(list.get(0).getIsAiGenerated());
    }

    @Test
    @DisplayName("Should retrieve illustration file publicly without requiring officer authentication")
    void testGetIllustrationFilePublic() {
        UUID illId = UUID.randomUUID();
        String storagePath = storageService.storeIllustrationBytes(cbeCase.getId(), illId, new byte[]{1, 2, 3, 4}, "image/jpeg");

        CaseIllustration ill = new CaseIllustration();
        ill.setId(illId);
        ill.setCaseEntity(cbeCase);
        ill.setPrompt("Sample sketch");
        ill.setIsAiGenerated(true);
        ill.setMimeType("image/jpeg");
        ill.setStoragePath(storagePath);

        when(illustrationRepository.findByIdAndCaseEntityId(illId, cbeCase.getId())).thenReturn(Optional.of(ill));

        IllustrationService.StorageServiceFileResult result = illustrationService.getIllustrationFilePublic(cbeCase.getId(), illId);
        assertNotNull(result);
        assertEquals("image/jpeg", result.getContentType());
        assertEquals("illustration_" + illId + ".jpg", result.getFilename());
        assertNotNull(result.getResource());
        assertTrue(result.getResource().exists());
    }
}
