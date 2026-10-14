package com.tripmind.services;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IcsEscapeTest {

    @Test
    void loneCarriageReturnCannotInjectProperties() {
        String escaped = IcsExporter.escape("a\rBEGIN:VEVENT\r\nb\nc");
        assertThat(escaped).doesNotContain("\r").doesNotContain("\n");
        assertThat(escaped).isEqualTo("a\\nBEGIN:VEVENT\\nb\\nc");
    }
}
