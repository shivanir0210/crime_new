package com.crimesphere.backend.service;

import com.crimesphere.backend.dto.OcrComparisonResultDto;
import com.crimesphere.backend.dto.OfficerRegistrationRequest;
import com.crimesphere.backend.entity.PoliceStation;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for OCR extraction and automated identity verification of Police ID documents.
 * Keeps OCR keys and processing securely on the backend.
 */
@Service
public class OfficerOcrService {

    private static final Logger logger = LoggerFactory.getLogger(OfficerOcrService.class);

    private final String geminiApiKey;
    private final String modelName;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public OfficerOcrService(
            @Value("${gemini.api.key:${GEMINI_API_KEY:}}") String geminiApiKey,
            @Value("${gemini.ocr.model:${GEMINI_OCR_MODEL:gemini-1.5-flash}}") String modelName,
            ObjectMapper objectMapper
    ) {
        this.geminiApiKey = geminiApiKey != null ? geminiApiKey.trim() : "";
        this.modelName = StringUtils.hasText(modelName) ? modelName.trim() : "gemini-1.5-flash";
        this.objectMapper = objectMapper != null ? objectMapper : new ObjectMapper();
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();
    }

    public static class OcrExtractionResult {
        private final String ocrStatus; // PASSED, REVIEW_REQUIRED, FAILED
        private final String detectedName;
        private final String detectedPoliceId;
        private final String detectedStation;
        private final String detectedRole;
        private final String rawText;
        private final boolean nameMatches;
        private final boolean policeIdMatches;
        private final boolean stationMatches;
        private final boolean roleMatches;
        private final String summaryMessage;

        public OcrExtractionResult(String ocrStatus, String detectedName, String detectedPoliceId,
                                   String detectedStation, String detectedRole, String rawText,
                                   boolean nameMatches, boolean policeIdMatches, boolean stationMatches,
                                   boolean roleMatches, String summaryMessage) {
            this.ocrStatus = ocrStatus;
            this.detectedName = detectedName;
            this.detectedPoliceId = detectedPoliceId;
            this.detectedStation = detectedStation;
            this.detectedRole = detectedRole;
            this.rawText = rawText;
            this.nameMatches = nameMatches;
            this.policeIdMatches = policeIdMatches;
            this.stationMatches = stationMatches;
            this.roleMatches = roleMatches;
            this.summaryMessage = summaryMessage;
        }

        public String getOcrStatus() {
            return ocrStatus;
        }

        public String getDetectedName() {
            return detectedName;
        }

        public String getDetectedPoliceId() {
            return detectedPoliceId;
        }

        public String getDetectedStation() {
            return detectedStation;
        }

        public String getDetectedRole() {
            return detectedRole;
        }

        public String getRawText() {
            return rawText;
        }

        public boolean isNameMatches() {
            return nameMatches;
        }

        public boolean isPoliceIdMatches() {
            return policeIdMatches;
        }

        public boolean isStationMatches() {
            return stationMatches;
        }

        public boolean isRoleMatches() {
            return roleMatches;
        }

        public String getSummaryMessage() {
            return summaryMessage;
        }

        public OcrComparisonResultDto toDto() {
            return new OcrComparisonResultDto(
                    ocrStatus,
                    detectedName,
                    detectedPoliceId,
                    detectedStation,
                    detectedRole,
                    nameMatches,
                    policeIdMatches,
                    stationMatches,
                    roleMatches,
                    summaryMessage
            );
        }
    }

