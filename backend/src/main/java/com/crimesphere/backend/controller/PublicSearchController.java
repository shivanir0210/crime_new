package com.crimesphere.backend.controller;

import com.crimesphere.backend.dto.public_search.*;
import com.crimesphere.backend.entity.Case;
import com.crimesphere.backend.entity.PoliceOfficer;
import com.crimesphere.backend.exception.ResourceNotFoundException;
import com.crimesphere.backend.repository.CaseRepository;
import com.crimesphere.backend.repository.PoliceOfficerRepository;
import com.crimesphere.backend.service.CaseSearchQueryService;
import com.crimesphere.backend.service.PublicInformationService;
import com.crimesphere.backend.service.PublicSearchService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class PublicSearchController {

    private final PublicSearchService publicSearchService;
    private final PublicInformationService publicInformationService;
    private final CaseSearchQueryService caseSearchQueryService;
    private final CaseRepository caseRepository;
    private final PoliceOfficerRepository officerRepository;

    public PublicSearchController(PublicSearchService publicSearchService,
                                  PublicInformationService publicInformationService,
                                  CaseSearchQueryService caseSearchQueryService,
                                  CaseRepository caseRepository,
                                  PoliceOfficerRepository officerRepository) {
        this.publicSearchService = publicSearchService;
        this.publicInformationService = publicInformationService;
        this.caseSearchQueryService = caseSearchQueryService;
        this.caseRepository = caseRepository;
        this.officerRepository = officerRepository;
    }

    /**
     * POST /api/public-search
     * General public news / web search for authenticated investigators.
     */
    /**
     * POST /api/public-search
     * General public news / web search for authenticated investigators.
     */
    @PostMapping("/public-search")
    public ResponseEntity<PublicSearchResponse> searchPublic(@RequestBody PublicSearchRequest request) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        PublicSearchResponse response = publicSearchService.searchPublicInformation(request, officer);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/public-search/news
     * Authenticated GET news search endpoint.
     */
    @GetMapping("/public-search/news")
    public ResponseEntity<PublicSearchResponse> searchPublicNewsGet(@RequestParam("q") String query,
                                                                   @RequestParam(value = "caseId", required = false) UUID caseId,
                                                                   @RequestParam(value = "from", required = false) String from,
                                                                   @RequestParam(value = "to", required = false) String to,
                                                                   @RequestParam(value = "page", defaultValue = "1") int page,
                                                                   @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        PublicSearchRequest request = new PublicSearchRequest();
        request.setQuery(query);
        request.setCaseId(caseId);
        request.setDateFrom(from);
        request.setDateTo(to);
        request.setPage(page);
        request.setPageSize(pageSize);
        PublicSearchResponse response = publicSearchService.searchPublicInformation(request, officer);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cases/{caseId}/public-search
     * Case-aware public news / web search with station authorization.
     */
    @PostMapping("/cases/{caseId}/public-search")
    public ResponseEntity<PublicSearchResponse> searchCasePublic(@PathVariable UUID caseId,
                                                                 @RequestBody PublicSearchRequest request) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        request.setCaseId(caseId);
        PublicSearchResponse response = publicSearchService.searchPublicInformation(request, officer);
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/cases/{caseId}/public-search/suggested-queries
     * Generates privacy-preserving suggested search queries tailored for this case.
     */
    @GetMapping("/cases/{caseId}/public-search/suggested-queries")
    public ResponseEntity<SuggestedQueriesResponse> getSuggestedQueries(@PathVariable UUID caseId) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        Case crimeCase = caseRepository.findById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Case not found: " + caseId));
        validateStationAccess(officer, crimeCase);

        SuggestedQueriesResponse response = caseSearchQueryService.generateSuggestedQueries(crimeCase);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/cases/{caseId}/public-information
     * POST /api/cases/{caseId}/public-search/references
     * Saves / bookmarks a public information search result to the case record.
     */
    @PostMapping({"/cases/{caseId}/public-information", "/cases/{caseId}/public-search/references"})
    public ResponseEntity<PublicInformationResponse> savePublicInformation(@PathVariable UUID caseId,
                                                                           @RequestBody SavePublicInformationRequest request) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        PublicInformationResponse response = publicInformationService.savePublicInformation(caseId, request, officer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * GET /api/cases/{caseId}/public-information
     * GET /api/cases/{caseId}/public-search/references
     * Lists all saved public information items for a case.
     */
    @GetMapping({"/cases/{caseId}/public-information", "/cases/{caseId}/public-search/references"})
    public ResponseEntity<List<PublicInformationResponse>> getCasePublicInformation(@PathVariable UUID caseId) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        List<PublicInformationResponse> list = publicInformationService.getCasePublicInformation(caseId, officer);
        return ResponseEntity.ok(list);
    }

    /**
     * GET /api/cases/{caseId}/public-information/{id}
     * Retrieves a single saved public information item.
     */
    @GetMapping("/cases/{caseId}/public-information/{id}")
    public ResponseEntity<PublicInformationResponse> getPublicInformationItem(@PathVariable UUID caseId,
                                                                              @PathVariable UUID id) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        PublicInformationResponse response = publicInformationService.getPublicInformationItem(caseId, id, officer);
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/cases/{caseId}/public-information/{id}
     * PATCH /api/cases/{caseId}/public-search/references/{id}/status
     * Updates verification status and investigator notes.
     */
    @RequestMapping(value = {"/cases/{caseId}/public-information/{id}", "/cases/{caseId}/public-search/references/{id}/status"},
                    method = {RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<PublicInformationResponse> updatePublicInformationItem(@PathVariable UUID caseId,
                                                                                 @PathVariable("id") UUID id,
                                                                                 @RequestBody UpdatePublicInformationRequest request) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        PublicInformationResponse response = publicInformationService.updatePublicInformationItem(caseId, id, request, officer);
        return ResponseEntity.ok(response);
    }

    /**
     * DELETE /api/cases/{caseId}/public-information/{id}
     * Deletes a saved public information item and records an audit log.
     */
    @DeleteMapping("/cases/{caseId}/public-information/{id}")
    public ResponseEntity<Map<String, Object>> deletePublicInformationItem(@PathVariable UUID caseId,
                                                                           @PathVariable UUID id) {
        PoliceOfficer officer = getAuthenticatedOfficer();
        publicInformationService.deletePublicInformationItem(caseId, id, officer);
        return ResponseEntity.ok(Map.of("message", "Public information item removed successfully", "id", id));
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

    private PoliceOfficer getAuthenticatedOfficer() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new BadCredentialsException("Unauthenticated request");
        }
        String policeId = authentication.getName();
        return officerRepository.findByPoliceId(policeId)
                .orElseThrow(() -> new BadCredentialsException("Authenticated officer not found: " + policeId));
    }
}
