package com.connecthub.moment.dto;

import com.connecthub.moment.MomentBackground;
import com.connecthub.moment.MomentKind;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.UUID;

public record MomentDto(
        UUID id,
        MomentKind kind,
        @Schema(nullable = true) String imageUrl,
        @Schema(nullable = true) Integer imageWidth,
        @Schema(nullable = true) Integer imageHeight,
        @Schema(nullable = true) String text,
        @Schema(nullable = true) MomentBackground background,
        Instant createdAt,
        Instant expiresAt,
        @Schema(description = "Whether the viewer has seen it (always true for your own)") boolean seen,
        @Schema(nullable = true, description = "Only present for the author") Integer viewCount) {
}
