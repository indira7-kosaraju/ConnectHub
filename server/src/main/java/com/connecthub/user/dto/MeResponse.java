package com.connecthub.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import com.connecthub.user.AuthProvider;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The signed-in user's own account view (includes private fields like email). */
public record MeResponse(
        UUID id,
        String email,
        @Schema(nullable = true) String username,
        @Schema(nullable = true) String displayName,
        @Schema(nullable = true) String bio,
        @Schema(nullable = true) String avatarUrl,
        @Schema(nullable = true) String coverUrl,
        @Schema(nullable = true) String location,
        @Schema(nullable = true) String website,
        boolean privateProfile,
        boolean onboarded,
        boolean hasPassword,
        AuthProvider provider,
        List<InterestDto> interests,
        Map<String, Boolean> notificationPrefs) {
}
