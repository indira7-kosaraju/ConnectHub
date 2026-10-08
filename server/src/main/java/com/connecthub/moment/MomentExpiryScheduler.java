package com.connecthub.moment;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Every 10 minutes, moves expired moments into their authors' Rewind ({@code archived = true}).
 * Reads already filter on {@code expires_at}, so an expired moment disappears from the strip on
 * time even between runs; this job makes the archive state durable and queryable.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MomentExpiryScheduler {

    private final MomentService moments;

    @Scheduled(fixedDelayString = "${app.moments.archive-interval:PT10M}", initialDelayString = "PT30S")
    public void archiveExpiredMoments() {
        int archived = moments.archiveExpired();
        if (archived > 0) {
            log.info("Archived {} expired moments into Rewind", archived);
        }
    }
}
