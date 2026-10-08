package com.connecthub.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TextSanitizerTest {

    @Test
    void stripsMarkupButKeepsPlainCharacters() {
        assertThat(TextSanitizer.plain("<script>alert(1)</script>Hi <b>there</b> & 1 < 2"))
                .isEqualTo("Hi there & 1 < 2");
    }

    @Test
    void lineCollapsesWhitespaceAndBlankBecomesNull() {
        assertThat(TextSanitizer.line("  Maya \n\t Okafor ")).isEqualTo("Maya Okafor");
        assertThat(TextSanitizer.plain("   ")).isNull();
    }
}
