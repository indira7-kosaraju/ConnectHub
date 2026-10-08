package com.connecthub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** {@code currentPassword} is required unless the account has no password yet (Google-only). */
public record ChangePasswordRequest(
        @Size(max = 72) String currentPassword,
        @NotBlank
        @Size(min = 8, max = 72, message = "Use 8–72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Include at least one letter and one number")
        String newPassword) {
}
