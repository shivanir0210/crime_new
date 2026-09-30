package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.public_search.SuggestedQueriesResponse;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.FIR;
import com.crimesphere.backend.repository.FIRRepository;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service to construct suggested public-information search queries from case parameters.
 * Strictly enforces privacy and data-minimization: never includes victim/suspect names,
 * contact numbers, or private addresses.
 */
@Service
public class CaseSearchQueryService {

    private final FIRRepository firRepository;

    public CaseSearchQueryService(FIRRepository firRepository) {
        this.firRepository = firRepository;
    }

    public SuggestedQueriesResponse generateSuggestedQueries(Case crimeCase) {
        Optional<FIR> firOpt = firRepository.findByCaseEntityId(crimeCase.getId());

        String location = extractGeneralLocation(crimeCase, firOpt);
        String caseType = extractCleanCaseType(crimeCase, firOpt);
        String timePeriod = extractTimePeriod(crimeCase, firOpt);

        Set<String> querySet = new LinkedHashSet<>();

        // Query 1: Location + Case Type + Timeframe (e.g., "Coimbatore murder August 2026")
        if (!location.isBlank() && !caseType.isBlank() && !timePeriod.isBlank()) {
            querySet.add(location + " " + caseType + " " + timePeriod);
        }

        // Query 2: Location + Police Investigation + Case Type
        if (!location.isBlank() && !caseType.isBlank()) {
            querySet.add(location + " police " + caseType + " investigation");
        }

        // Query 3: Location + Homicide/Theft/Crime Incident + Year
        String year = extractYear(crimeCase, firOpt);
        if (!location.isBlank() && !caseType.isBlank() && !year.isBlank()) {
            querySet.add(location + " " + caseType + " " + year);
        }

        // Query 4: FIR Offence Type if specific (e.g., "Coimbatore armed robbery")
        if (firOpt.isPresent() && firOpt.get().getOffenceType() != null && !firOpt.get().getOffenceType().isBlank()) {
            String cleanOffence = firOpt.get().getOffenceType().replaceAll("[^a-zA-Z0-9 ]", "").trim();
            if (!cleanOffence.isBlank() && !cleanOffence.equalsIgnoreCase(caseType)) {
                querySet.add(location + " " + cleanOffence.toLowerCase());
            }
        }

        // Fallback default query if queries are sparse
        if (querySet.isEmpty()) {
            querySet.add((location.isBlank() ? "police" : location + " police") + " incident investigation");
        }

        String explanation = "Suggested queries generated using case classification, general location, and incident timeframe. " +
                "Personal details (victim, suspect, witness names, phone numbers, and addresses) are strictly omitted.";

        return new SuggestedQueriesResponse(crimeCase.getId(), crimeCase.getCaseNumber(), new ArrayList<>(querySet), explanation);
    }

    private String extractGeneralLocation(Case crimeCase, Optional<FIR> firOpt) {
        if (firOpt.isPresent() && firOpt.get().getIncidentLocation() != null) {
            String loc = firOpt.get().getIncidentLocation().trim();
            // Clean up: if comma separated, take the city/district part
            if (loc.contains(",")) {
                String[] parts = loc.split(",");
                return parts[parts.length - 1].trim();
            }
            if (loc.length() > 0 && loc.length() <= 30) {
                return loc;
            }
        }

        if (crimeCase.getStation() != null) {
            if (crimeCase.getStation().getDistrict() != null && !crimeCase.getStation().getDistrict().isBlank()) {
                return crimeCase.getStation().getDistrict().trim();
            }
            if (crimeCase.getStation().getStationName() != null) {
                return crimeCase.getStation().getStationName().replace("Police Station", "").replace("Station", "").trim();
            }
        }

        return "Coimbatore";
    }

    private String extractCleanCaseType(Case crimeCase, Optional<FIR> firOpt) {
        if (crimeCase.getCaseType() != null && !crimeCase.getCaseType().isBlank()) {
            return crimeCase.getCaseType().toLowerCase().replace("_", " ").trim();
        }
        if (firOpt.isPresent() && firOpt.get().getOffenceType() != null) {
            return firOpt.get().getOffenceType().toLowerCase().trim();
        }
        return "crime";
    }

    private String extractTimePeriod(Case crimeCase, Optional<FIR> firOpt) {
        OffsetDateTime dt = null;
        if (firOpt.isPresent() && firOpt.get().getIncidentDate() != null) {
            dt = firOpt.get().getIncidentDate();
        } else if (crimeCase.getCreatedAt() != null) {
            dt = crimeCase.getCreatedAt();
        }

        if (dt != null) {
            return dt.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH));
        }
        return "";
    }

    private String extractYear(Case crimeCase, Optional<FIR> firOpt) {
        OffsetDateTime dt = null;
        if (firOpt.isPresent() && firOpt.get().getIncidentDate() != null) {
            dt = firOpt.get().getIncidentDate();
        } else if (crimeCase.getCreatedAt() != null) {
            dt = crimeCase.getCreatedAt();
        }

        if (dt != null) {
            return String.valueOf(dt.getYear());
        }
        return String.valueOf(OffsetDateTime.now().getYear());
    }
}
