package com.connecthub.common;

import com.connecthub.common.exception.BadRequestException;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Keyset pagination cursor of the form {@code <createdAt ISO-8601>,<uuid>}.
 * Items are ordered by (created_at DESC, id DESC); the cursor points at the last item returned.
 */
public record PageCursor(Instant createdAt, UUID id) {

    public static final int DEFAULT_LIMIT = 20;
    public static final int MAX_LIMIT = 50;

    /** Sentinels for the first page of descending / ascending keyset scans. */
    public static final PageCursor NEWEST = new PageCursor(Instant.parse("9999-12-31T00:00:00Z"),
            UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff"));
    public static final PageCursor OLDEST = new PageCursor(Instant.parse("1970-01-01T00:00:00Z"),
            UUID.fromString("00000000-0000-0000-0000-000000000000"));

    public static PageCursor parseOr(String raw, PageCursor firstPage) {
        PageCursor c = parse(raw);
        return c == null ? firstPage : c;
    }

    public static PageCursor parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int comma = raw.lastIndexOf(',');
        if (comma <= 0) {
            throw new BadRequestException("Invalid cursor");
        }
        try {
            return new PageCursor(Instant.parse(raw.substring(0, comma)), UUID.fromString(raw.substring(comma + 1)));
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new BadRequestException("Invalid cursor");
        }
    }

    public static int clampLimit(Integer limit) {
        if (limit == null || limit <= 0) {
            return DEFAULT_LIMIT;
        }
        return Math.min(limit, MAX_LIMIT);
    }

    public String encode() {
        return createdAt.toString() + "," + id;
    }
}
