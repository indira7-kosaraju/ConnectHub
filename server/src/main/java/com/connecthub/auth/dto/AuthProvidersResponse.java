package com.connecthub.auth.dto;

/** Which sign-in methods this deployment supports; the client hides unavailable ones. */
public record AuthProvidersResponse(boolean password, boolean google) {
}
