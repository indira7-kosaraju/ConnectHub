package com.connecthub.storage;

/** A stored, processed image. Dimensions let clients reserve space and avoid layout shift. */
public record StoredImage(String url, int width, int height) {
}
