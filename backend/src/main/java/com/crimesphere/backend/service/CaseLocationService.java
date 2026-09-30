package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.location.CaseLocationDTO;
import com.crimesphere.backend.dto.location.CreateCaseLocationRequest;
import com.crimesphere.backend.dto.location.LocationEventDTO;
import com.crimesphere.backend.dto.location.LocationIntelligenceSummaryDTO;
import com.crimesphere.backend.entity.AuditLog;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.CaseLocation;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.AuditLogRepository;
import com.crimesphere.backend.repository.CaseLocationRepository;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CaseLocationService {

    private static final Logger logger = LoggerFactory.getLogger(CaseLocationService.class);

    private static final Set<String> ALLOWED_ENTITY_TYPES = Set.of(
            "VICTIM", "CRIME_SCENE", "LAST_KNOWN", "SUSPECT", "WITNESS", "EVIDENCE", "OTHER"
    );

    private static final Set<String> ALLOWED_STATUSES = Set.of(
            "LIVE", "LAST_KNOWN", "HISTORICAL", "ESTIMATED"
    );

    private static final Set<String> ALLOWED_VERIFICATION = Set.of(
            "VERIFIED", "UNVERIFIED", "PENDING"
    );

    private final CaseLocationRepository caseLocationRepository;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final AuditLogRepository auditLogRepository;

    public CaseLocationService(CaseLocationRepository caseLocationRepository,
                               CaseRepository caseRepository,
                               PoliceOfficerRepository officerRepository,
                               AuditLogRepository auditLogRepository) {
        this.caseLocationRepository = caseLocationRepository;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(readOnly = true)
    public LocationIntelligenceSummaryDTO getLocationIntelligence(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        List<CaseLocation> locations = caseLocationRepository.findByCaseEntityIdOrderBySequenceOrderAscRecordedAtAsc(caseId);

        LocationIntelligenceSummaryDTO summary = new LocationIntelligenceSummaryDTO();
        summary.setCaseId(parentCase.getId());
        summary.setCaseNumber(parentCase.getCaseNumber());
        summary.setCaseTitle(parentCase.getTitle());
        summary.setCaseStatus(parentCase.getStatus() != null ? parentCase.getStatus() : "OPEN");
        summary.setCasePriority(parentCase.getPriority() != null ? parentCase.getPriority() : "MEDIUM");
        summary.setCaseType(parentCase.getCaseType() != null ? parentCase.getCaseType() : "GENERAL");
        summary.setStationName(parentCase.getStation() != null ? parentCase.getStation().getStationName() : "Coimbatore Station");
        summary.setAssignedOfficerName(parentCase.getCreatedBy() != null ? parentCase.getCreatedBy().getFullName() : "Assigned Officer");

        summary.setTotalLocations(locations.size());

        // Count statuses
        int liveCount = 0;
        int lastKnownCount = 0;
        int historicalCount = 0;
        int estimatedCount = 0;

        for (CaseLocation loc : locations) {
            String st = loc.getLocationStatus() != null ? loc.getLocationStatus().toUpperCase() : "HISTORICAL";
            switch (st) {
                case "LIVE" -> liveCount++;
                case "LAST_KNOWN" -> lastKnownCount++;
                case "ESTIMATED" -> estimatedCount++;
                default -> historicalCount++;
            }
        }

        summary.setLiveCount(liveCount);
        summary.setLastKnownCount(lastKnownCount);
        summary.setHistoricalCount(historicalCount);
        summary.setEstimatedCount(estimatedCount);

        // Calculate sequential distance using Haversine formula
        double totalDistanceKm = 0.0;
        if (locations.size() >= 2) {
            for (int i = 0; i < locations.size() - 1; i++) {
                CaseLocation p1 = locations.get(i);
                CaseLocation p2 = locations.get(i + 1);
                if (p1.getLatitude() != null && p1.getLongitude() != null &&
                        p2.getLatitude() != null && p2.getLongitude() != null) {
                    totalDistanceKm += calculateHaversineDistanceKm(
                            p1.getLatitude(), p1.getLongitude(),
                            p2.getLatitude(), p2.getLongitude()
                    );
                }
            }
        }

        double roundedDistance = Math.round(totalDistanceKm * 10.0) / 10.0;
        summary.setTotalDistanceKm(roundedDistance);
        summary.setFormattedDistance(roundedDistance > 0 ? roundedDistance + " km" : (locations.size() > 0 ? "0.0 km" : "N/A"));

        // Estimated travel time assuming average speed ~45 km/h for road travel
        int travelMinutes = 0;
        if (roundedDistance > 0) {
            travelMinutes = (int) Math.round((roundedDistance / 45.0) * 60.0);
        }
        summary.setEstimatedTravelTimeMinutes(travelMinutes);
        summary.setFormattedTravelTime(formatTravelTime(travelMinutes));

        // Convert locations to DTO
        List<CaseLocationDTO> locationDTOs = locations.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
        summary.setLocations(locationDTOs);

        // Create chronological events list (ordered by recordedAt ascending)
        List<CaseLocation> chronoSorted = new ArrayList<>(locations);
        chronoSorted.sort(Comparator.comparing(CaseLocation::getRecordedAt, Comparator.nullsLast(Comparator.naturalOrder())));

        List<LocationEventDTO> eventDTOs = chronoSorted.stream()
                .map(this::mapToEventDTO)
                .collect(Collectors.toList());
        summary.setEvents(eventDTOs);
        summary.setTotalEvents(eventDTOs.size());

        return summary;
    }

    @Transactional
    public CaseLocationDTO createLocation(UUID caseId, CreateCaseLocationRequest request, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        if (request.getLatitude() == null || request.getLongitude() == null) {
            throw new IllegalArgumentException("Geographic coordinates (latitude and longitude) are required");
        }
        if (request.getLocationName() == null || request.getLocationName().trim().isEmpty()) {
            throw new IllegalArgumentException("Location name is required");
        }
        if (request.getEntityName() == null || request.getEntityName().trim().isEmpty()) {
            throw new IllegalArgumentException("Entity or person name is required");
        }

        String entityType = request.getEntityType() != null && !request.getEntityType().trim().isEmpty()
                ? request.getEntityType().trim().toUpperCase() : "OTHER";
        if (!ALLOWED_ENTITY_TYPES.contains(entityType)) {
            entityType = "OTHER";
        }

        String status = request.getLocationStatus() != null && !request.getLocationStatus().trim().isEmpty()
                ? request.getLocationStatus().trim().toUpperCase() : "HISTORICAL";
        if (!ALLOWED_STATUSES.contains(status)) {
            status = "HISTORICAL";
        }

        String verification = request.getVerificationStatus() != null && !request.getVerificationStatus().trim().isEmpty()
                ? request.getVerificationStatus().trim().toUpperCase() : "VERIFIED";
        if (!ALLOWED_VERIFICATION.contains(verification)) {
            verification = "VERIFIED";
        }

        CaseLocation location = new CaseLocation();
        location.setCaseEntity(parentCase);
        location.setEntityType(entityType);
        location.setEntityId(request.getEntityId());
        location.setEntityName(request.getEntityName().trim());
        location.setLatitude(request.getLatitude());
        location.setLongitude(request.getLongitude());
        location.setLocationName(request.getLocationName().trim());
        location.setAddress(request.getAddress() != null ? request.getAddress().trim() : null);
        location.setRecordedAt(request.getRecordedAt() != null ? request.getRecordedAt() : OffsetDateTime.now());
        location.setSource(request.getSource() != null && !request.getSource().trim().isEmpty()
                ? request.getSource().trim() : "Authorized Officer Entry");
        location.setLocationStatus(status);
        location.setVerificationStatus(verification);
        location.setNotes(request.getNotes() != null ? request.getNotes().trim() : null);
        location.setSequenceOrder(request.getSequenceOrder() != null ? request.getSequenceOrder() : (int) caseLocationRepository.countByCaseEntityId(caseId) + 1);
        location.setCreatedBy(officer);

        CaseLocation saved = caseLocationRepository.save(location);

        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "LOCATION_INTELLIGENCE_CREATED", "SUCCESS", httpRequest);
        logger.info("Location record created (ID: {}) for case {} by officer {}", saved.getId(), parentCase.getCaseNumber(), policeId);

        return mapToDTO(saved);
    }

    @Transactional
    public void deleteLocation(UUID caseId, UUID locationId, String policeId, HttpServletRequest httpRequest) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        CaseLocation location = caseLocationRepository.findByIdAndCaseEntityId(locationId, parentCase.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Location record not found for case"));

        caseLocationRepository.delete(location);

        parentCase.setUpdatedAt(OffsetDateTime.now());
        caseRepository.save(parentCase);

        recordAuditLog(officer, "LOCATION_INTELLIGENCE_DELETED", "SUCCESS", httpRequest);
        logger.info("Location record deleted (ID: {}) for case {} by officer {}", locationId, parentCase.getCaseNumber(), policeId);
    }

    private CaseLocationDTO mapToDTO(CaseLocation loc) {
        CaseLocationDTO dto = new CaseLocationDTO();
        dto.setId(loc.getId());
        dto.setCaseId(loc.getCaseEntity().getId());
        dto.setEntityType(loc.getEntityType());
        dto.setEntityId(loc.getEntityId());
        dto.setEntityName(loc.getEntityName());
        dto.setLatitude(loc.getLatitude());
        dto.setLongitude(loc.getLongitude());
        dto.setLocationName(loc.getLocationName());
        dto.setAddress(loc.getAddress());
        dto.setRecordedAt(loc.getRecordedAt());
        dto.setSource(loc.getSource());
        dto.setLocationStatus(loc.getLocationStatus());
        dto.setVerificationStatus(loc.getVerificationStatus());
        dto.setNotes(loc.getNotes());
        dto.setSequenceOrder(loc.getSequenceOrder());
        dto.setCreatedByOfficerName(loc.getCreatedBy() != null ? loc.getCreatedBy().getFullName() : "System");
        dto.setCreatedAt(loc.getCreatedAt());
        dto.setUpdatedAt(loc.getUpdatedAt());
        return dto;
    }

    private LocationEventDTO mapToEventDTO(CaseLocation loc) {
        LocationEventDTO event = new LocationEventDTO();
        event.setId("loc-event-" + loc.getId());
        event.setLocationId(loc.getId());
        event.setRecordedAt(loc.getRecordedAt());
        if (loc.getRecordedAt() != null) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");
            event.setFormattedTime(loc.getRecordedAt().format(dtf));
        } else {
            event.setFormattedTime("N/A");
        }
        event.setEntityType(loc.getEntityType());
        event.setEntityName(loc.getEntityName());
        event.setLocationName(loc.getLocationName());
        event.setAddress(loc.getAddress());
        event.setSource(loc.getSource());
        event.setLocationStatus(loc.getLocationStatus());
        event.setVerificationStatus(loc.getVerificationStatus());
        event.setNotes(loc.getNotes());
        event.setLatitude(loc.getLatitude());
        event.setLongitude(loc.getLongitude());
        event.setSequenceOrder(loc.getSequenceOrder());
        return event;
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

    private double calculateHaversineDistanceKm(double lat1, double lon1, double lat2, double lon2) {
        final double EARTH_RADIUS = 6371.0; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return EARTH_RADIUS * c;
    }

    private String formatTravelTime(int totalMinutes) {
        if (totalMinutes <= 0) return "0m";
        int hours = totalMinutes / 60;
        int mins = totalMinutes % 60;
        if (hours > 0 && mins > 0) {
            return hours + "h " + mins + "m";
        } else if (hours > 0) {
            return hours + "h";
        } else {
            return mins + "m";
        }
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
