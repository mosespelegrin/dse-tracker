package com.moses.dse_track.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

// In-memory brute-force guard for /auth/login. Tracks failed attempts per
// email and locks that email out for a short window once the limit is hit.
@Service
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT_WINDOW = Duration.ofMinutes(15);

    private record Attempt(AtomicInteger count, Instant windowStart) {}

    private final ConcurrentHashMap<String, Attempt> attempts = new ConcurrentHashMap<>();

    public void loginFailed(String email) {
        attempts.compute(key(email), (k, existing) -> {
            if (existing == null || isExpired(existing)) {
                return new Attempt(new AtomicInteger(1), Instant.now());
            }
            existing.count().incrementAndGet();
            return existing;
        });
    }

    public void loginSucceeded(String email) {
        attempts.remove(key(email));
    }

    public boolean isBlocked(String email) {
        Attempt attempt = attempts.get(key(email));
        return attempt != null && !isExpired(attempt) && attempt.count().get() >= MAX_ATTEMPTS;
    }

    private boolean isExpired(Attempt attempt) {
        return Instant.now().isAfter(attempt.windowStart().plus(LOCKOUT_WINDOW));
    }

    private String key(String email) {
        return email.trim().toLowerCase();
    }
}
