package com.moses.dse_track.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// The raw token is only ever handed to the client — this row stores a SHA-256
// hash of it, so a DB leak alone can't be used to impersonate a session.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(nullable = false)
    private Boolean revoked;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // Closes a race where two near-simultaneous /auth/refresh calls on the
    // same token could both pass the revoked/expiry check before either
    // write lands — the second save now fails fast instead of silently
    // minting two live tokens from one.
    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.revoked == null) {
            this.revoked = false;
        }
    }
}
