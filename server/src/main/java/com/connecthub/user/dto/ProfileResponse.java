package com.connecthub.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

/**
 * Public profile. When {@code restricted} is true (private profile, viewer shares no circle with
 * the owner) location, website and cover are withheld and posts/circles are not readable.
 */
public record ProfileResponse(
        UUID id,
        String username,
        @Schema(nullable = true) String displayName,
        @Schema(nullable = true) String bio,
        @Schema(nullable = true) String avatarUrl,
        @Schema(nullable = true) String coverUrl,
        @Schema(nullable = true) String location,
        @Schema(nullable = true) String website,
        Instant joinedAt,
        boolean privateProfile,
        boolean me,
        boolean restricted,
        @Schema(description = "Viewer shares a circle with this person and can message them") boolean canMessage) {
}
