package io.github.joaodallagnol.backend.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class SessionVisibilityTest {

    @Test
    void shouldExposeStableApiValuesAndReserveFollowersForLater() {
        assertThat(SessionVisibility.fromValue("everyone")).isEqualTo(SessionVisibility.EVERYONE);
        assertThat(SessionVisibility.fromValue("only_me")).isEqualTo(SessionVisibility.ONLY_ME);
        assertThatThrownBy(() -> SessionVisibility.fromValue("followers"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
