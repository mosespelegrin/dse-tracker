package com.moses.dse_track.service;

import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.RefreshToken;
import com.moses.dse_track.model.User;
import com.moses.dse_track.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;

// Refresh tokens are additive to the existing stateless-JWT login flow — the
// access token's own expiration is unchanged, so the current frontend keeps
// working exactly as before. This just gives callers who want it a way to
// get a new access token (and to actually log out / revoke a session,
// which a bare stateless JWT can never support).
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration}")
    private long refreshExpirationMs;

    private final SecureRandom secureRandom = new SecureRandom();

    public record RefreshResult(User user, String newRefreshToken) {}

    public String issue(User user) {
        String raw = generateRawToken();
        RefreshToken entity = RefreshToken.builder()
                .user(user)
                .tokenHash(hash(raw))
                .expiresAt(LocalDateTime.now().plus(Duration.ofMillis(refreshExpirationMs)))
                .revoked(false)
                .build();
        refreshTokenRepository.save(entity);
        return raw;
    }

    // Validates the token, then rotates it (revokes the old one, issues a new one)
    public RefreshResult refresh(String rawToken) {
        RefreshToken entity = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessException("Invalid refresh token"));

        if (Boolean.TRUE.equals(entity.getRevoked()) || entity.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Refresh token has expired or been revoked — please log in again");
        }

        entity.setRevoked(true);
        refreshTokenRepository.save(entity);

        String newToken = issue(entity.getUser());
        return new RefreshResult(entity.getUser(), newToken);
    }

    // Logout — revoking is idempotent/silent on an unknown or already-revoked
    // token so this endpoint can't be used to probe for valid tokens
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokenRepository.save(t);
        });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hashed) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
