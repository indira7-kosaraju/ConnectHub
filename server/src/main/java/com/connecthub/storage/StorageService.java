package com.connecthub.storage;

import org.springframework.web.multipart.MultipartFile;

public interface StorageService {

    /** Validates, resizes and stores an image. */
    StoredImage store(MultipartFile file, ImagePreset preset);

    /** Convenience for callers that only need the URL. */
    default String uploadImage(MultipartFile file, ImagePreset preset) {
        return store(file, preset).url();
    }
}
