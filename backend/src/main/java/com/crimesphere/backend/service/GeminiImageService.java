package com.crimesphere.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class GeminiImageService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiImageService.class);

    private final String geminiApiKey;
    private final String modelName;
    private final int timeoutSeconds;
    private final boolean mockEnabled;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public GeminiImageService(
            @Value("${gemini.api.key:${GEMINI_API_KEY:}}") String geminiApiKey,
            @Value("${gemini.image.model:${GEMINI_IMAGE_MODEL:imagen-3.0-generate-002}}") String modelName,
            @Value("${gemini.image.timeout-seconds:${GEMINI_IMAGE_TIMEOUT_SECONDS:45}}") int timeoutSeconds,
            @Value("${gemini.image.mock-enabled:${GEMINI_IMAGE_MOCK_ENABLED:false}}") boolean mockEnabled,
            ObjectMapper objectMapper
    ) {
        this.geminiApiKey = geminiApiKey != null ? geminiApiKey.trim() : "";
        this.modelName = StringUtils.hasText(modelName) ? modelName.trim() : "imagen-3.0-generate-002";
        this.timeoutSeconds = timeoutSeconds > 0 ? timeoutSeconds : 45;
        this.mockEnabled = mockEnabled;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build();

        if (StringUtils.hasText(this.geminiApiKey)) {
            logger.info("GeminiImageService initialized with model: {} (API key configured)", this.modelName);
        } else {
            logger.info("GeminiImageService initialized in development/fallback visual mode (GEMINI_API_KEY not configured)");
        }
    }

    public static class GeneratedImageResult {
        private final byte[] imageBytes;
        private final String mimeType;
        private final String modelUsed;
        private final String revisedPrompt;

        public GeneratedImageResult(byte[] imageBytes, String mimeType, String modelUsed, String revisedPrompt) {
            this.imageBytes = imageBytes;
            this.mimeType = mimeType;
            this.modelUsed = modelUsed;
            this.revisedPrompt = revisedPrompt;
        }

        public byte[] getImageBytes() {
            return imageBytes;
        }

        public String getMimeType() {
            return mimeType;
        }

        public String getModelUsed() {
            return modelUsed;
        }

        public String getRevisedPrompt() {
            return revisedPrompt;
        }
    }

    /**
     * Builds standardized prompt for contextual forensic evidence illustration.
     */
    public String buildEvidenceIllustrationPrompt(String evidenceType, String title, String description, String caseTitle, String caseDescription) {
        String type = StringUtils.hasText(evidenceType) ? evidenceType.trim().replace("_", " ") : "Forensic Object";
        String evTitle = StringUtils.hasText(title) ? title.trim() : "Forensic Record";
        String evDesc = StringUtils.hasText(description) ? description.trim() : "Investigative evidence cataloged for case analysis.";
        String cContext = StringUtils.hasText(caseTitle) ? (caseTitle + (StringUtils.hasText(caseDescription) ? " — " + caseDescription : "")) : "Criminal Case Investigation";

        return String.format(
                "Create a realistic forensic documentation illustration representing:\n" +
                "Evidence type: %s\n" +
                "Evidence title: %s\n" +
                "Evidence brief: %s\n" +
                "Case context: %s\n\n" +
                "Create a professional fictional forensic illustration.\n" +
                "Do not depict a real person.\n" +
                "Do not claim that the image represents actual evidence.\n" +
                "Do not add unsupported facts.",
                type, evTitle, evDesc, cContext
        );
    }

    /**
     * Generates a synthetic scenario illustration from prompt using Google Gemini Imagen API.
     * Falls back safely to high-resolution forensic visualization if API key is not present or offline.
     */
    public GeneratedImageResult generateIllustration(String prompt, String aspectRatio) {
        return generateIllustration(prompt, aspectRatio, "OTHER", "Forensic Evidence");
    }

    public GeneratedImageResult generateIllustration(String prompt, String aspectRatio, String evidenceType, String title) {
        if (!StringUtils.hasText(prompt)) {
            throw new IllegalArgumentException("Prompt must not be empty");
        }

        String targetAspectRatio = StringUtils.hasText(aspectRatio) ? aspectRatio : "1:1";

        if (!mockEnabled && StringUtils.hasText(geminiApiKey)) {
            try {
                return callGeminiImagenApi(prompt, targetAspectRatio);
            } catch (Exception e) {
                logger.warn("Live Gemini Image API call failed: {}. Falling back to high-resolution forensic scene visualizer.", e.getMessage());
            }
        }

        return generateFallbackForensicIllustration(prompt, targetAspectRatio, evidenceType, title);
    }

    /**
     * Calls the official Google Gemini / Imagen 3 generation API.
     */
    private GeneratedImageResult callGeminiImagenApi(String prompt, String aspectRatio) throws Exception {
        String endpoint = "https://generativelanguage.googleapis.com/v1beta/models/" + modelName + ":predict?key=" + geminiApiKey;

        Map<String, Object> requestPayload = new HashMap<>();
        requestPayload.put("instances", List.of(Map.of("prompt", prompt)));

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("sampleCount", 1);
        parameters.put("aspectRatio", aspectRatio);
        parameters.put("outputMimeType", "image/jpeg");
        parameters.put("safetyFilterLevel", "block_medium_and_above");
        parameters.put("personGeneration", "allow_adult");
        requestPayload.put("parameters", parameters);

        String jsonBody = objectMapper.writeValueAsString(requestPayload);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() >= 200 && response.statusCode() < 300) {
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode predictions = root.path("predictions");
            if (predictions.isArray() && !predictions.isEmpty()) {
                JsonNode firstPrediction = predictions.get(0);
                String base64Data = null;
                String mimeType = "image/jpeg";

                if (firstPrediction.has("bytesBase64Encoded")) {
                    base64Data = firstPrediction.get("bytesBase64Encoded").asText();
                } else if (firstPrediction.has("image") && firstPrediction.get("image").has("imageBytes")) {
                    base64Data = firstPrediction.get("image").get("imageBytes").asText();
                }

                if (firstPrediction.has("mimeType")) {
                    mimeType = firstPrediction.get("mimeType").asText();
                }

                if (StringUtils.hasText(base64Data)) {
                    byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                    return new GeneratedImageResult(imageBytes, mimeType, modelName, prompt);
                }
            }
        }

        // Check if error message returned
        String errorMsg = "Gemini API HTTP status " + response.statusCode();
        try {
            JsonNode errRoot = objectMapper.readTree(response.body());
            if (errRoot.has("error") && errRoot.get("error").has("message")) {
                errorMsg = errRoot.get("error").get("message").asText();
            }
        } catch (Exception ignored) {}

        throw new RuntimeException("Failed to generate image via Gemini API: " + errorMsg);
    }

    /**
     * Generates a high-quality, formatted forensic scene illustration canvas.
     * Contains clear non-evidence banners, coordinate grids, and the prompt text.
     */
    public GeneratedImageResult generateFallbackForensicIllustration(String prompt, String aspectRatio) {
        return generateFallbackForensicIllustration(prompt, aspectRatio, "OTHER", "Forensic Record");
    }

    /**
     * Generates a high-quality, formatted forensic scene illustration canvas tailored to the evidence type.
     * Contains clear non-evidence banners, specialized theme graphics, and the prompt text.
     */
    public GeneratedImageResult generateFallbackForensicIllustration(String prompt, String aspectRatio, String evidenceType, String title) {
        int width = 1024;
        int height = 1024;

        if ("16:9".equals(aspectRatio)) {
            height = 576;
        } else if ("4:3".equals(aspectRatio)) {
            height = 768;
        } else if ("9:16".equals(aspectRatio)) {
            width = 576;
        }

        String typeUpper = StringUtils.hasText(evidenceType) ? evidenceType.trim().toUpperCase() : "OTHER";
        String displayTitle = StringUtils.hasText(title) ? title.trim() : "FORENSIC EVIDENCE SPECIMEN";

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        // Rendering hints for crisp visuals
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Determine visual theme based on evidence type
        Color bgStart, bgEnd, gridColor, accentColor, cardBg, themeBadgeBg, themeBadgeText;
        String themeBadgeLabel;

        if ("CCTV".equals(typeUpper) || "VIDEO".equals(typeUpper)) {
            bgStart = new Color(5, 18, 12);
            bgEnd = new Color(10, 28, 20);
            gridColor = new Color(16, 185, 129, 60);
            accentColor = new Color(16, 185, 129);
            cardBg = new Color(8, 24, 16, 230);
            themeBadgeBg = new Color(16, 185, 129, 40);
            themeBadgeText = new Color(52, 211, 153);
            themeBadgeLabel = "SURVEILLANCE / CCTV OPTICAL RECONSTRUCTION";
        } else if ("AUDIO".equals(typeUpper)) {
            bgStart = new Color(6, 20, 32);
            bgEnd = new Color(10, 30, 48);
            gridColor = new Color(6, 182, 212, 60);
            accentColor = new Color(6, 182, 212);
            cardBg = new Color(10, 25, 40, 230);
            themeBadgeBg = new Color(6, 182, 212, 40);
            themeBadgeText = new Color(103, 232, 249);
            themeBadgeLabel = "ACOUSTIC SPECTROGRAM & AUDIO FORENSICS";
        } else if ("DIGITAL_DEVICE".equals(typeUpper) || "CALL_RECORD".equals(typeUpper)) {
            bgStart = new Color(18, 12, 32);
            bgEnd = new Color(28, 18, 48);
            gridColor = new Color(168, 85, 247, 60);
            accentColor = new Color(168, 85, 247);
            cardBg = new Color(24, 16, 42, 230);
            themeBadgeBg = new Color(168, 85, 247, 40);
            themeBadgeText = new Color(216, 180, 254);
            themeBadgeLabel = "DIGITAL DEVICE / CDR TELEMETRY SPECIMEN";
        } else if ("DOCUMENT".equals(typeUpper) || "FORENSIC_REPORT".equals(typeUpper)) {
            bgStart = new Color(24, 20, 14);
            bgEnd = new Color(36, 28, 18);
            gridColor = new Color(245, 158, 11, 60);
            accentColor = new Color(245, 158, 11);
            cardBg = new Color(30, 24, 16, 230);
            themeBadgeBg = new Color(245, 158, 11, 40);
            themeBadgeText = new Color(252, 211, 77);
            themeBadgeLabel = "FORENSIC DOCUMENT & LAB EXAMINATION SPECIMEN";
        } else {
            bgStart = new Color(12, 14, 20);
            bgEnd = new Color(20, 24, 36);
            gridColor = new Color(239, 68, 68, 60);
            accentColor = new Color(239, 68, 68);
            cardBg = new Color(18, 22, 32, 230);
            themeBadgeBg = new Color(239, 68, 68, 40);
            themeBadgeText = new Color(252, 165, 165);
            themeBadgeLabel = "FORENSIC SPATIAL & SITUATIONAL RECONSTRUCTION";
        }

        // Background Gradient
        GradientPaint backgroundGradient = new GradientPaint(0, 0, bgStart, width, height, bgEnd);
        g2d.setPaint(backgroundGradient);
        g2d.fillRect(0, 0, width, height);

        // Technical Grid overlay
        g2d.setColor(gridColor);
        g2d.setStroke(new BasicStroke(1.0f));
        int gridSize = 40;
        for (int x = 0; x < width; x += gridSize) {
            g2d.drawLine(x, 0, x, height);
        }
        for (int y = 0; y < height; y += gridSize) {
            g2d.drawLine(0, y, width, y);
        }

        // Distinct Graphic Motif based on evidence type
        int centerX = width / 2;
        int centerY = height / 2;

        if ("CCTV".equals(typeUpper) || "VIDEO".equals(typeUpper)) {
            // CCTV Reticle and scanlines
            g2d.setColor(new Color(16, 185, 129, 80));
            g2d.setStroke(new BasicStroke(2.0f));
            int r = 160;
            g2d.drawOval(centerX - r, centerY - r, r * 2, r * 2);
            g2d.drawLine(centerX - r - 20, centerY, centerX + r + 20, centerY);
            g2d.drawLine(centerX, centerY - r - 20, centerX, centerY + r + 20);
            g2d.drawRect(centerX - r / 2, centerY - r / 2, r, r);
            // Timecode stamp in green
            g2d.setFont(new Font("Monospaced", Font.BOLD, 16));
            g2d.drawString("[CAM-01] ● REC  02:41:19 UTC", 60, height - 70);
        } else if ("AUDIO".equals(typeUpper)) {
            // Audio wave bars
            g2d.setColor(new Color(6, 182, 212, 120));
            g2d.setStroke(new BasicStroke(3.0f));
            int bars = 32;
            int barWidth = (width - 160) / bars;
            for (int i = 0; i < bars; i++) {
                int barH = (int) (Math.sin(i * 0.4) * 80 + Math.cos(i * 0.8) * 40 + 90);
                g2d.fillRect(80 + (i * barWidth), centerY - barH / 2, barWidth - 4, barH);
            }
        } else if ("DIGITAL_DEVICE".equals(typeUpper) || "CALL_RECORD".equals(typeUpper)) {
            // Circuit & binary matrix
            g2d.setColor(new Color(168, 85, 247, 80));
            g2d.setStroke(new BasicStroke(1.5f));
            g2d.drawRect(centerX - 180, centerY - 120, 360, 240);
            g2d.drawRect(centerX - 160, centerY - 100, 320, 200);
            g2d.setFont(new Font("Monospaced", Font.PLAIN, 12));
            g2d.drawString("0x43 0x53 0x2D 0x41 0x49 // BITSTREAM INTEGRITY OK", 60, height - 70);
        } else if ("DOCUMENT".equals(typeUpper) || "FORENSIC_REPORT".equals(typeUpper)) {
            // Document paper watermark frame
            g2d.setColor(new Color(245, 158, 11, 70));
            g2d.setStroke(new BasicStroke(2.0f));
            g2d.drawRoundRect(centerX - 200, centerY - 130, 400, 260, 8, 8);
            for (int ly = centerY - 90; ly <= centerY + 90; ly += 24) {
                g2d.drawLine(centerX - 160, ly, centerX + 160, ly);
            }
        } else {
            // Radar circles
            g2d.setColor(new Color(239, 68, 68, 50));
            g2d.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{8, 8}, 0));
            int radius = Math.min(width, height) / 3;
            g2d.drawOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
            g2d.drawOval(centerX - radius / 2, centerY - radius / 2, radius, radius);
        }

        // Outer Border
        g2d.setColor(accentColor);
        g2d.setStroke(new BasicStroke(3.0f));
        g2d.drawRect(8, 8, width - 16, height - 16);

        // TOP WARNING BANNER (MANDATORY SAFETY DISCLAIMER)
        g2d.setColor(new Color(220, 38, 38, 235));
        g2d.fillRect(16, 16, width - 32, 46);

        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 16));
        String bannerText = "⚠ AI-GENERATED ILLUSTRATION — NOT REAL EVIDENCE";
        FontMetrics fmBanner = g2d.getFontMetrics();
        int bannerTextX = (width - fmBanner.stringWidth(bannerText)) / 2;
        g2d.drawString(bannerText, bannerTextX, 45);

        // Theme Badge
        g2d.setColor(themeBadgeBg);
        g2d.fillRect(32, 74, width - 64, 28);
        g2d.setColor(accentColor);
        g2d.drawRect(32, 74, width - 64, 28);

        g2d.setColor(themeBadgeText);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        FontMetrics fmTheme = g2d.getFontMetrics();
        int themeX = (width - fmTheme.stringWidth(themeBadgeLabel)) / 2;
        g2d.drawString(themeBadgeLabel, themeX, 93);

        // Center Content Card
        int cardWidth = width - 80;
        int cardHeight = height - 230;
        int cardX = 40;
        int cardY = 114;

        g2d.setColor(cardBg);
        g2d.fillRoundRect(cardX, cardY, cardWidth, cardHeight, 10, 10);
        g2d.setColor(accentColor);
        g2d.setStroke(new BasicStroke(1.5f));
        g2d.drawRoundRect(cardX, cardY, cardWidth, cardHeight, 10, 10);

        // Evidence Title Header
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 18));
        String safeTitle = displayTitle.length() > 60 ? displayTitle.substring(0, 57) + "..." : displayTitle;
        g2d.drawString(safeTitle, cardX + 24, cardY + 38);

        // Subtitle Type Pill
        g2d.setColor(themeBadgeText);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.drawString("TYPE: " + typeUpper + " // SYNTHETIC DEFAULT VISUALIZATION", cardX + 24, cardY + 62);

        // Separator line
        g2d.setColor(new Color(255, 255, 255, 30));
        g2d.drawLine(cardX + 24, cardY + 74, cardX + cardWidth - 24, cardY + 74);

        // Multi-line Prompt Rendering
        g2d.setColor(new Color(225, 235, 245));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 15));
        FontMetrics fmPrompt = g2d.getFontMetrics();
        int lineHeight = fmPrompt.getHeight() + 4;
        int textY = cardY + 104;
        int maxTextWidth = cardWidth - 48;

        String[] words = prompt.split("\\s+");
        StringBuilder currentLine = new StringBuilder();

        for (String word : words) {
            String testLine = currentLine.length() == 0 ? word : currentLine + " " + word;
            if (fmPrompt.stringWidth(testLine) > maxTextWidth) {
                g2d.drawString(currentLine.toString(), cardX + 24, textY);
                textY += lineHeight;
                currentLine = new StringBuilder(word);
                if (textY > cardY + cardHeight - 60) {
                    currentLine.append("...");
                    break;
                }
            } else {
                currentLine = new StringBuilder(testLine);
            }
        }
        if (currentLine.length() > 0 && textY <= cardY + cardHeight - 60) {
            g2d.drawString(currentLine.toString(), cardX + 24, textY);
        }

        // Forensic disclaimer badge at bottom of card
        g2d.setColor(new Color(148, 163, 184));
        g2d.setFont(new Font("SansSerif", Font.ITALIC, 12));
        String legalDisclaimer = "Default investigative illustration. Upload real physical evidence to replace.";
        g2d.drawString(legalDisclaimer, cardX + 24, cardY + cardHeight - 16);

        // FOOTER BAR
        g2d.setColor(new Color(15, 23, 42));
        g2d.fillRect(16, height - 48, width - 32, 32);
        g2d.setColor(new Color(148, 163, 184));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        g2d.drawString("CRIMESPHERE AI FORENSIC SPECIMEN ENGINE • " + OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss 'UTC'")), 32, height - 28);

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpeg", baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode synthetic illustration to JPEG", e);
        }

        return new GeneratedImageResult(
                baos.toByteArray(),
                "image/jpeg",
                modelName + " (Forensic Renderer)",
                prompt
        );
    }

    /**
     * Builds safe fictional portrait prompt for a suspect dossier.
     */
    public String buildSuspectPortraitPrompt(String name, String alias, Integer age, String gender, String caseContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a fictional cinematic law enforcement dossier portrait illustration for a case simulation.\n");
        sb.append("Subject role: Fictional Suspect / Person of Interest\n");
        if (StringUtils.hasText(name)) sb.append("Fictional name: ").append(name.trim()).append("\n");
        if (StringUtils.hasText(alias)) sb.append("Alias descriptor: ").append(alias.trim()).append("\n");
        if (age != null && age > 0) sb.append("Approximate age category: ").append(age).append(" years\n");
        if (StringUtils.hasText(gender)) sb.append("Gender presentation: ").append(gender.trim()).append("\n");
        if (StringUtils.hasText(caseContext)) sb.append("Case context: ").append(caseContext.trim()).append("\n");
        sb.append("\nSTYLE & SAFETY INSTRUCTIONS:\n");
        sb.append("- Fictional stylized concept portrait with dossier watermark lighting.\n");
        sb.append("- STRICT SAFETY: Do NOT depict any real living or deceased person or public figure.\n");
        sb.append("- Do NOT claim this image represents the actual suspect.\n");
        sb.append("- Fictional police file simulation visual placeholder only.");
        return sb.toString();
    }

    /**
     * Builds safe fictional portrait prompt for a victim dossier.
     */
    public String buildVictimPortraitPrompt(String name, Integer age, String gender, String relationship, String caseContext) {
        StringBuilder sb = new StringBuilder();
        sb.append("Create a fictional cinematic law enforcement dossier portrait illustration for a case simulation.\n");
        sb.append("Subject role: Fictional Victim Profile\n");
        if (StringUtils.hasText(name)) sb.append("Fictional name: ").append(name.trim()).append("\n");
        if (age != null && age > 0) sb.append("Approximate age category: ").append(age).append(" years\n");
        if (StringUtils.hasText(gender)) sb.append("Gender presentation: ").append(gender.trim()).append("\n");
        if (StringUtils.hasText(relationship)) sb.append("Case relationship: ").append(relationship.trim()).append("\n");
        if (StringUtils.hasText(caseContext)) sb.append("Case context: ").append(caseContext.trim()).append("\n");
        sb.append("\nSTYLE & SAFETY INSTRUCTIONS:\n");
        sb.append("- Fictional stylized concept portrait with dossier watermark lighting.\n");
        sb.append("- STRICT SAFETY: Do NOT depict any real living or deceased person or public figure.\n");
        sb.append("- Do NOT claim this image represents the actual victim.\n");
        sb.append("- Fictional police file simulation visual placeholder only.");
        return sb.toString();
    }

    /**
     * Generates a synthetic dummy portrait for a suspect or victim.
     */
    public GeneratedImageResult generateFictionalDossierPortrait(
            String role,
            String name,
            String aliasOrRel,
            Integer age,
            String gender,
            String caseContext,
            String seedId
    ) {
        String prompt = "SUSPECT".equalsIgnoreCase(role)
                ? buildSuspectPortraitPrompt(name, aliasOrRel, age, gender, caseContext)
                : buildVictimPortraitPrompt(name, age, gender, aliasOrRel, caseContext);

        if (!mockEnabled && StringUtils.hasText(geminiApiKey)) {
            try {
                return callGeminiImagenApi(prompt, "1:1");
            } catch (Exception e) {
                logger.warn("Live Gemini Image API call for fictional portrait failed: {}. Using procedural dossier portrait.", e.getMessage());
            }
        }

        return generateFallbackDossierPortrait(role, name, aliasOrRel, age, gender, caseContext, seedId);
    }

    /**
     * Generates a high-quality, procedural vector dossier portrait customized to the person's record.
     * Guaranteed to produce distinct, visually striking portraits for different records.
     */
    public GeneratedImageResult generateFallbackDossierPortrait(
            String role,
            String name,
            String aliasOrRel,
            Integer age,
            String gender,
            String caseContext,
            String seedId
    ) {
        int width = 600;
        int height = 600;

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = image.createGraphics();

        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        // Derive deterministic variations from seed
        String seedKey = (seedId != null ? seedId : "") + "|" + (name != null ? name : "") + "|" + (gender != null ? gender : "") + "|" + (age != null ? age : 0);
        int hash = Math.abs(seedKey.hashCode());

        boolean isSuspect = "SUSPECT".equalsIgnoreCase(role);

        // Color palettes based on role and hash
        Color bgTop, bgBottom, accentColor, glowColor, silhouetteColor, highlightColor;
        int paletteChoice = hash % 4;

        if (isSuspect) {
            if (paletteChoice == 0) {
                bgTop = new Color(24, 12, 14);
                bgBottom = new Color(10, 6, 8);
                accentColor = new Color(239, 68, 68);
                glowColor = new Color(220, 38, 38, 50);
                silhouetteColor = new Color(55, 25, 30);
                highlightColor = new Color(252, 165, 165);
            } else if (paletteChoice == 1) {
                bgTop = new Color(26, 16, 10);
                bgBottom = new Color(12, 8, 6);
                accentColor = new Color(245, 158, 11);
                glowColor = new Color(217, 119, 6, 50);
                silhouetteColor = new Color(60, 35, 20);
                highlightColor = new Color(253, 230, 138);
            } else if (paletteChoice == 2) {
                bgTop = new Color(20, 14, 28);
                bgBottom = new Color(10, 6, 16);
                accentColor = new Color(168, 85, 247);
                glowColor = new Color(147, 51, 234, 50);
                silhouetteColor = new Color(45, 25, 65);
                highlightColor = new Color(216, 180, 254);
            } else {
                bgTop = new Color(28, 18, 18);
                bgBottom = new Color(14, 8, 8);
                accentColor = new Color(244, 63, 94);
                glowColor = new Color(225, 29, 72, 50);
                silhouetteColor = new Color(65, 30, 35);
                highlightColor = new Color(254, 205, 211);
            }
        } else {
            // Victim palettes
            if (paletteChoice == 0) {
                bgTop = new Color(10, 20, 30);
                bgBottom = new Color(5, 10, 18);
                accentColor = new Color(14, 165, 233);
                glowColor = new Color(2, 132, 199, 50);
                silhouetteColor = new Color(20, 45, 65);
                highlightColor = new Color(186, 230, 253);
            } else if (paletteChoice == 1) {
                bgTop = new Color(10, 28, 24);
                bgBottom = new Color(5, 14, 12);
                accentColor = new Color(20, 184, 166);
                glowColor = new Color(13, 148, 136, 50);
                silhouetteColor = new Color(20, 55, 50);
                highlightColor = new Color(153, 246, 228);
            } else if (paletteChoice == 2) {
                bgTop = new Color(14, 18, 36);
                bgBottom = new Color(7, 10, 20);
                accentColor = new Color(99, 102, 241);
                glowColor = new Color(79, 70, 229, 50);
                silhouetteColor = new Color(30, 40, 75);
                highlightColor = new Color(199, 210, 254);
            } else {
                bgTop = new Color(12, 25, 32);
                bgBottom = new Color(6, 12, 18);
                accentColor = new Color(56, 189, 248);
                glowColor = new Color(14, 165, 233, 50);
                silhouetteColor = new Color(25, 50, 70);
                highlightColor = new Color(224, 242, 254);
            }
        }

        // 1. Background Gradient
        GradientPaint bgPaint = new GradientPaint(0, 0, bgTop, 0, height, bgBottom);
        g2d.setPaint(bgPaint);
        g2d.fillRect(0, 0, width, height);

        // 2. Subtle Grid Lines
        g2d.setColor(new Color(255, 255, 255, 8));
        g2d.setStroke(new BasicStroke(1.0f));
        for (int x = 20; x < width; x += 40) {
            g2d.drawLine(x, 0, x, height);
        }
        for (int y = 20; y < height; y += 40) {
            g2d.drawLine(0, y, width, y);
        }

        // 3. Ambient Radial Glow behind Avatar
        int glowSize = 340;
        int glowX = (width - glowSize) / 2;
        int glowY = 130;
        g2d.setColor(glowColor);
        g2d.fillOval(glowX, glowY, glowSize, glowSize);

        // 4. Large Fictional Initials Watermark
        String initials = "?";
        if (StringUtils.hasText(name)) {
            String[] parts = name.trim().split("\\s+");
            if (parts.length >= 2) {
                initials = (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1)).toUpperCase();
            } else if (!parts[0].isEmpty()) {
                initials = parts[0].substring(0, 1).toUpperCase();
            }
        }
        g2d.setColor(new Color(255, 255, 255, 12));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 180));
        FontMetrics fmInit = g2d.getFontMetrics();
        int initW = fmInit.stringWidth(initials);
        g2d.drawString(initials, (width - initW) / 2, 330);

        // 5. Procedural Silhouette Portrait
        int centerX = width / 2;
        int shoulderY = 410;
        int headCenterY = 240;
        int headRadius = 70 + (hash % 10);

        // Shoulders / Torso
        g2d.setColor(silhouetteColor);
        g2d.fillRoundRect(centerX - 130, shoulderY - 70, 260, 140, 90, 90);

        // Collar / Neck
        g2d.setColor(silhouetteColor.brighter());
        g2d.fillRect(centerX - 35, headCenterY + 45, 70, 55);

        // Head Oval
        g2d.setColor(silhouetteColor.brighter().brighter());
        g2d.fillOval(centerX - headRadius, headCenterY - headRadius, headRadius * 2, (int)(headRadius * 2.2));

        // Hair styling variation based on gender & hash
        String genUpper = gender != null ? gender.trim().toUpperCase() : "UNSPECIFIED";
        g2d.setColor(silhouetteColor.darker());
        if ("FEMALE".equals(genUpper) || (hash % 2 == 1 && !"MALE".equals(genUpper))) {
            // Longer hair / styling
            g2d.fillOval(centerX - headRadius - 12, headCenterY - headRadius - 15, headRadius * 2 + 24, (int)(headRadius * 1.5));
            g2d.fillRoundRect(centerX - headRadius - 16, headCenterY - 20, 32, 130, 20, 20);
            g2d.fillRoundRect(centerX + headRadius - 16, headCenterY - 20, 32, 130, 20, 20);
        } else {
            // Short parted / cropped hair
            g2d.fillArc(centerX - headRadius - 8, headCenterY - headRadius - 15, headRadius * 2 + 16, headRadius * 2, 0, 180);
        }

        // Rim Lighting / Highlight Stroke
        g2d.setColor(new Color(highlightColor.getRed(), highlightColor.getGreen(), highlightColor.getBlue(), 120));
        g2d.setStroke(new BasicStroke(2.5f));
        g2d.drawOval(centerX - headRadius, headCenterY - headRadius, headRadius * 2, (int)(headRadius * 2.2));
        g2d.drawRoundRect(centerX - 130, shoulderY - 70, 260, 140, 90, 90);

        // 6. Holographic Target Reticle / Brackets
        g2d.setColor(accentColor);
        g2d.setStroke(new BasicStroke(2.0f));
        int bSize = 24;
        int boxPad = 28;
        int boxW = width - (boxPad * 2);
        int boxH = height - 120;

        // Top-left bracket
        g2d.drawLine(boxPad, boxPad + 40, boxPad + bSize, boxPad + 40);
        g2d.drawLine(boxPad, boxPad + 40, boxPad, boxPad + 40 + bSize);

        // Top-right bracket
        g2d.drawLine(boxPad + boxW - bSize, boxPad + 40, boxPad + boxW, boxPad + 40);
        g2d.drawLine(boxPad + boxW, boxPad + 40, boxPad + boxW, boxPad + 40 + bSize);

        // 7. Top Header Banner
        g2d.setColor(new Color(15, 23, 42, 220));
        g2d.fillRect(0, 0, width, 48);
        g2d.setColor(new Color(255, 255, 255, 20));
        g2d.drawLine(0, 48, width, 48);

        g2d.setColor(accentColor);
        g2d.setFont(new Font("SansSerif", Font.BOLD, 12));
        g2d.drawString("CRIMESPHERE AI • FICTIONAL CASE DOSSIER", 24, 28);

        String badgeText = isSuspect ? "SYNTHETIC SUSPECT PROFILE" : "SYNTHETIC VICTIM PROFILE";
        FontMetrics fmBadge = g2d.getFontMetrics();
        int bw = fmBadge.stringWidth(badgeText);
        g2d.setColor(new Color(accentColor.getRed(), accentColor.getGreen(), accentColor.getBlue(), 40));
        g2d.fillRoundRect(width - bw - 40, 14, bw + 20, 22, 10, 10);
        g2d.setColor(accentColor);
        g2d.drawString(badgeText, width - bw - 30, 29);

        // 8. Bottom Safety Banner (CRITICAL REQUIREMENT)
        int bannerH = 72;
        int bannerY = height - bannerH;

        g2d.setColor(new Color(10, 12, 18, 245));
        g2d.fillRect(0, bannerY, width, bannerH);
        g2d.setColor(accentColor);
        g2d.drawLine(0, bannerY, width, bannerY);

        g2d.setColor(new Color(254, 240, 138));
        g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
        FontMetrics fmSafety = g2d.getFontMetrics();
        String line1 = "SYNTHETIC DEMO IMAGE • NOT A REAL PERSON IDENTIFICATION";
        int s1W = fmSafety.stringWidth(line1);
        g2d.drawString(line1, (width - s1W) / 2, bannerY + 28);

        g2d.setColor(new Color(148, 163, 184));
        g2d.setFont(new Font("SansSerif", Font.PLAIN, 11));
        FontMetrics fmSub = g2d.getFontMetrics();
        String line2 = "Fictional portrait for simulation • Upload official case photograph to replace.";
        int s2W = fmSub.stringWidth(line2);
        g2d.drawString(line2, (width - s2W) / 2, bannerY + 50);

        g2d.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpeg", baos);
        } catch (IOException e) {
            throw new RuntimeException("Failed to encode synthetic dossier portrait to JPEG", e);
        }

        return new GeneratedImageResult(
                baos.toByteArray(),
                "image/jpeg",
                modelName + " (Procedural Dossier Portrait)",
                line1
        );
    }
}
