package com.connecthub.storage;

import com.cloudinary.Cloudinary;
import com.connecthub.config.AppProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.time.Duration;

@Configuration
public class StorageConfig {

    @Bean
    public StorageService storageService(AppProperties props, Cloudinary cloudinary, ImageProcessor processor) {
        if (props.cloudinary().isConfigured()) {
            return new CloudinaryService(cloudinary, processor);
        }
        return new LocalStorageService(Path.of(props.storage().localDir()), processor);
    }

    @Bean
    public WebMvcConfigurer localUploadsResourceHandler(StorageService storage) {
        return new WebMvcConfigurer() {
            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                if (storage instanceof LocalStorageService local) {
                    registry.addResourceHandler(LocalStorageService.URL_PREFIX + "**")
                            .addResourceLocations(local.root().toUri().toString())
                            .setCacheControl(CacheControl.maxAge(Duration.ofDays(30)).cachePublic().immutable());
                }
            }
        };
    }
}
