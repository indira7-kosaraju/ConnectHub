package com.connecthub.auth;

import com.connecthub.common.exception.UnauthorizedException;
import com.connecthub.config.AppProperties;
import com.connecthub.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Opaque refresh tokens with rotation and reuse detection.
 * <ul>
 *   <li>Each refresh revokes the presented token and issues a new one ({@code replaced_by} links them).</li>
 *   <li>Presenting an already-rotated token outside a short grace window is treated as theft:
 *       every session for that user is revoked.</li>
 *   <li>The grace window tolerates two tabs refreshing at the same moment.</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    static final Duration REUSE_GRACE = Duration.ofSeconds(30);
    private static final SecureRandom RANDOM = new SecureRandom();

    private final RefreshTokenRepository repository;
    private final AppProperties props;

    /** Result of a rotation. {@code newRawToken} is null when the grace path was taken (keep the current cookie). */
    public record Rotation(User user, String newRawToken) {
    }

    @Transactional
    public String issue(User user, String userAgent) {
        String raw = generate();
        RefreshToken token = new RefreshToken();
        token.setUser(user);
        token.setTokenHash(hash(raw));
        token.setExpiresAt(Instant.now().plus(ttl()));
        token.setUserAgent(truncate(userAgent));
        repository.save(token);
        return raw;
    }

    @Transactional(noRollbackFor = UnauthorizedException.class)
    public Rotation rotate(String raw, String userAgent) {
        if (raw == null || raw.isBlank()) {
            throw new UnauthorizedException("No session");
        }
        Instant now = Instant.now();
        RefreshToken current = repository.findByTokenHashForUpdate(hash(raw))
                .orElseThrow(() -> new UnauthorizedException("Session expired"));

        if (current.getRevokedAt() != null) {
            boolean concurrentRefresh = current.getReplacedBy() != null
                    && current.getRevokedAt().isAfter(now.minus(REUSE_GRACE));
            if (concurrentRefresh) {
                return new Rotation(current.getUser(), null);
            }
            log.warn("Refresh token reuse detected for user {} — revoking all sessions", current.getUser().getId());
            repository.revokeAllForUser(current.getUser().getId(), now);
            throw new UnauthorizedException("Session expired");
        }
        if (current.isExpired(now)) {
            throw new UnauthorizedException("Session expired");
        }

        String nextRaw = generate();
        RefreshToken next = new RefreshToken();
        next.setUser(current.getUser());
        next.setTokenHash(hash(nextRaw));
        next.setExpiresAt(now.plus(ttl()));
        next.setUserAgent(truncate(userAgent));
        repository.save(next);

        current.setRevokedAt(now);
        current.setReplacedBy(next.getId());
        return new Rotation(current.getUser(), nextRaw);
    }

    @Transactional
    public void revoke(String raw) {
        if (raw == null || raw.isBlank()) {
            return;
        }
        repository.findByTokenHash(hash(raw)).ifPresent(t -> {
            if (t.getRevokedAt() == null) {
                t.setRevokedAt(Instant.now());
            }
        });
    }

    @Transactional
    public void revokeAll(User user) {
        repository.revokeAllForUser(user.getId(), Instant.now());
    }

    /** Daily housekeeping: drop tokens that expired more than a day ago. */
    @Scheduled(cron = "0 17 3 * * *")
    @Transactional
    public void purgeExpired() {
        int n = repository.deleteExpiredBefore(Instant.now().minus(Duration.ofDays(1)));
        if (n > 0) {
            log.info("Purged {} expired refresh tokens", n);
        }
    }

    public Duration ttl() {
        return Duration.ofDays(props.jwt().refreshTtlDays());
    }

    private static String generate() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String hash(String raw) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String truncate(String s) {
        return s == null ? null : s.length() > 255 ? s.substring(0, 255) : s;
    }
}
