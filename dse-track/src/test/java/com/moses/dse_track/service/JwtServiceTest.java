package com.moses.dse_track.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService();

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(jwtService, "secret", "test_only_jwt_secret_key_at_least_32_bytes_long");
        ReflectionTestUtils.setField(jwtService, "expiration", 3_600_000L); // 1 hour
    }

    @Test
    void generatesATokenThatRoundTripsUserIdAndEmail() {
        String token = jwtService.generateToken(42L, "user@example.com", "USER", false);

        assertThat(jwtService.isTokenValid(token)).isTrue();
        assertThat(jwtService.extractUserId(token)).isEqualTo(42L);
        assertThat(jwtService.extractEmail(token)).isEqualTo("user@example.com");
        assertThat(jwtService.extractRole(token)).isEqualTo("USER");
        assertThat(jwtService.extractMustChangePassword(token)).isFalse();
    }

    @Test
    void garbageTokenIsInvalid() {
        assertThat(jwtService.isTokenValid("not-a-real-token")).isFalse();
    }

    @Test
    void alreadyExpiredTokenIsInvalid() {
        ReflectionTestUtils.setField(jwtService, "expiration", -1000L); // expires 1s in the past
        String token = jwtService.generateToken(1L, "user@example.com", "USER", false);

        assertThat(jwtService.isTokenValid(token)).isFalse();
    }
}
