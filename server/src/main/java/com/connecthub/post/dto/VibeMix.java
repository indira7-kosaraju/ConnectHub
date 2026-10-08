package com.connecthub.post.dto;

import com.connecthub.post.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.Map;

/**
 * A post's vibe as <b>proportions</b> (0–1, summing to 1, or all 0 when nobody has reacted).
 * Raw counts are deliberately never exposed: ConnectHub shows a colour blend, not a number.
 */
@Schema(description = "Share of each reaction type (0–1). All zero when there are no reactions.")
public record VibeMix(double spark, double deep, double funny, double support, double mindblown) {

    public static final VibeMix EMPTY = new VibeMix(0, 0, 0, 0, 0);

    public static VibeMix of(Map<ReactionType, Long> counts) {
        long total = counts.values().stream().mapToLong(Long::longValue).sum();
        if (total == 0) {
            return EMPTY;
        }
        return new VibeMix(share(counts, ReactionType.spark, total), share(counts, ReactionType.deep, total),
                share(counts, ReactionType.funny, total), share(counts, ReactionType.support, total),
                share(counts, ReactionType.mindblown, total));
    }

    private static double share(Map<ReactionType, Long> counts, ReactionType t, long total) {
        return Math.round(counts.getOrDefault(t, 0L) * 1000.0 / total) / 1000.0;
    }
}
