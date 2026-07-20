package io.github.joaodallagnol.backend.backlog;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BacklogStatusTest {

    @Test
    void shouldResolveKnownStatusValues() {
        assertThat(BacklogStatus.from("pending")).isEqualTo(BacklogStatus.PENDING);
        assertThat(BacklogStatus.from("in_progress")).isEqualTo(BacklogStatus.IN_PROGRESS);
        assertThat(BacklogStatus.from("done")).isEqualTo(BacklogStatus.DONE);
    }

    @Test
    void shouldRejectUnknownStatusValue() {
        assertThatThrownBy(() -> BacklogStatus.from("archived"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invalid backlog status.");
    }
}
