package com.connecthub.auth.dto;

import com.connecthub.user.dto.MeResponse;

public record AuthResponse(String accessToken, long expiresIn, MeResponse user) {
}
