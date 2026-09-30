package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.fir.CreateFIRRequest;
import com.crimesphere.backend.dto.fir.FIRResponse;
import com.crimesphere.backend.dto.fir.UpdateFIRRequest;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.FIR;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.entity.PoliceStation;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.FIRRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.Year;
import java.util.UUID;

@Service
public class FIRService {

    private static final Logger logger = LoggerFactory.getLogger(FIRService.class);

    private final FIRRepository firRepository;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;

    public FIRService(FIRRepository firRepository,
                      CaseRepository caseRepository,
                      PoliceOfficerRepository officerRepository,
                      AuditLogRepository auditLogRepository) {
        this.firRepository = firRepository;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public FIRResponse createFIR(UUID caseId, CreateFIRRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (firRepository.existsByCaseEntityId(caseId)) {
            throw new IllegalArgumentException("An FIR is already registered for this case. Only one FIR per case is permitted.");
        }

        PoliceStation station = parentCase.getStation();
        String stationCodeShort = extractStationShortCode(station);
        String firNumber = generateFirNumber(stationCodeShort);

        FIR fir = new FIR();
        fir.setCaseEntity(parentCase);
        fir.setFirNumber(firNumber);
        fir.setPoliceStation(station != null ? station.getStationName() : "PRECINCT POLICE STATION");
        fir.setRegistrationDate(request.getRegistrationDate() != null ? request.getRegistrationDate() : OffsetDateTime.now());
        fir.setComplainantName(request.getComplainantName() != null ? request.getComplainantName().trim() : "State / Police Complainant");
        fir.setComplainantContact(request.getComplainantContact() != null ? request.getComplainantContact().trim() : "");
        fir.setIncidentDate(request.getIncidentDate());
        fir.setIncidentLocation(request.getIncidentLocation() != null ? request.getIncidentLocation().trim() : "");
        fir.setOffenceType(request.getOffenceType() != null && !request.getOffenceType().trim().isEmpty() 
                ? request.getOffenceType().trim().toUpperCase() : parentCase.getCaseType());
        fir.setSectionsOfLaw(request.getSectionsOfLaw() != null ? request.getSectionsOfLaw().trim() : "");
        fir.setDescription(request.getDescription() != null ? request.getDescription().trim() : "");
        fir.setCreatedBy(officer);

        FIR savedFir = firRepository.save(fir);

        // Update parent case updated_at
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "FIR_CREATED", "SUCCESS", httpRequest);
        logger.info("FIR {} created successfully for case: {} by officer: {}", firNumber, parentCase.getCaseNumber(), policeId);

        return new FIRResponse(savedFir);
    }

    @Transactional(readOnly = true)
    public FIRResponse getFIRByCaseId(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        FIR fir = firRepository.findByCaseEntityId(parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No FIR record found registered for this case"));

        return new FIRResponse(fir);
    }

    @Transactional
    public FIRResponse updateFIR(UUID caseId, UpdateFIRRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        FIR fir = firRepository.findByCaseEntityId(parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("No FIR record found to update for this case"));

        if (request.getComplainantName() != null) fir.setComplainantName(request.getComplainantName().trim());
        if (request.getComplainantContact() != null) fir.setComplainantContact(request.getComplainantContact().trim());
        if (request.getRegistrationDate() != null) fir.setRegistrationDate(request.getRegistrationDate());
        if (request.getIncidentDate() != null) fir.setIncidentDate(request.getIncidentDate());
        if (request.getIncidentLocation() != null) fir.setIncidentLocation(request.getIncidentLocation().trim());
        if (request.getOffenceType() != null) fir.setOffenceType(request.getOffenceType().trim().toUpperCase());
        if (request.getSectionsOfLaw() != null) fir.setSectionsOfLaw(request.getSectionsOfLaw().trim());
        if (request.getDescription() != null) fir.setDescription(request.getDescription().trim());

        FIR updatedFir = firRepository.save(fir);

        // Update parent case updated_at
        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "FIR_UPDATED", "SUCCESS", httpRequest);
        logger.info("FIR {} updated successfully for case: {} by officer: {}", fir.getFirNumber(), parentCase.getCaseNumber(), policeId);

        return new FIRResponse(updatedFir);
    }

    private synchronized String generateFirNumber(String stationCodeShort) {
        int currentYear = Year.now().getValue();
        try {
            Long nextSeq = firRepository.getNextFirNumberSequence();
            return String.format("FIR-%s-%d-%04d", stationCodeShort, currentYear, nextSeq);
        } catch (Exception e) {
            long fallback = System.currentTimeMillis() % 10000;
            return String.format("FIR-%s-%d-%04d", stationCodeShort, currentYear, fallback);
        }
    }

    private String extractStationShortCode(PoliceStation station) {
        if (station == null || station.getStationCode() == null) return "POL";
        String code = station.getStationCode().toUpperCase();
        if (code.contains("-")) {
            String[] parts = code.split("-");
            return parts[parts.length - 1]; // e.g. TN-POLICE-CBE -> CBE
        }
        return code.length() > 3 ? code.substring(0, 3) : code;
    }

    private Case getAuthorizedCase(UUID caseId, PoliceOfficer officer) {
        UUID stationId = officer.getStation() != null ? officer.getStation().getId() : null;
        if (stationId == null) {
            throw new IllegalStateException("Police officer has no assigned station");
        }
        return caseRepository.findByIdAndStationId(caseId, stationId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found or access restricted to your station"));
    }

    private PoliceOfficer getAuthenticatedOfficer(String policeId) {
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated police officer record not found"));
    }

    private void recordAuditLog(PoliceOfficer officer, String action, String status, HttpServletRequest request) {
        try {
            String clientIp = extractClientIp(request);
            String userAgent = request != null ? request.getHeader("User-Agent") : "Unknown";
            AuditLog log = new AuditLog(null, officer, action, status, clientIp, userAgent);
            auditLogRepository.save(log);
        } catch (Exception e) {
            logger.warn("Failed to record audit log for action {}: {}", action, e.getMessage());
        }
    }

    private String extractClientIp(HttpServletRequest request) {
        if (request == null) return "Unknown";
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isBlank() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else if (ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip != null ? ip : "Unknown";
    }
}
