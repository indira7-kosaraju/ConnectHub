package com.connecthub.user.dto;

import jakarta.validation.constraints.Size;

/** Password-protected accounts confirm with their password; Google-only accounts type DELETE. */
public record DeleteAccountRequest(@Size(max = 72) String password, @Size(max = 10) String confirm) {
}
