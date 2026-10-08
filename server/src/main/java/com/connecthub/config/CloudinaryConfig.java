package com.connecthub.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class CloudinaryConfig {

    /**
     * Always registered so the app boots without credentials (local dev, CI). Uploads fail fast
     * with a clear error in {@code CloudinaryService} when {@code app.cloudinary} is not configured.
     */
    @Bean
    public Cloudinary cloudinary(AppProperties props) {
        AppProperties.Cloudinary c = props.cloudinary();
        return new Cloudinary(Map.of(
                "cloud_name", nullToEmpty(c.cloudName()),
                "api_key", nullToEmpty(c.apiKey()),
                "api_secret", nullToEmpty(c.apiSecret()),
                "secure", true));
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
