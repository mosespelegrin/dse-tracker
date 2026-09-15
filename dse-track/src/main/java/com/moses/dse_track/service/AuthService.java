package com.moses.dse_track.service;

import com.moses.dse_track.exception.BusinessException;
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
                    return new BusinessException("invalid email or password");
                });
        if(!passwordEncoder.matches(password,user.getPassword())){
            loginAttemptService.loginFailed(email);
            throw new BusinessException("invalid username or password");
        }

        if (requireEmailVerification && !Boolean.TRUE.equals(user.getEmailVerified())) {
            throw new BusinessException("Please verify your email before logging in — check your inbox for the link");
        }

        loginAttemptService.loginSucceeded(email);
       return user;

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
        userRepository.save(user);

        // A leaked/stolen refresh token shouldn't survive a password reset
        refreshTokenRepository.deleteAllByUser(user);
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
