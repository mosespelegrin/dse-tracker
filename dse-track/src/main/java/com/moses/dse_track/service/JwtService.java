package com.moses.dse_track.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiration}")
    private Long expiration;

    // ───────────────────────────────────────
    // Generate a token for a user
    // Called after successful login
    // ───────────────────────────────────────
    public String generateToken(Long userId, String email) {
        return Jwts.builder()
                .subject(String.valueOf(userId))   // userId stored in token
                .claim("email", email)             // email stored in token
                .issuedAt(new Date())              // when token was created
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())         // sign with secret
                .compact();
    }

    // ───────────────────────────────────────
    // Extract userId from token
    // Called on every protected request
    // ───────────────────────────────────────
    public Long extractUserId(String token) {
        return Long.parseLong(getClaims(token).getSubject());
    }

    // Extract email from token
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    // Check if token is still valid (not expired)
    public boolean isTokenValid(String token) {
        try {
            return getClaims(token).getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }


    // Internal helpers

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
}