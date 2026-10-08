package com.connecthub.moderation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfanityFilterTest {

    private final ProfanityFilter filter = new ProfanityFilter();

    @Test
    void masksWholeWordsWithSuffixesAndLeetspeak() {
        assertThat(filter.clean("What the FUCK, this is shit")).isEqualTo("What the F***, this is s***");
        assertThat(filter.clean("sh1t happens, a$$hole")).isEqualTo("s*** happens, a******");
        assertThat(filter.clean("total bullshitting")).isEqualTo("total b" + "*".repeat("ullshitting".length()));
        assertThat(filter.clean("fuuuuck")).isEqualTo("f******");
    }

    @Test
    void leavesInnocentWordsAlone() {
        String clean = "Scunthorpe, peacock, shitake, assassin, cocktail, Dickens";
        assertThat(filter.clean(clean)).isEqualTo(clean);
        assertThat(filter.isProfane(clean)).isFalse();
        assertThat(filter.clean(null)).isNull();
    }
}
