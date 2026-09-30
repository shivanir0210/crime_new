package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.timeline.TimelineEventDTO;
import com.crimesphere.backend.entity.*;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.*;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;

@Service
public class CaseTimelineService {

    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;
    private final FIRRepository firRepository;
    private final VictimRepository victimRepository;
    private final SuspectRepository suspectRepository;
    private final WitnessRepository witnessRepository;
    private final EvidenceRepository evidenceRepository;
    private final CaseLocationRepository caseLocationRepository;

    public CaseTimelineService(CaseRepository caseRepository,
                               PoliceOfficerRepository officerRepository,
                               FIRRepository firRepository,
                               VictimRepository victimRepository,
                               SuspectRepository suspectRepository,
                               WitnessRepository witnessRepository,
                               EvidenceRepository evidenceRepository,
                               CaseLocationRepository caseLocationRepository) {
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
        this.firRepository = firRepository;
        this.victimRepository = victimRepository;
        this.suspectRepository = suspectRepository;
        this.witnessRepository = witnessRepository;
        this.evidenceRepository = evidenceRepository;
        this.caseLocationRepository = caseLocationRepository;
    }

    @Transactional(readOnly = true)
    public List<TimelineEventDTO> getCaseTimeline(UUID caseId, String policeId) {
        PoliceOfficer officer = getAuthenticatedOfficer(policeId);
        Case parentCase = getAuthorizedCase(caseId, officer);

        List<TimelineEventDTO> events = new ArrayList<>();

        // 1. Case Registration Milestone
        if (parentCase.getCreatedAt() != null) {
            String actor = parentCase.getCreatedBy() != null ? parentCase.getCreatedBy().getFullName() : "Station Officer";
            events.add(new TimelineEventDTO(
                    "case-created-" + parentCase.getId(),
                    parentCase.getId(),
                    "Case Registered: " + parentCase.getCaseNumber(),
                    "Case file initialized with classification " + parentCase.getCaseType() + " and priority " + parentCase.getPriority() + ".",
                    parentCase.getCreatedAt(),
                    "CASE_INITIATION",
                    "blue",
                    actor,
                    Map.of("caseNumber", parentCase.getCaseNumber(), "status", parentCase.getStatus() != null ? parentCase.getStatus() : "OPEN")
            ));
        }

        // 2. FIR Milestones (Incident occurred & FIR Registered)
        Optional<FIR> firOpt = firRepository.findByCaseEntityId(parentCase.getId());
        if (firOpt.isPresent()) {
            FIR fir = firOpt.get();
            if (fir.getIncidentDate() != null) {
                events.add(new TimelineEventDTO(
                        "fir-incident-" + fir.getId(),
                        parentCase.getId(),
                        "Crime Incident Occurred",
                        "Incident occurred at " + (fir.getIncidentLocation() != null ? fir.getIncidentLocation() : "Scene") + ". Offence: " + (fir.getOffenceType() != null ? fir.getOffenceType() : "Unknown"),
                        fir.getIncidentDate(),
                        "INCIDENT",
                        "red",
                        fir.getComplainantName() != null ? "Reported by " + fir.getComplainantName() : "Unknown Informant",
                        Map.of("location", fir.getIncidentLocation() != null ? fir.getIncidentLocation() : "")
                ));
            }
            if (fir.getRegistrationDate() != null || fir.getCreatedAt() != null) {
                OffsetDateTime regDate = fir.getRegistrationDate() != null ? fir.getRegistrationDate() : fir.getCreatedAt();
                String actor = fir.getCreatedBy() != null ? fir.getCreatedBy().getFullName() : "Duty Officer";
                events.add(new TimelineEventDTO(
                        "fir-registered-" + fir.getId(),
                        parentCase.getId(),
                        "First Information Report Lodged (" + fir.getFirNumber() + ")",
                        "Sections of law invoked: " + (fir.getSectionsOfLaw() != null ? fir.getSectionsOfLaw() : "IPC General") + ". Complainant: " + (fir.getComplainantName() != null ? fir.getComplainantName() : "N/A"),
                        regDate,
                        "FIR",
                        "amber",
                        actor,
                        Map.of("firNumber", fir.getFirNumber(), "policeStation", fir.getPoliceStation() != null ? fir.getPoliceStation() : "")
                ));
            }
        }

        // 3. Victim Records
        List<Victim> victims = victimRepository.findByCaseEntityIdOrderByCreatedAtAsc(parentCase.getId());
        for (Victim v : victims) {
            String actor = v.getCreatedBy() != null ? v.getCreatedBy().getFullName() : "Investigator";
            events.add(new TimelineEventDTO(
                    "victim-" + v.getId(),
                    parentCase.getId(),
                    "Victim Profile Added: " + v.getFullName(),
                    "Status: " + v.getStatus() + (v.getRelationshipToCase() != null ? " (" + v.getRelationshipToCase() + ")" : "") + ". " + (v.getDescription() != null ? v.getDescription() : ""),
                    v.getCreatedAt() != null ? v.getCreatedAt() : parentCase.getCreatedAt(),
                    "VICTIM",
                    "purple",
                    actor,
                    Map.of("victimName", v.getFullName(), "status", v.getStatus() != null ? v.getStatus() : "")
            ));
        }

        // 4. Suspect Records
        List<Suspect> suspects = suspectRepository.findByCaseEntityIdOrderByCreatedAtAsc(parentCase.getId());
        for (Suspect s : suspects) {
            String actor = s.getCreatedBy() != null ? s.getCreatedBy().getFullName() : "Investigator";
            events.add(new TimelineEventDTO(
                    "suspect-" + s.getId(),
                    parentCase.getId(),
                    "Suspect Cataloged: " + s.getFullName() + (s.getAlias() != null ? " (Alias: " + s.getAlias() + ")" : ""),
                    "Investigation status: " + s.getInvestigationStatus() + ". " + (s.getDescription() != null ? s.getDescription() : ""),
                    s.getCreatedAt() != null ? s.getCreatedAt() : parentCase.getCreatedAt(),
                    "SUSPECT",
                    "red",
                    actor,
                    Map.of("suspectName", s.getFullName(), "status", s.getInvestigationStatus() != null ? s.getInvestigationStatus() : "")
            ));
        }

        // 5. Witness Statements Recorded
        List<Witness> witnesses = witnessRepository.findByCaseEntityIdOrderByCreatedAtAsc(parentCase.getId());
        for (Witness w : witnesses) {
            String actor = w.getCreatedBy() != null ? w.getCreatedBy().getFullName() : "Investigator";
            OffsetDateTime witnessTime = w.getInterviewDate() != null ? w.getInterviewDate() : (w.getCreatedAt() != null ? w.getCreatedAt() : parentCase.getCreatedAt());
            events.add(new TimelineEventDTO(
                    "witness-" + w.getId(),
                    parentCase.getId(),
                    "Witness Statement Recorded: " + w.getFullName(),
                    "Type: " + w.getWitnessType() + " | Reliability: " + w.getReliabilityScore() + ". Statement: " + (w.getStatementSummary() != null ? w.getStatementSummary() : "Statement recorded in case file."),
                    witnessTime,
                    "WITNESS",
                    "cyan",
                    actor,
                    Map.of("witnessName", w.getFullName(), "witnessType", w.getWitnessType() != null ? w.getWitnessType() : "EYEWITNESS")
            ));
        }

        // 6. Evidence Collection & Verification Milestones
        List<Evidence> evidenceList = evidenceRepository.findByCaseEntityIdOrderByCreatedAtDesc(parentCase.getId());
        for (Evidence e : evidenceList) {
            String actor = e.getCollectedBy() != null ? e.getCollectedBy() : (e.getCreatedBy() != null ? e.getCreatedBy().getFullName() : "Forensic Team");
            OffsetDateTime eventTime = e.getCollectedAt() != null ? e.getCollectedAt() : (e.getCreatedAt() != null ? e.getCreatedAt() : parentCase.getCreatedAt());
            events.add(new TimelineEventDTO(
                    "evidence-collected-" + e.getId(),
                    parentCase.getId(),
                    "Evidence Secured: " + e.getTitle(),
                    "Category: " + e.getEvidenceType() + " | Status: " + e.getStatus() + (e.getCollectedLocation() != null ? " | Scene: " + e.getCollectedLocation() : "") + (e.getFileHash() != null ? " | SHA-256: " + e.getFileHash().substring(0, Math.min(12, e.getFileHash().length())) + "..." : ""),
                    eventTime,
                    "EVIDENCE",
                    "emerald",
                    actor,
                    Map.of("evidenceNumber", e.getEvidenceNumber() != null ? e.getEvidenceNumber() : "", "status", e.getStatus() != null ? e.getStatus() : "")
            ));
        }

        // 7. Location Intelligence Milestones
        List<CaseLocation> locations = caseLocationRepository.findByCaseEntityIdOrderByRecordedAtAsc(parentCase.getId());
        for (CaseLocation loc : locations) {
            String actor = loc.getCreatedBy() != null ? loc.getCreatedBy().getFullName() : "Intelligence Unit";
            String color = "rose";
            if ("CRIME_SCENE".equalsIgnoreCase(loc.getEntityType())) color = "red";
            else if ("VICTIM".equalsIgnoreCase(loc.getEntityType())) color = "cyan";
            else if ("SUSPECT".equalsIgnoreCase(loc.getEntityType())) color = "purple";
            else if ("LAST_KNOWN".equalsIgnoreCase(loc.getEntityType())) color = "amber";
            else if ("WITNESS".equalsIgnoreCase(loc.getEntityType())) color = "emerald";

            events.add(new TimelineEventDTO(
                    "loc-intel-" + loc.getId(),
                    parentCase.getId(),
                    "Location Logged: " + loc.getEntityName() + " (" + loc.getLocationName() + ")",
                    "Source: " + loc.getSource() + " | Status: " + loc.getLocationStatus() + " | Verification: " + loc.getVerificationStatus() + (loc.getNotes() != null ? " — " + loc.getNotes() : ""),
                    loc.getRecordedAt() != null ? loc.getRecordedAt() : parentCase.getCreatedAt(),
                    "LOCATION",
                    color,
                    actor,
                    Map.of("locationName", loc.getLocationName(), "entityType", loc.getEntityType(), "status", loc.getLocationStatus())
            ));
        }

        // Sort events chronologically (newest first)
        events.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            return b.getTimestamp().compareTo(a.getTimestamp());
        });

        return events;
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
}
