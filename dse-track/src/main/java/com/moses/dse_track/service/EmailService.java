package com.moses.dse_track.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // Many SMTP servers (Gmail included) reject a message with no From header
    // at all — SimpleMailMessage doesn't default this on its own.
    @Value("${spring.mail.username}")
    private String fromAddress;

    // A mail failure (bad credentials, SMTP down, etc.) must never break the
    // scheduled price-fetch loop — so this swallows and logs instead of throwing.
    public void sendAlertTriggeredEmail(String toEmail, String userName, String ticker,
                                         String companyName, String conditionType,
                                         BigDecimal targetPrice, BigDecimal currentPrice) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("DSE Track alert: " + ticker + " is now " + conditionType.toLowerCase() + " TZS " + targetPrice);
            message.setText(
                    "Hi " + userName + ",\n\n" +
                    ticker + " (" + companyName + ") just went " + conditionType.toLowerCase() +
                    " your target price of TZS " + targetPrice + ".\n\n" +
                    "Current price: TZS " + currentPrice + "\n\n" +
                    "— DSE Track"
            );
            mailSender.send(message);
            log.info("Alert email sent to {} for {}", toEmail, ticker);
        } catch (Exception e) {
            log.error("Failed to send alert email to {}", toEmail, e);
        }
    }

    public void sendVerificationEmail(String toEmail, String userName, String token) {
        try {
            String link = frontendUrl + "/auth/verify-email?token=" + token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Verify your DSE Track account");
            message.setText(
                    "Hi " + userName + ",\n\n" +
                    "Click the link below to verify your email address:\n" + link + "\n\n" +
                    "This link expires in 24 hours.\n\n" +
                    "— DSE Track"
            );
            mailSender.send(message);
            log.info("Verification email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send verification email to {}", toEmail, e);
        }
    }

    public void sendPasswordResetEmail(String toEmail, String userName, String token) {
        try {
            String link = frontendUrl + "/reset-password.html?token=" + token;
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(toEmail);
            message.setSubject("Reset your DSE Track password");
            message.setText(
                    "Hi " + userName + ",\n\n" +
                    "We received a request to reset your password. Use this link (or the token below) " +
                    "within the next hour:\n" + link + "\n\n" +
                    "Token: " + token + "\n\n" +
                    "If you didn't request this, you can safely ignore this email.\n\n" +
                    "— DSE Track"
            );
            mailSender.send(message);
            log.info("Password reset email sent to {}", toEmail);
        } catch (Exception e) {
            log.error("Failed to send password reset email to {}", toEmail, e);
        }
    }
}