    /**
     * Extracts text and structured fields from uploaded ID document and compares with registration details.
     */
    public OcrExtractionResult processAndVerifyIdDocument(MultipartFile file, OfficerRegistrationRequest request, PoliceStation station) {
        if (file == null || file.isEmpty()) {
            return new OcrExtractionResult(
                    "FAILED",
                    null, null, null, null,
                    "No document provided",
                    false, false, false, false,
                    "ID Document upload is required."
            );
        }

        Map<String, String> extractedFields = new HashMap<>();
        String rawText = "";

        try {
            byte[] fileBytes = file.getBytes();
            String contentType = file.getContentType() != null ? file.getContentType() : "application/octet-stream";

            // 1. Try Gemini Multimodal Vision OCR if key is present
            if (StringUtils.hasText(geminiApiKey) && !isPlaceholder(geminiApiKey) && isImage(contentType)) {
                try {
                    Map<String, String> geminiResult = callGeminiVisionOcr(fileBytes, contentType);
                    if (geminiResult != null && !geminiResult.isEmpty()) {
                        extractedFields.putAll(geminiResult);
                        rawText = geminiResult.getOrDefault("rawText", "");
                    }
                } catch (Exception e) {
                    logger.warn("Gemini vision OCR call failed, falling back to local heuristic extractor: {}", e.getMessage());
                }
            }

            // 2. If Gemini didn't extract all fields or is offline, run heuristic document analyzer
            if (extractedFields.isEmpty() || extractedFields.get("name") == null) {
                Map<String, String> heuristicResult = extractHeuristicFields(fileBytes, file.getOriginalFilename(), request);
                for (Map.Entry<String, String> entry : heuristicResult.entrySet()) {
                    extractedFields.putIfAbsent(entry.getKey(), entry.getValue());
                }
                if (rawText.isBlank()) {
                    rawText = heuristicResult.getOrDefault("rawText", "");
                }
            }
        } catch (Exception e) {
            logger.error("Error during ID document OCR processing: {}", e.getMessage());
            rawText = "Extraction error: " + e.getMessage();
        }

        String detectedName = extractedFields.get("name");
        String detectedPoliceId = extractedFields.get("policeId");
        String detectedStation = extractedFields.get("station");
        String detectedRole = extractedFields.get("role");

        // Comparison logic
        boolean nameMatches = isFieldMatching(request.getFullName(), detectedName);
        boolean policeIdMatches = isPoliceIdMatching(request.getPoliceId(), detectedPoliceId);
        boolean stationMatches = isStationMatching(station, detectedStation);
        boolean roleMatches = isFieldMatching(request.getRole(), detectedRole);

        String ocrStatus;
        String summaryMessage;

        if (nameMatches && policeIdMatches && stationMatches && roleMatches) {
            ocrStatus = "PASSED";
            summaryMessage = "OCR verification passed. All ID card details match registration records.";
        } else if (detectedName == null && detectedPoliceId == null && detectedStation == null) {
            ocrStatus = "REVIEW_REQUIRED";
            summaryMessage = "OCR could not automatically detect all ID card fields. Station Head manual review required.";
        } else {
            ocrStatus = "REVIEW_REQUIRED";
            StringBuilder mismatches = new StringBuilder("Discrepancies flagged for review: ");
            List<String> list = new ArrayList<>();
            if (!nameMatches) list.add("Name mismatch");
            if (!policeIdMatches) list.add("Police ID mismatch");
            if (!stationMatches) list.add("Station mismatch");
            if (!roleMatches) list.add("Role mismatch");
            mismatches.append(String.join(", ", list)).append(". Manual verification required.");
            summaryMessage = mismatches.toString();
        }

        logger.info("OCR Extraction completed for Police ID: {} - Status: {} (Matches: Name={}, PID={}, Station={}, Role={})",
                request.getPoliceId(), ocrStatus, nameMatches, policeIdMatches, stationMatches, roleMatches);

        return new OcrExtractionResult(
                ocrStatus,
                detectedName,
                detectedPoliceId,
                detectedStation,
                detectedRole,
                rawText,
                nameMatches,
                policeIdMatches,
                stationMatches,
                roleMatches,
                summaryMessage
        );
    }

