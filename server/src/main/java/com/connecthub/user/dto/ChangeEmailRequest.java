package com.connecthub.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeEmailRequest(@NotBlank @Email @Size(max = 254) String email, @NotBlank @Size(max = 72) String currentPassword) {
}
