package com.capstore.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class AuthDtos {
    public record RegisterRequest(
            @NotBlank String name,
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits") String phone,
            @NotBlank @Size(min = 8, message = "Password must be at least 8 characters") String password,
            boolean businessAccount,
            String companyName,
            String gstNumber) {}

    public record LoginRequest(@NotBlank @Email String email, @NotBlank String password) {}

    public record RefreshRequest(@NotBlank String refreshToken) {}

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Email @Size(max = 190) String email,
            @NotBlank @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits") String phone) {}

    public record UserInfo(Long id, String name, String email, String phone, String role,
                           String companyName, boolean businessApproved) {}

    public record AuthResponse(String accessToken, String refreshToken, UserInfo user) {}
}