    /**
     * Intelligent local heuristic text and ID card parser.
     */
    private Map<String, String> extractHeuristicFields(byte[] fileBytes, String filename, OfficerRegistrationRequest request) {
        Map<String, String> result = new HashMap<>();
        String contentStr = new String(fileBytes, StandardCharsets.UTF_8);

        // Check if file is text/json/synthetic or contains readable strings
        StringBuilder rawText = new StringBuilder();
        rawText.append("Document: ").append(filename != null ? filename : "officer_id.pdf").append("\n");

        // Check for key-value text in raw bytes
        String cleanText = contentStr.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", " ");
        rawText.append(cleanText);

        // Regex patterns for Indian Police ID Cards & Badges
        Pattern pidPattern = Pattern.compile("(?i)(?:POLICE\\s*ID|PID|BADGE\\s*NO|ID\\s*NO|ID)[:\\s-]*([A-Z0-9-]{4,20})");
        Matcher pidMatcher = pidPattern.matcher(cleanText);
        if (pidMatcher.find()) {
            result.put("policeId", pidMatcher.group(1).trim());
        } else {
            // Check standalone PID format e.g. TN-POL-1001, POL-1001
            Pattern codePattern = Pattern.compile("([A-Z]{2,3}-POL-\\d{3,6}|POL-\\d{3,6})");
            Matcher codeMatcher = codePattern.matcher(cleanText);
            if (codeMatcher.find()) {
                result.put("policeId", codeMatcher.group(1).trim());
            }
        }

        Pattern namePattern = Pattern.compile("(?i)(?:NAME|OFFICER\\s*NAME|FULL\\s*NAME)[:\\s-]*([A-Za-z.\\s]{2,40})");
        Matcher nameMatcher = namePattern.matcher(cleanText);
        if (nameMatcher.find()) {
            result.put("name", nameMatcher.group(1).trim());
        }

        Pattern stationPattern = Pattern.compile("(?i)(?:STATION|POLICE\\s*STATION|POSTING|LOCATION)[:\\s-]*([A-Za-z0-9,\\s-]{3,60})");
        Matcher stationMatcher = stationPattern.matcher(cleanText);
        if (stationMatcher.find()) {
            result.put("station", stationMatcher.group(1).trim());
        }

        Pattern rolePattern = Pattern.compile("(?i)(?:ROLE|DESIGNATION|RANK)[:\\s-]*([A-Za-z\\s-]{3,40})");
        Matcher roleMatcher = rolePattern.matcher(cleanText);
        if (roleMatcher.find()) {
            result.put("role", roleMatcher.group(1).trim());
        }

        // If filename contains clues (e.g. mock demo files like "TN-POL-1001_id.jpg" or "R_Kumar_id.png")
        if (filename != null) {
            String lowerFn = filename.toLowerCase();
            if (result.get("policeId") == null && lowerFn.contains("pol")) {
                Pattern fnPid = Pattern.compile("([a-z0-9]+-pol-[0-9]+|pol-[0-9]+)", Pattern.CASE_INSENSITIVE);
                Matcher fnMatcher = fnPid.matcher(filename);
                if (fnMatcher.find()) {
                    result.put("policeId", fnMatcher.group(1).toUpperCase());
                }
            }
        }

        // If the document content explicitly contains the candidate's exact Police ID or Name, detect them
        if (result.get("policeId") == null && request != null && request.getPoliceId() != null && cleanText.contains(request.getPoliceId())) {
            result.put("policeId", request.getPoliceId());
        }
        if (result.get("name") == null && request != null && request.getFullName() != null && cleanText.toLowerCase().contains(request.getFullName().toLowerCase())) {
            result.put("name", request.getFullName());
        }

        result.put("rawText", rawText.toString());
        return result;
    }

    /**
     * Calls Gemini Multimodal Vision API to parse official Police ID cards.
     */
    private Map<String, String> callGeminiVisionOcr(byte[] imageBytes, String mimeType) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":generateContent?key=" + geminiApiKey;

        String base64Image = Base64.getEncoder().encodeToString(imageBytes);

        String prompt = "You are a secure Law Enforcement OCR verification service. " +
                "Extract the following fields from this Police Officer ID Document:\n" +
                "- Full Name\n" +
                "- Police ID Number\n" +
                "- Police Station Name / Department\n" +
                "- Role / Designation / Rank\n\n" +
                "Respond ONLY with a valid JSON object with keys: 'name', 'policeId', 'station', 'role', 'rawText'. " +
                "If a field cannot be read, use null.";

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> imagePart = Map.of(
                "inlineData", Map.of(
                        "mimeType", mimeType,
                        "data", base64Image
                )
        );

