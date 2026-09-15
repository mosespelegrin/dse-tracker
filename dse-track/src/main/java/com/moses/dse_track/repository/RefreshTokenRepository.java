package com.moses.dse_track.repository;

import com.moses.dse_track.model.RefreshToken;
import com.moses.dse_track.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    // Called on password reset — a stolen refresh token shouldn't survive it
    void deleteAllByUser(User user);

    // Housekeeping — expired/revoked tokens are otherwise never removed
    long deleteByExpiresAtBeforeOrRevokedTrue(LocalDateTime cutoff);
}
