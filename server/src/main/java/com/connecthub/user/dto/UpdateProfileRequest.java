package com.connecthub.user.dto;

import jakarta.validation.constraints.Size;

/** Partial update: null = unchanged, empty string = clear (except displayName). */
public record UpdateProfileRequest(
        @Size(max = 60) String displayName,
        @Size(max = 160) String bio,
        @Size(max = 80) String location,
        @Size(max = 255) String website) {
}
