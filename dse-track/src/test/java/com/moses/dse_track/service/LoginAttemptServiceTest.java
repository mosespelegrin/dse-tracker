package com.moses.dse_track.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    private final LoginAttemptService service = new LoginAttemptService();

    @Test
    void notBlockedBeforeAnyFailures() {
        assertThat(service.isBlocked("user@example.com")).isFalse();
    }

    @Test
    void blocksAfterFiveFailuresAndClearsOnSuccess() {
        String email = "user@example.com";

        for (int i = 0; i < 4; i++) {
            service.loginFailed(email);
            assertThat(service.isBlocked(email)).isFalse();
        }

        service.loginFailed(email); // 5th failure
        assertThat(service.isBlocked(email)).isTrue();

        service.loginSucceeded(email);
        assertThat(service.isBlocked(email)).isFalse();
    }

    @Test
    void isCaseInsensitiveAndTrimsWhitespace() {
        service.loginFailed("User@Example.com");
        for (int i = 0; i < 4; i++) {
            service.loginFailed("  user@example.com  ");
        }
        assertThat(service.isBlocked("USER@EXAMPLE.COM")).isTrue();
    }

    @Test
    void differentKeysAreTrackedIndependently() {
        for (int i = 0; i < 5; i++) {
            service.loginFailed("victim@example.com");
        }
        assertThat(service.isBlocked("victim@example.com")).isTrue();
        assertThat(service.isBlocked("someone-else@example.com")).isFalse();
    }
}
