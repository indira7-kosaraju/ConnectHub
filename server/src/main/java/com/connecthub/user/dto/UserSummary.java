package com.connecthub.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/** Minimal public view of a user, embedded in other resources. */
public record UserSummary(
        UUID id,
        String username,
        @Schema(nullable = true) String displayName,
        @Schema(nullable = true) String avatarUrl) {
}
