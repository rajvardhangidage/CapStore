package com.capstore.controller;

import com.capstore.dto.AuthDtos.*;
import com.capstore.dto.ExtraDtos.ForgotPasswordRequest;
import com.capstore.dto.ExtraDtos.ResetPasswordRequest;
import com.capstore.service.PasswordResetService;
import com.capstore.security.AuthFacade;
import com.capstore.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final AuthFacade auth;
    private final PasswordResetService resetService;

    @PostMapping("/auth/register")
    @ResponseStatus(HttpStatus.CREATED)
    public AuthResponse register(@Valid @RequestBody RegisterRequest r) { return authService.register(r); }

    @PostMapping("/auth/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest r) { return authService.login(r); }

    @PostMapping("/auth/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest r) { return authService.refresh(r); }

    @PostMapping("/auth/forgot-password")
    public java.util.Map<String, String> forgot(@Valid @RequestBody ForgotPasswordRequest r) {
        resetService.request(r.email());
        return java.util.Map.of("message", "If that email is registered, a reset link has been sent.");
    }

    @PostMapping("/auth/reset-password")
    public java.util.Map<String, String> reset(@Valid @RequestBody ResetPasswordRequest r) {
        resetService.reset(r.token(), r.newPassword());
        return java.util.Map.of("message", "Password updated. You can log in now.");
    }

    @GetMapping("/me")
    public UserInfo me() { return AuthService.info(auth.currentUser()); }

    @PutMapping("/me")
    public AuthResponse updateProfile(@Valid @RequestBody UpdateProfileRequest r) {
        return authService.updateProfile(auth.currentUser(), r);
    }
}
