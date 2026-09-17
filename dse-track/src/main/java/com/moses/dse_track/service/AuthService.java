package com.moses.dse_track.service;

import com.moses.dse_track.exception.BusinessException;
import com.moses.dse_track.model.ActivityLog;
import com.moses.dse_track.model.AuthToken;
import com.moses.dse_track.model.User;
import com.moses.dse_track.repository.AuthTokenRepository;
import com.moses.dse_track.repository.RefreshTokenRepository;
import com.moses.dse_track.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final AuthTokenRepository authTokenRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final LoginAttemptService loginAttemptService;
    private final EmailService emailService;
    private final ActivityLogService activityLogService;

    @Value("${app.security.require-email-verification}")
    private boolean requireEmailVerification;



    //Registration service
    public User register(String name,String email,String password){
        if (userRepository.existsByEmail(email)){
            throw new BusinessException("Email already registered");
        }

        String hashedPassword=passwordEncoder.encode(password);
       User user = User.builder()
               .name(name)
               .email(email)
               .password(hashedPassword)
               .build();
        User saved;
        try {
            saved = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            // Two concurrent registrations for the same email raced past the
            // existsByEmail check above — the unique constraint caught it.
            throw new BusinessException("Email already registered");
        }

        issueAndSendToken(saved, AuthToken.AuthTokenType.EMAIL_VERIFICATION, 24);
        activityLogService.record(ActivityLog.EventType.REGISTER, saved.getId(), saved.getEmail(), null);

        return saved;
    }
    //login
    public User login(String email, String password){
        if (loginAttemptService.isBlocked(email)) {
            throw new BusinessException("Too many failed login attempts. Please try again later.");
        }

        User user=userRepository.findByEmail(email)
                .orElseThrow(()->{
                    loginAttemptService.loginFailed(email);
                    activityLogService.record(ActivityLog.EventType.LOGIN_FAILED, null, email, null);
                    return new BusinessException("invalid email or password");
                });
        if(!passwordEncoder.matches(password,user.getPassword())){
            loginAttemptService.loginFailed(email);
            activityLogService.record(ActivityLog.EventType.LOGIN_FAILED, user.getId(), email, null);
            throw new BusinessException("invalid username or password");
        }

        if (requireEmailVerification && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException("Please verify your email before logging in — check your inbox for the link");
        }

        loginAttemptService.loginSucceeded(email);
        activityLogService.record(ActivityLog.EventType.LOGIN_SUCCESS, user.getId(), email, null);
       return user;

    }

    // Finds the existing account for a Google-authenticated email, or creates
    // one. A Google sign-in already proves the email is owned by the caller,
    // so the new account is marked verified immediately and gets a random,
    // never-used password (this account only ever logs in via Google).
    public User findOrCreateGoogleUser(String email, String name) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            User user = User.builder()
                    .name(name)
                    .email(email)
                    .password(passwordEncoder.encode(UUID.randomUUID().toString()))
                    .emailVerified(true)
                    .build();
            try {
                return userRepository.save(user);
            } catch (DataIntegrityViolationException e) {
                // Lost a race with a concurrent signup for the same email
                return userRepository.findByEmail(email)
                        .orElseThrow(() -> new BusinessException("Could not sign in with Google"));
            }
        });
    }

    // Always succeeds from the caller's point of view, whether or not the
    // email exists — otherwise this endpoint could be used to enumerate accounts.
    public void resendVerificationEmail(String email) {
        String rateLimitKey = "resend-verify:" + email;
        if (loginAttemptService.isBlocked(rateLimitKey)) {
            return;
        }
        loginAttemptService.loginFailed(rateLimitKey);

        userRepository.findByEmail(email)
                .filter(user -> !Boolean.TRUE.equals(user.getEmailVerified()))
                .ifPresent(user -> issueAndSendToken(user, AuthToken.AuthTokenType.EMAIL_VERIFICATION, 24));
    }

    // Always succeeds from the caller's point of view, whether or not the
    // email exists — otherwise this endpoint could be used to enumerate accounts.
    public void requestPasswordReset(String email) {
        // Reuses the login rate-limiter under a separate key namespace, so this
        // endpoint can't be hammered to spam a victim's inbox or burn the app's
        // SMTP send quota. Response stays generic either way — see the caller.
        String rateLimitKey = "reset:" + email;
        if (loginAttemptService.isBlocked(rateLimitKey)) {
            return;
        }
        loginAttemptService.loginFailed(rateLimitKey);

        userRepository.findByEmail(email)
                .ifPresent(user -> issueAndSendToken(user, AuthToken.AuthTokenType.PASSWORD_RESET, 1));
    }

    public void resetPassword(String token, String newPassword) {
        AuthToken authToken = consumeToken(token, AuthToken.AuthTokenType.PASSWORD_RESET);

        User user = authToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        activityLogService.record(ActivityLog.EventType.PASSWORD_RESET, user.getId(), user.getEmail(), null);

        // A leaked/stolen refresh token shouldn't survive a password reset
        refreshTokenRepository.deleteAllByUser(user);
    }

    // Used by the logged-in "change my password" flow (as opposed to the
    // forgot-password flow above, which proves ownership via an emailed
    // token instead of the current password).
    public User changePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("User not found"));

        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new BusinessException("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        User saved = userRepository.save(user);
        activityLogService.record(ActivityLog.EventType.PASSWORD_CHANGED, saved.getId(), saved.getEmail(), null);

        // A leaked/stolen refresh token shouldn't survive a password change
        refreshTokenRepository.deleteAllByUser(saved);

        return saved;
    }

    public void verifyEmail(String token) {
        AuthToken authToken = consumeToken(token, AuthToken.AuthTokenType.EMAIL_VERIFICATION);

        User user = authToken.getUser();
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    private void issueAndSendToken(User user, AuthToken.AuthTokenType type, long validHours) {
        String token = generateToken();
        AuthToken authToken = AuthToken.builder()
                .user(user)
                .token(token)
                .type(type)
                .expiresAt(LocalDateTime.now().plusHours(validHours))
                .used(false)
                .build();
        authTokenRepository.save(authToken);

        if (type == AuthToken.AuthTokenType.EMAIL_VERIFICATION) {
            emailService.sendVerificationEmail(user.getEmail(), user.getName(), token);
        } else {
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), token);
        }
    }

    private AuthToken consumeToken(String token, AuthToken.AuthTokenType type) {
        AuthToken authToken = authTokenRepository.findByTokenAndType(token, type)
                .orElseThrow(() -> new BusinessException("Invalid or expired token"));

        if (Boolean.TRUE.equals(authToken.getUsed()) || authToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BusinessException("Invalid or expired token");
        }

        authToken.setUsed(true);
        authTokenRepository.save(authToken);
        return authToken;
    }

    private String generateToken() {
        return UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
    }

}
