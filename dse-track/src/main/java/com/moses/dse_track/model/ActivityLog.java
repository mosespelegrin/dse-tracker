package com.moses.dse_track.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// Security/activity audit trail for the admin dashboard — who did what,
// not application debug logs. Written by ActivityLogService; never updated
// or deleted, so it stays a reliable record of what actually happened.
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "activity_logs")
public class ActivityLog {

    public enum EventType {
        REGISTER,
        LOGIN_SUCCESS,
        LOGIN_FAILED,
        PASSWORD_CHANGED,
        PASSWORD_RESET,
        FUNDAMENTALS_UPDATED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 30)
    private EventType eventType;

    // Nullable — a failed login before the email is known to belong to a
    // real account has no user id, only the email that was typed.
    @Column(name = "user_id")
    private Long userId;

    @Column(length = 100)
    private String email;

    // Free-text context, e.g. the stock ticker for a FUNDAMENTALS_UPDATED event.
    @Column(length = 255)
    private String detail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
