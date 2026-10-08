package com.connecthub.auth;

import com.connecthub.config.AppProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;

/** Builds and reads the httpOnly refresh-token cookie, scoped to the auth endpoints only. */
@Component
@RequiredArgsConstructor
public class RefreshCookies {

    public static final String NAME = "ch_rt";
    static final String PATH = "/api/v1/auth";

    private final AppProperties props;

    public ResponseCookie create(String rawToken, Duration ttl) {
        return base(rawToken).maxAge(ttl).build();
    }

    public ResponseCookie clear() {
        return base("").maxAge(0).build();
    }

    public String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        return Arrays.stream(cookies).filter(c -> NAME.equals(c.getName())).map(Cookie::getValue).findFirst().orElse(null);
    }

    private ResponseCookie.ResponseCookieBuilder base(String value) {
        return ResponseCookie.from(NAME, value)
                .httpOnly(true)
                .secure(props.jwt().cookieSecure())
                .sameSite(props.jwt().cookieSameSite())
                .path(PATH);
    }
}
