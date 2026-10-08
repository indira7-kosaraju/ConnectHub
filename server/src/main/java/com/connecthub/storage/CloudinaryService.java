package com.connecthub.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.connecthub.common.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class CloudinaryService implements StorageService {

    private final Cloudinary cloudinary;
    private final ImageProcessor processor;

    @Override
    public StoredImage store(MultipartFile file, ImagePreset preset) {
        ProcessedImage image = processor.process(file, preset);
        try {
            Map<?, ?> result = cloudinary.uploader().upload(image.bytes(), ObjectUtils.asMap(
                    "folder", "connecthub/" + preset.folder,
                    "resource_type", "image",
                    "overwrite", false));
            return new StoredImage((String) result.get("secure_url"), image.width(), image.height());
        } catch (IOException | RuntimeException e) {
            log.error("Cloudinary upload failed", e);
            throw new StorageUnavailableException();
        }
    }

    static class StorageUnavailableException extends ApiException {
        StorageUnavailableException() {
            super(HttpStatus.SERVICE_UNAVAILABLE, "Image upload is temporarily unavailable");
        }
    }
}
