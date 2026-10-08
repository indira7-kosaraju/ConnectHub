package com.connecthub.auth;

import com.connecthub.config.AppProperties;
import com.connecthub.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

/** Issues and verifies short-lived HS256 access tokens. */
@Service
public class JwtService {

    private static final String ISSUER = "connecthub";

    private final SecretKey key;
    private final Duration accessTtl;

    public JwtService(AppProperties props) {
        String configured = props.jwt().secret();
        if (configured == null || configured.isBlank() || configured.startsWith("${")) {
            throw new IllegalStateException("JWT_SECRET is not set. Generate one with: openssl rand -base64 48");
        }
        byte[] secret;
        try {
            secret = Decoders.BASE64.decode(configured);
        } catch (io.jsonwebtoken.io.DecodingException e) {
            throw new IllegalStateException("JWT_SECRET must be base64. Generate one with: openssl rand -base64 48");
        }
        if (secret.length < 32) {
            throw new IllegalStateException("JWT_SECRET must be base64 of at least 32 bytes (256 bits)");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.accessTtl = Duration.ofMinutes(props.jwt().accessTtlMinutes());
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .issuer(ISSUER)
                .subject(user.getId().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtl)))
                .signWith(key)
                .compact();
    }

    /** Returns the user id for a valid, unexpired token; empty otherwise. */
    public Optional<UUID> verify(String token) {
        try {
            Claims claims = Jwts.parser().verifyWith(key).requireIssuer(ISSUER).build()
                    .parseSignedClaims(token).getPayload();
            return Optional.of(UUID.fromString(claims.getSubject()));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long accessTtlSeconds() {
        return accessTtl.toSeconds();
    }
}
