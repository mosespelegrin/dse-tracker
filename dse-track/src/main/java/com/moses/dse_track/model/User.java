package com.moses.dse_track.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false, unique = true, length = 100)
    private String email;

    @Column(nullable = false)
    @ToString.Exclude
    private String password;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "email_verified", nullable = false)
    private Boolean emailVerified;

    public enum Role { USER, ADMIN }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    // Forces a password change (enforced by JwtFilter, blocking every other
    // endpoint until it's cleared) before the account can be used normally —
    // set for accounts issued with a known/default password, like the seeded admin.
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        if (this.emailVerified == null) {
            this.emailVerified = false;
        }
        if (this.role == null) {
            this.role = Role.USER;
        }
        if (this.mustChangePassword == null) {
            this.mustChangePassword = false;
        }
    }
}