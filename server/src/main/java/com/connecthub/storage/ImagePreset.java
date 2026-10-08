package com.connecthub.storage;

/**
 * Target sizes for uploaded images. {@code crop} images are centre-cropped to the exact box;
 * others are scaled to fit within it, preserving aspect ratio. Never upscaled.
 */
public enum ImagePreset {
    AVATAR("avatars", 400, 400, true),
    COVER("covers", 1500, 500, true),
    CIRCLE_COVER("circles", 1200, 400, true),
    POST("posts", 1600, 1600, false),
    MOMENT("moments", 1080, 1920, false);

    public final String folder;
    public final int width;
    public final int height;
    public final boolean crop;

    ImagePreset(String folder, int width, int height, boolean crop) {
        this.folder = folder;
        this.width = width;
        this.height = height;
        this.crop = crop;
    }
}
