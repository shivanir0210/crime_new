package com.crimesphere.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.io.File;
import java.nio.file.Files;
import java.util.List;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class CrimeSphereBackendApplication {

    private static final Logger logger = LoggerFactory.getLogger(CrimeSphereBackendApplication.class);

    public static void main(String[] args) {
        loadDotEnv();
        SpringApplication.run(CrimeSphereBackendApplication.class, args);
    }

    private static void loadDotEnv() {
        String userDir = System.getProperty("user.dir", ".");
        File[] candidateFiles = new File[]{
            new File("D:/crime _sphere_new/.env"),
            new File("D:\\crime _sphere_new\\.env"),
            new File(userDir, "../.env"),
            new File(userDir, ".env"),
            new File("../.env"),
            new File(".env")
        };

        String loadedPath = null;

        for (File envFile : candidateFiles) {
            try {
                if (envFile.exists() && envFile.isFile()) {
                    File canonical = envFile.getCanonicalFile();
                    if (canonical.getAbsolutePath().toLowerCase().contains("ai-service")) {
                        continue;
                    }
                    List<String> lines = Files.readAllLines(canonical.toPath());
                    loadedPath = canonical.getAbsolutePath();

                    for (String line : lines) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                            continue;
                        }
                        int eqIdx = trimmed.indexOf('=');
                        if (eqIdx > 0) {
                            String key = trimmed.substring(0, eqIdx).trim();
                            String value = trimmed.substring(eqIdx + 1).trim();
                            if ((value.startsWith("\"") && value.endsWith("\"")) ||
                                (value.startsWith("'") && value.endsWith("'"))) {
                                value = value.substring(1, value.length() - 1);
                            }

                            if (!isPlaceholderValue(value)) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                    break;
                }
            } catch (Exception ignored) {
            }
        }

        String resolvedProvider = System.getProperty("PUBLIC_SEARCH_PROVIDER", System.getenv().getOrDefault("PUBLIC_SEARCH_PROVIDER", "newsapi"));
        String mockEnabledStr = System.getProperty("PUBLIC_SEARCH_MOCK_ENABLED", System.getenv().getOrDefault("PUBLIC_SEARCH_MOCK_ENABLED", "false"));
        String apiKey = System.getProperty("NEWS_API_KEY", System.getenv("NEWS_API_KEY"));
        boolean apiKeyPlaceholder = isPlaceholderValue(apiKey);
        boolean apiKeyPresent = apiKey != null && !apiKey.isBlank() && !apiKeyPlaceholder;

        logger.info("[DotEnv Loader] Loaded env file path: {}", (loadedPath != null ? loadedPath : "None"));
        logger.info("NEWS_API_KEY present: {}", apiKeyPresent);
        logger.info("NEWS_API_KEY placeholder: {}", apiKeyPlaceholder);
        logger.info("PUBLIC_SEARCH_MOCK_ENABLED: {}", mockEnabledStr);
        logger.info("PUBLIC_SEARCH_PROVIDER: {}", resolvedProvider);
    }

    private static boolean isPlaceholderValue(String val) {
        if (val == null) return true;
        String trimmed = val.trim().toLowerCase();
        return trimmed.isEmpty() ||
               trimmed.contains("your_real_newsapi_key_here") ||
               trimmed.contains("your_news_api_key") ||
               trimmed.contains("replace-with-your-key") ||
               trimmed.contains("replace_with_real_newsapi_key") ||
               trimmed.contains("your_real_groq_api_key_here") ||
               trimmed.contains("your_groq_api_key") ||
               trimmed.contains("your_resend_api_key") ||
               trimmed.contains("your_real_resend_api_key") ||
               trimmed.contains("change_me") ||
               trimmed.equals("xxx") ||
               trimmed.equals("placeholder");
    }
}

