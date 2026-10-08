package com.connecthub.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.concurrent.ThreadLocalRandom;

public final class Slugs {

    private static final int MAX = 50;

    private Slugs() {
    }

    /** "Film Photography & Darkroom!" → "film-photography-darkroom". */
    public static String slugify(String input) {
        String s = Normalizer.normalize(input == null ? "" : input, Normalizer.Form.NFKD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (s.length() > MAX) {
            s = s.substring(0, MAX).replaceAll("-+$", "");
        }
        return s.isEmpty() ? "circle" : s;
    }

    /** First free slug among base, base-2 … base-20, then base-xxxxx. */
    public static String unique(String base, Predicate<String> taken) {
        if (!taken.test(base)) {
            return base;
        }
        for (int i = 2; i <= 20; i++) {
            String candidate = base + "-" + i;
            if (!taken.test(candidate)) {
                return candidate;
            }
        }
        return base + "-" + Integer.toString(ThreadLocalRandom.current().nextInt(36 * 36 * 36, 36 * 36 * 36 * 36), 36);
    }
}
