package com.moses.dse_track.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.moses.dse_track.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

// Verifies the ID token Google Identity Services hands the frontend after a
// "Sign in with Google" click. Signature/audience/expiry are all checked
// locally against Google's public keys — no call back to Google needed per
// login, and no client secret required (that's only for server-side
// authorization-code flows, not this ID-token flow).
@Service
@Slf4j
public class GoogleAuthService {

    @Value("${app.google.client-id}")
    private String clientId;

    public record GoogleUser(String email, String name) {}

    public GoogleUser verify(String idTokenString) {
        if (clientId == null || clientId.isBlank()) {
            throw new BusinessException("Google Sign-In is not configured on this server");
        }

        try {
            GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                    new NetHttpTransport(), GsonFactory.getDefaultInstance())
                    .setAudience(Collections.singletonList(clientId))
                    .build();

            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                throw new BusinessException("Invalid Google sign-in token");
            }

            GoogleIdToken.Payload payload = idToken.getPayload();
            String email = payload.getEmail();
            boolean emailVerified = Boolean.TRUE.equals(payload.getEmailVerified());
            Object name = payload.get("name");

            if (email == null || !emailVerified) {
                throw new BusinessException("Google account email is not verified");
            }

            return new GoogleUser(email, name != null ? name.toString() : email);

        } catch (GeneralSecurityException | IOException e) {
            log.error("Failed to verify Google ID token", e);
            throw new BusinessException("Could not verify Google sign-in token");
        }
    }
}
