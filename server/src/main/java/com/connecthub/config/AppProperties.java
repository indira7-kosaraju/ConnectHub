package com.connecthub.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly-typed view of the {@code app.*} configuration block.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(Jwt jwt, Cors cors, Cloudinary cloudinary, Storage storage, OAuth oauth, String frontendUrl) {

    public record Jwt(String secret, long accessTtlMinutes, long refreshTtlDays, boolean cookieSecure, String cookieSameSite) {
    }

    public record Cors(List<String> allowedOrigins) {
    }

    public record Cloudinary(String cloudName, String apiKey, String apiSecret) {

        public boolean isConfigured() {
            return notBlank(cloudName) && notBlank(apiKey) && notBlank(apiSecret);
        }
    }

    /** Local-disk fallback used when Cloudinary isn't configured (dev, CI). */
    public record Storage(String localDir) {
    }

    public record OAuth(Google google) {
    }

    public record Google(String clientId, String clientSecret, String redirectUri) {

        public boolean isConfigured() {
            return notBlank(clientId) && notBlank(clientSecret);
        }
    }

    private static boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }
}
