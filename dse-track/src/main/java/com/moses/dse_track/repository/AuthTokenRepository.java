package com.moses.dse_track.repository;

import com.moses.dse_track.model.AuthToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface AuthTokenRepository extends JpaRepository<AuthToken, Long> {

    Optional<AuthToken> findByTokenAndType(String token, AuthToken.AuthTokenType type);

    // Housekeeping — used/expired tokens are otherwise never removed
    long deleteByExpiresAtBeforeOrUsedTrue(LocalDateTime cutoff);
}
