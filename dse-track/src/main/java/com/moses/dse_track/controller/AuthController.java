package com.moses.dse_track.controller;

import com.moses.dse_track.dto.request.ForgotPasswordRequest;
import com.moses.dse_track.dto.request.GoogleAuthRequest;
import com.moses.dse_track.dto.request.LoginRequest;
import com.moses.dse_track.dto.request.RefreshTokenRequest;
import com.moses.dse_track.dto.request.RegisterRequest;
import com.moses.dse_track.dto.request.ResetPasswordRequest;
import com.moses.dse_track.dto.response.AuthResponse;
import com.moses.dse_track.model.User;
import com.moses.dse_track.service.AuthService;
import com.moses.dse_track.service.GoogleAuthService;
import com.moses.dse_track.service.JwtService;
import com.moses.dse_track.service.RefreshTokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final JwtService jwtService;
    private final RefreshTokenService refreshTokenService;
    private final GoogleAuthService googleAuthService;
    //POST /auth/register
     @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request){
         //take the request into the auth service for logics
         User user=authService.register(
                 request.getName(),
                 request.getEmail(),
                 request.getPassword()
         );
         String token= jwtService.generateToken(user.getId(), user.getEmail());
         String refreshToken = refreshTokenService.issue(user);
         //convert the response given byb the service layer into a dto
         AuthResponse response = AuthResponse.builder()
                 .token(token)
                 .refreshToken(refreshToken)
                 .name(user.getName())
                 .email(user.getEmail())
                 .message("Registration successfully — check your email to verify your account")
                 .build();
         return ResponseEntity.status(HttpStatus.CREATED).body(response);
     }
     @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request){
         User user=authService.login(
                 request.getEmail(),
                 request.getPassword()
         );
         //generate token
         String token=jwtService.generateToken(user.getId(), user.getEmail());
         String refreshToken = refreshTokenService.issue(user);
         AuthResponse response= AuthResponse.builder()
                 .token(token)
                 .refreshToken(refreshToken)
                 .name(user.getName())
                 .email(user.getEmail())
                 .message("login successful")
                 .build();
         return ResponseEntity.ok(response);
     }

    // POST /auth/refresh — exchange a valid refresh token for a new access token (and a rotated refresh token)
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        RefreshTokenService.RefreshResult result = refreshTokenService.refresh(request.getRefreshToken());
        User user = result.user();
        String token = jwtService.generateToken(user.getId(), user.getEmail());

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .refreshToken(result.newRefreshToken())
                .name(user.getName())
                .email(user.getEmail())
                .message("Token refreshed")
                .build();
        return ResponseEntity.ok(response);
    }

    // POST /auth/logout — revokes a refresh token (logs out just that session)
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshTokenRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.noContent().build();
    }

    // POST /auth/google — sign in (or auto-register) using a Google ID token
    @PostMapping("/google")
    public ResponseEntity<AuthResponse> googleSignIn(@Valid @RequestBody GoogleAuthRequest request) {
        GoogleAuthService.GoogleUser googleUser = googleAuthService.verify(request.getIdToken());
        User user = authService.findOrCreateGoogleUser(googleUser.email(), googleUser.name());

        String token = jwtService.generateToken(user.getId(), user.getEmail());
        String refreshToken = refreshTokenService.issue(user);

        AuthResponse response = AuthResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .name(user.getName())
                .email(user.getEmail())
                .message("Signed in with Google")
                .build();
        return ResponseEntity.ok(response);
    }

    // POST /auth/resend-verification — always responds the same way, whether or not the email exists/is already verified
    @PostMapping("/resend-verification")
    public ResponseEntity<AuthResponse> resendVerification(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(AuthResponse.builder()
                .message("If that email is registered and not yet verified, a new link has been sent")
                .build());
    }

    // POST /auth/forgot-password — always responds the same way, whether or not the email exists
    @PostMapping("/forgot-password")
    public ResponseEntity<AuthResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.requestPasswordReset(request.getEmail());
        return ResponseEntity.ok(AuthResponse.builder()
                .message("If that email is registered, a reset link has been sent")
                .build());
    }

    // POST /auth/reset-password
    @PostMapping("/reset-password")
    public ResponseEntity<AuthResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok(AuthResponse.builder()
                .message("Password reset successfully — please log in again")
                .build());
    }

    // GET /auth/verify-email?token=... — clicked directly from the verification email
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.ok("<h2>Email verified</h2><p>You can close this tab and log in.</p>");
    }
}
