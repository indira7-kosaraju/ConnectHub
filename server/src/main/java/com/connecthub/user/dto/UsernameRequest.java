package com.connecthub.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record UsernameRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]{3,30}$", message = "3–30 letters, numbers or underscores")
        String username) {
}
