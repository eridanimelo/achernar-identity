package com.eridani.achernar.identity.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eridani.achernar.identity.shared.error.ApiException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class EventCursorTest {

    @Test
    void idaEVolta() {
        EventCursor c = new EventCursor(Instant.parse("2026-02-03T04:05:06.123456Z"), UUID.randomUUID());
        assertThat(EventCursor.decode(c.encode())).isEqualTo(c);
    }

    @Test
    void cursorEhOpacoEUrlSafe() {
        String enc = new EventCursor(Instant.parse("2026-02-03T04:05:06Z"), UUID.randomUUID()).encode();
        assertThat(enc).matches("^[A-Za-z0-9_-]+$");
    }

    @Test
    void invalidoViraErroDeValidacao() {
        for (String ruim : new String[] {"", "###", "bm8tYmFy", "MjAyNi0wMi0wM3xub3QtYS11dWlk"}) {
            assertThatThrownBy(() -> EventCursor.decode(ruim)).isInstanceOf(ApiException.class);
        }
    }
}
