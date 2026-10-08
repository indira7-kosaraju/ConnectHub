package com.connecthub.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 60) String displayName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank
        @Size(min = 8, max = 72, message = "Use 8–72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$", message = "Include at least one letter and one number")
        String password) {
}
