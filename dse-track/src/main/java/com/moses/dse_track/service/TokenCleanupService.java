package com.moses.dse_track.service;

import com.moses.dse_track.repository.AuthTokenRepository;
import com.moses.dse_track.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

// auth_tokens and refresh_tokens rows are never deleted anywhere else — used
// verification/reset tokens and revoked/expired refresh tokens would otherwise
// accumulate forever.
@Service
@RequiredArgsConstructor
@Slf4j
public class TokenCleanupService {

    private final AuthTokenRepository authTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;

    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupExpiredTokens() {
        LocalDateTime now = LocalDateTime.now();
        long deletedAuthTokens = authTokenRepository.deleteByExpiresAtBeforeOrUsedTrue(now);
        long deletedRefreshTokens = refreshTokenRepository.deleteByExpiresAtBeforeOrRevokedTrue(now);
        log.info("Token cleanup: removed {} auth tokens, {} refresh tokens", deletedAuthTokens, deletedRefreshTokens);
    }
}
