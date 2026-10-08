package com.connecthub.storage;

/** JPEG bytes produced by {@link ImageProcessor}, with final dimensions. */
public record ProcessedImage(byte[] bytes, int width, int height) {
}