        Map<String, Object> contentObj = Map.of("parts", List.of(textPart, imagePart));
        Map<String, Object> requestPayload = Map.of("contents", List.of(contentObj));

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(30))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && !candidates.isEmpty()) {
                JsonNode parts = candidates.get(0).path("content").path("parts");
                if (parts.isArray() && !parts.isEmpty()) {
                    String responseText = parts.get(0).path("text").asText("");
                    return parseJsonFromOcrResponse(responseText);
                }
            }
        }

        return Collections.emptyMap();
    }

    private Map<String, String> parseJsonFromOcrResponse(String responseText) {
        Map<String, String> map = new HashMap<>();
        try {
            String jsonStr = responseText.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            } else if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();

            JsonNode node = objectMapper.readTree(jsonStr);
            if (node.hasNonNull("name")) map.put("name", node.get("name").asText());
            if (node.hasNonNull("policeId")) map.put("policeId", node.get("policeId").asText());
            if (node.hasNonNull("station")) map.put("station", node.get("station").asText());
            if (node.hasNonNull("role")) map.put("role", node.get("role").asText());
            if (node.hasNonNull("rawText")) map.put("rawText", node.get("rawText").asText());
        } catch (Exception e) {
            logger.warn("Could not parse JSON from Gemini OCR response: {}", e.getMessage());
        }
        return map;
    }

    public boolean isFieldMatching(String entered, String detected) {
        if (entered == null || detected == null) return false;
        String e = normalize(entered);
        String d = normalize(detected);
        if (e.isEmpty() || d.isEmpty()) return false;
        if (e.equals(d)) return true;
        if (d.contains(e) || e.contains(d)) return true;

        // Token match
        String[] eTokens = e.split("\\s+");
        String[] dTokens = d.split("\\s+");
        int matchCount = 0;
        for (String et : eTokens) {
            for (String dt : dTokens) {
                if (et.equals(dt) || (et.length() > 3 && dt.contains(et))) {
                    matchCount++;
                    break;
                }
            }
        }
        return matchCount >= Math.min(eTokens.length, 2);
    }

    public boolean isPoliceIdMatching(String enteredPid, String detectedPid) {
        if (enteredPid == null || detectedPid == null) return false;
        String e = normalize(enteredPid);
        String d = normalize(detectedPid);
        if (e.isEmpty() || d.isEmpty()) return false;
        return e.equals(d) || e.replace("-", "").equals(d.replace("-", ""));
    }

    public boolean isStationMatching(PoliceStation station, String detectedStation) {
        if (station == null || detectedStation == null) return false;
        String detected = normalize(detectedStation);
        if (detected.isEmpty()) return false;
        String code = normalize(station.getStationCode());
        String name = normalize(station.getStationName());
        String district = normalize(station.getDistrict());
        return detected.contains(code) || detected.contains(name) || name.contains(detected) || detected.contains(district);
    }

    private String normalize(String str) {
        if (str == null) return "";
        return str.toLowerCase().replaceAll("[^a-z0-9]", " ").trim().replaceAll("\\s+", " ");
    }

    private boolean isImage(String contentType) {
        return contentType != null && (
                contentType.equalsIgnoreCase("image/jpeg") ||
                contentType.equalsIgnoreCase("image/jpg") ||
                contentType.equalsIgnoreCase("image/png") ||
                contentType.equalsIgnoreCase("image/webp")
        );
    }

    private boolean isPlaceholder(String val) {
        if (val == null) return true;
        String trimmed = val.trim().toLowerCase();
        return trimmed.isEmpty() ||
                trimmed.contains("replace") ||
                trimmed.contains("placeholder") ||
                trimmed.contains("change_me") ||
                trimmed.equals("xxx");
    }
}
