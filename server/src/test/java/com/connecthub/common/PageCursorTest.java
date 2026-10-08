package com.connecthub.common;

import com.connecthub.common.exception.BadRequestException;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PageCursorTest {

    @Test
    void roundTrips() {
        PageCursor c = new PageCursor(Instant.parse("2026-01-02T03:04:05.123456Z"), UUID.randomUUID());
        assertThat(PageCursor.parse(c.encode())).isEqualTo(c);
    }

    @Test
    void blankIsNull() {
        assertThat(PageCursor.parse(null)).isNull();
        assertThat(PageCursor.parse(" ")).isNull();
    }

    @Test
    void rejectsGarbage() {
        assertThatThrownBy(() -> PageCursor.parse("nope")).isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> PageCursor.parse("2026-01-01,not-a-uuid")).isInstanceOf(BadRequestException.class);
    }

    @Test
    void clampsLimit() {
        assertThat(PageCursor.clampLimit(null)).isEqualTo(PageCursor.DEFAULT_LIMIT);
        assertThat(PageCursor.clampLimit(500)).isEqualTo(PageCursor.MAX_LIMIT);
        assertThat(PageCursor.clampLimit(5)).isEqualTo(5);
    }
}
