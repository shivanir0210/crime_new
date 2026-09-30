package com.crimesphere.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.*;

@Service
public class StorageService {

    private static final Logger logger = LoggerFactory.getLogger(StorageService.class);

    /** Maximum evidence file size in bytes. Default: 50 MB. Configurable via evidence.upload.max-size */
    @Value("${evidence.upload.max-size-bytes:#{52428800}}")
    private long maxFileSizeBytes = 52428800L; // 50 MB default

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "pdf", "jpg", "jpeg", "png", "webp",
            "mp4", "mov", "mp3", "wav",
            "doc", "docx", "xls", "xlsx", "txt"
    );

    /** Allowed MIME types — validated alongside file extension to prevent content-type spoofing */
    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "application/pdf",
            "image/jpeg", "image/jpg", "image/png", "image/webp",
            "video/mp4", "video/quicktime",
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
    );

    @Value("${storage.evidence.local-dir:./storage/evidence}")
    private String localDirectory = "./storage/evidence";

    @Value("${supabase.url:}")
    private String supabaseUrl = "";

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceKey = "";

    @Value("${supabase.evidence-bucket:crime-evidence}")
    private String supabaseEvidenceBucket = "crime-evidence";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    /**
     * Validates upload file — checks for empty, size limit, file extension, and MIME type.
     */
    public void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Evidence file must not be empty");
        }

        if (file.getSize() > maxFileSizeBytes) {
            long limitMb = maxFileSizeBytes / (1024 * 1024);
            long actualMb = file.getSize() / (1024 * 1024);
            throw new IllegalArgumentException(
                "Evidence file size exceeds maximum limit of " + limitMb + " MB (Actual: " + actualMb + " MB)"
            );
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename != null && !originalFilename.isBlank()) {
            String extension = getFileExtension(originalFilename).toLowerCase(Locale.ROOT);
            if (!extension.isEmpty() && !ALLOWED_EXTENSIONS.contains(extension)) {
                throw new IllegalArgumentException(
                    "File extension ." + extension + " is not permitted. " +
                    "Allowed types: PDF, JPG, PNG, WEBP, MP4, MOV, MP3, WAV, DOC, DOCX, XLS, XLSX, TXT."
                );
            }
        }

        // MIME type validation — must match allowed types regardless of filename extension
        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank()) {
            String normalizedMime = contentType.split(";")[0].trim().toLowerCase(Locale.ROOT);
            if (!ALLOWED_MIME_TYPES.contains(normalizedMime) && !normalizedMime.equals("application/octet-stream")) {
                throw new IllegalArgumentException(
                    "File MIME type '" + normalizedMime + "' is not permitted. " +
                    "Only forensic evidence file types are accepted (PDF, images, video, audio, documents)."
                );
            }
        }
    }

    /**
     * Calculates cryptographic SHA-256 hash for evidence integrity and duplicate detection.
     */
    public String calculateSha256(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = is.read(buffer)) != -1) {
                digest.update(buffer, 0, read);
            }
            byte[] hashBytes = digest.digest();
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException | IOException e) {
            logger.error("Failed to calculate SHA-256 hash for evidence file: {}", e.getMessage());
            throw new RuntimeException("Failed to calculate cryptographic file hash", e);
        }
    }

    /**
     * Calculates cryptographic SHA-256 hash for byte arrays.
     */
    public String calculateSha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to calculate SHA-256 hash", e);
        }
    }

    /**
     * Stores AI illustration image bytes securely.
     */
    public String storeIllustrationBytes(UUID caseId, UUID illustrationId, byte[] data, String mimeType) {
        String relativePath = "cases/" + caseId + "/illustrations/" + illustrationId + ".jpg";

        if (isSupabaseConfigured()) {
            try {
                uploadToSupabase(relativePath, data, mimeType != null ? mimeType : "image/jpeg");
                logger.info("Uploaded illustration image to Supabase: {}", relativePath);
                return relativePath;
            } catch (Exception e) {
                logger.warn("Supabase illustration upload failed, falling back to local storage: {}", e.getMessage());
            }
        }

        try {
            Path targetPath = Paths.get(localDirectory, relativePath).normalize();
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, data);
            logger.info("Stored illustration image securely on local storage: {}", targetPath);
            return relativePath;
        } catch (IOException e) {
            logger.error("Failed to store illustration locally: {}", e.getMessage());
            throw new RuntimeException("Failed to persist illustration image", e);
        }
    }

    /**
     * Stores real suspect/case photograph securely.
     */
    public String storeSuspectPhoto(UUID caseId, UUID suspectId, MultipartFile file) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "suspect_photo.jpg";
        String ext = getFileExtension(originalFilename);
        if (ext.isEmpty()) ext = "jpg";
        String relativePath = "cases/" + caseId + "/suspects/" + suspectId + "/photo_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

        if (isSupabaseConfigured()) {
            try {
                uploadToSupabase(relativePath, file.getBytes(), file.getContentType());
                logger.info("Uploaded suspect photo to Supabase: {}", relativePath);
                return relativePath;
            } catch (Exception e) {
                logger.warn("Supabase suspect photo upload failed, falling back to local storage: {}", e.getMessage());
            }
        }

        try {
            Path targetPath = Paths.get(localDirectory, relativePath).normalize();
            Files.createDirectories(targetPath.getParent());
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Stored suspect photo securely on local storage: {}", targetPath);
            return relativePath;
        } catch (IOException e) {
            logger.error("Failed to store suspect photo locally: {}", e.getMessage());
            throw new RuntimeException("Failed to persist suspect photo", e);
        }
    }

    /**
     * Stores real victim/case photograph securely.
     */
    public String storeVictimPhoto(UUID caseId, UUID victimId, MultipartFile file) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "victim_photo.jpg";
        String ext = getFileExtension(originalFilename);
        if (ext.isEmpty()) ext = "jpg";
        String relativePath = "cases/" + caseId + "/victims/" + victimId + "/photo_" + UUID.randomUUID().toString().substring(0, 8) + "." + ext;

        if (isSupabaseConfigured()) {
            try {
                uploadToSupabase(relativePath, file.getBytes(), file.getContentType());
                logger.info("Uploaded victim photo to Supabase: {}", relativePath);
                return relativePath;
            } catch (Exception e) {
                logger.warn("Supabase victim photo upload failed, falling back to local storage: {}", e.getMessage());
            }
        }

        try {
            Path targetPath = Paths.get(localDirectory, relativePath).normalize();
            Files.createDirectories(targetPath.getParent());
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Stored victim photo securely on local storage: {}", targetPath);
            return relativePath;
        } catch (IOException e) {
            logger.error("Failed to store victim photo locally: {}", e.getMessage());
            throw new RuntimeException("Failed to persist victim photo", e);
        }
    }

    /**
     * Stores synthetic dummy portrait image bytes securely.
     */
    public String storeSyntheticPortraitBytes(UUID caseId, String role, UUID recordId, byte[] data, String mimeType) {
        String relativePath = "cases/" + caseId + "/" + role.toLowerCase(Locale.ROOT) + "s/" + recordId + "/synthetic_portrait.jpg";

        if (isSupabaseConfigured()) {
            try {
                uploadToSupabase(relativePath, data, mimeType != null ? mimeType : "image/jpeg");
                logger.info("Uploaded synthetic portrait image to Supabase: {}", relativePath);
                return relativePath;
            } catch (Exception e) {
                logger.warn("Supabase synthetic portrait upload failed, falling back to local storage: {}", e.getMessage());
            }
        }

        try {
            Path targetPath = Paths.get(localDirectory, relativePath).normalize();
            Files.createDirectories(targetPath.getParent());
            Files.write(targetPath, data);
            logger.info("Stored synthetic portrait image securely on local storage: {}", targetPath);
            return relativePath;
        } catch (IOException e) {
            logger.error("Failed to store synthetic portrait locally: {}", e.getMessage());
            throw new RuntimeException("Failed to persist synthetic portrait image", e);
        }
    }

    /**
     * Stores evidence file securely with sanitized unique storage path.
     */
    public String storeEvidenceFile(UUID caseId, UUID evidenceId, MultipartFile file) {
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "evidence_file";
        String sanitizedName = sanitizeFilename(originalFilename);
        String relativePath = "cases/" + caseId + "/evidence/" + evidenceId + "/" + UUID.randomUUID().toString().substring(0, 8) + "_" + sanitizedName;

        if (isSupabaseConfigured()) {
            try {
                uploadToSupabase(relativePath, file.getBytes(), file.getContentType());
                logger.info("Uploaded evidence file to Supabase private bucket: {}", relativePath);
                return relativePath;
            } catch (Exception e) {
                logger.warn("Supabase storage upload failed, falling back to secure local storage: {}", e.getMessage());
            }
        }

        // Secure local disk storage
        try {
            Path targetPath = Paths.get(localDirectory, relativePath).normalize();
            Files.createDirectories(targetPath.getParent());
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Stored evidence file securely on local storage: {}", targetPath);
            return relativePath;
        } catch (IOException e) {
            logger.error("Failed to store evidence file locally: {}", e.getMessage());
            throw new RuntimeException("Failed to persist evidence file to secure storage", e);
        }
    }

    /**
     * Stores sensitive police officer ID document securely in a private directory.
     */
    public String storeOfficerIdDocument(UUID officerId, MultipartFile file) {
        validateFile(file);
        String originalFilename = file.getOriginalFilename() != null ? file.getOriginalFilename() : "officer_id_document.pdf";
        String ext = getFileExtension(originalFilename);
        if (ext.isEmpty()) ext = "pdf";
        String sanitizedName = sanitizeFilename(originalFilename);
        String relativePath = "officers/" + officerId + "/id_docs/" + UUID.randomUUID().toString().substring(0, 8) + "_" + sanitizedName;

        if (isSupabaseConfigured()) {
            try {
                uploadToSupabase(relativePath, file.getBytes(), file.getContentType());
                logger.info("Uploaded officer ID document to Supabase private storage: {}", relativePath);
                return relativePath;
            } catch (Exception e) {
                logger.warn("Supabase ID doc upload failed, falling back to local storage: {}", e.getMessage());
            }
        }

        try {
            Path targetPath = Paths.get(localDirectory, relativePath).normalize();
            Files.createDirectories(targetPath.getParent());
            try (InputStream is = file.getInputStream()) {
                Files.copy(is, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
            logger.info("Stored officer ID document securely on local disk: {}", targetPath);
            return relativePath;
        } catch (IOException e) {
            logger.error("Failed to store officer ID document locally: {}", e.getMessage());
            throw new RuntimeException("Failed to persist officer ID document", e);
        }
    }

    /**
     * Loads officer ID document as a Resource.
     */
    public Resource loadOfficerIdDocumentResource(String storagePath) {
        return loadEvidenceAsResource(storagePath);
    }

    /**
     * Loads evidence file as a downloadable Resource.
     */
    public Resource loadEvidenceAsResource(String storagePath) {
        // Try local disk first
        Path localPath = Paths.get(localDirectory, storagePath).normalize();
        if (Files.exists(localPath)) {
            return new FileSystemResource(localPath);
        }

        // If not found locally and Supabase configured, download from Supabase
        if (isSupabaseConfigured()) {
            try {
                byte[] data = downloadFromSupabase(storagePath);
                if (data != null && data.length > 0) {
                    Files.createDirectories(localPath.getParent());
                    Files.write(localPath, data);
                    return new FileSystemResource(localPath);
                }
            } catch (Exception e) {
                logger.error("Failed to retrieve file from Supabase storage: {}", e.getMessage());
            }
        }

        throw new RuntimeException("Evidence file not found at storage location");
    }

    /**
     * Deletes evidence file in case of transaction rollback or cleanup.
     */
    public void deleteEvidenceFile(String storagePath) {
        try {
            Path localPath = Paths.get(localDirectory, storagePath).normalize();
            Files.deleteIfExists(localPath);
        } catch (Exception e) {
            logger.warn("Could not delete local file on rollback: {}", e.getMessage());
        }

        if (isSupabaseConfigured()) {
            try {
                deleteFromSupabase(storagePath);
            } catch (Exception e) {
                logger.warn("Could not delete Supabase object on rollback: {}", e.getMessage());
            }
        }
    }

    private boolean isSupabaseConfigured() {
        return StringUtils.hasText(supabaseUrl) && StringUtils.hasText(supabaseServiceKey);
    }

    private void uploadToSupabase(String storagePath, byte[] data, String contentType) throws Exception {
        String endpoint = supabaseUrl.replaceAll("/$", "") + "/storage/v1/object/" + supabaseEvidenceBucket + "/" + storagePath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("apikey", supabaseServiceKey)
                .header("Content-Type", contentType != null ? contentType : "application/octet-stream")
                .header("x-upsert", "true")
                .POST(HttpRequest.BodyPublishers.ofByteArray(data))
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() >= 300) {
            throw new RuntimeException("Supabase Storage error (status " + response.statusCode() + "): " + response.body());
        }
    }

    private byte[] downloadFromSupabase(String storagePath) throws Exception {
        String endpoint = supabaseUrl.replaceAll("/$", "") + "/storage/v1/object/authenticated/" + supabaseEvidenceBucket + "/" + storagePath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("apikey", supabaseServiceKey)
                .GET()
                .timeout(Duration.ofSeconds(20))
                .build();

        HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() == 200) {
            return response.body();
        }
        throw new RuntimeException("Supabase download returned status: " + response.statusCode());
    }

    private void deleteFromSupabase(String storagePath) throws Exception {
        String endpoint = supabaseUrl.replaceAll("/$", "") + "/storage/v1/object/" + supabaseEvidenceBucket + "/" + storagePath;
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(endpoint))
                .header("Authorization", "Bearer " + supabaseServiceKey)
                .header("apikey", supabaseServiceKey)
                .DELETE()
                .timeout(Duration.ofSeconds(10))
                .build();
        httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String sanitizeFilename(String filename) {
        String clean = Paths.get(filename).getFileName().toString();
        return clean.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String getFileExtension(String filename) {
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx > 0 && dotIdx < filename.length() - 1) {
            return filename.substring(dotIdx + 1);
        }
        return "";
    }
}
