package com.capstore.service;

import com.capstore.entity.PasswordResetToken;
import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.repository.PasswordResetTokenRepository;
import com.capstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class PasswordResetService {
    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder encoder;
    private final NotificationService notifier;

    @Value("${app.frontend-url}") private String frontendUrl;

    /** Always succeeds from the caller's view, so account existence is not revealed. */
    @Transactional
    public void request(String email) {
        users.findByEmail(email.trim().toLowerCase()).filter(User::isEnabled).ifPresent(u -> {
            byte[] raw = new byte[32];
            new SecureRandom().nextBytes(raw);
            String token = Base64.getUrlEncoder().withoutPadding().encodeToString(raw);
            PasswordResetToken t = new PasswordResetToken();
            t.setUser(u);
            t.setTokenHash(sha256(token));
            t.setExpiresAt(LocalDateTime.now().plusMinutes(30));
            tokens.save(t);
            notifier.passwordReset(u.getEmail(), frontendUrl + "/reset-password?token=" + token);
        });
    }

    @Transactional
    public void reset(String token, String newPassword) {
        PasswordResetToken t = tokens.findByTokenHash(sha256(token))
                .filter(x -> !x.isUsed() && x.getExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> ApiException.badRequest("This reset link is invalid or has expired"));
        t.getUser().setPasswordHash(encoder.encode(newPassword));
        t.setUsed(true);
    }

    private String sha256(String s) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
