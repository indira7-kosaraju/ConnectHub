package com.connecthub.auth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpHeaders;

/** Request metadata helpers. Remote address honours X-Forwarded-For via server.forward-headers-strategy. */
public final class ClientInfo {

    private ClientInfo() {
    }

    public static String ip(HttpServletRequest request) {
        return request.getRemoteAddr();
    }

    public static String userAgent(HttpServletRequest request) {
        return request.getHeader(HttpHeaders.USER_AGENT);
    }
}
