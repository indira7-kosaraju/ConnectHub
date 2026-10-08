package com.connecthub.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Stores processed images on local disk and serves them from {@code /uploads/**}. Used when
 * Cloudinary isn't configured so dev and CI work offline. Not for production (ephemeral disk).
 */
@Slf4j
public class LocalStorageService implements StorageService {

    public static final String URL_PREFIX = "/uploads/";

    private final Path root;
    private final ImageProcessor processor;

    public LocalStorageService(Path root, ImageProcessor processor) {
        this.root = root.toAbsolutePath().normalize();
        this.processor = processor;
        try {
            Files.createDirectories(this.root);
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot create upload dir " + this.root, e);
        }
        log.warn("Cloudinary is not configured — storing images on local disk at {}", this.root);
    }

    public Path root() {
        return root;
    }

    @Override
    public StoredImage store(MultipartFile file, ImagePreset preset) {
        ProcessedImage image = processor.process(file, preset);
        String name = preset.folder + "-" + UUID.randomUUID() + ".jpg";
        try {
            Files.write(root.resolve(name), image.bytes());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return new StoredImage(URL_PREFIX + name, image.width(), image.height());
    }
}
