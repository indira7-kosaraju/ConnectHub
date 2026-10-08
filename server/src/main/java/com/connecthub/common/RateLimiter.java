package com.connecthub.common;

import com.connecthub.common.exception.RateLimitException;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * In-memory token-bucket rate limiting (Bucket4j). Buckets are evicted after an hour of
 * inactivity. Single-instance by design — see DECISIONS.md for the multi-instance path.
 */
@Component
public class RateLimiter {

    /** Named limits used across the app. */
    public enum Limit {
        AUTH(10, Duration.ofMinutes(5), "Too many attempts — please wait a few minutes"),
        REFRESH(30, Duration.ofMinutes(1), "Too many requests"),
        UPLOAD(20, Duration.ofMinutes(10), "Too many uploads — please slow down"),
        CIRCLE_CREATE(5, Duration.ofHours(1), "You've created a lot of circles recently — try again later"),
        POST(10, Duration.ofMinutes(15), "You're posting a lot — take a breather and try again soon"),
        MOMENT(15, Duration.ofHours(1), "That's a lot of moments — give it a little while"),
        COMMENT(30, Duration.ofMinutes(5), "You're commenting very quickly — slow down a little"),
        MESSAGE(60, Duration.ofMinutes(1), "You're sending messages too quickly");

        final int capacity;
        final Duration period;
        final String message;

        Limit(int capacity, Duration period, String message) {
            this.capacity = capacity;
            this.period = period;
            this.message = message;
        }
    }

    private final Cache<String, Bucket> buckets = Caffeine.newBuilder()
            .expireAfterAccess(Duration.ofHours(1))
            .maximumSize(200_000)
            .build();

    /**
     * Scales every limit (1 in production). Dev and e2e runs raise it, since all test users
     * share one IP and would otherwise trip the brute-force limits.
     */
    private final int multiplier;

    public RateLimiter(@Value("${app.rate-limit.multiplier:1}") int multiplier) {
        this.multiplier = Math.max(1, multiplier);
    }

    /** Consumes one token for {@code key} under {@code limit}, or throws {@link RateLimitException}. */
    public void consume(Limit limit, String key) {
        long capacity = (long) limit.capacity * multiplier;
        Bucket bucket = buckets.get(limit.name() + ':' + key, k -> Bucket.builder()
                .addLimit(Bandwidth.builder().capacity(capacity).refillGreedy(capacity, limit.period).build())
                .build());
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
        if (!probe.isConsumed()) {
            long seconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(probe.getNanosToWaitForRefill()));
            throw new RateLimitException(limit.message, seconds);
        }
    }

    /** Test hook: forget all buckets. */
    public void reset() {
        buckets.invalidateAll();
    }
}
