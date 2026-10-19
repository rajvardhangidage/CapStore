package com.capstore.service;

import com.capstore.dto.AuthDtos.*;
import com.capstore.entity.Role;
import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.repository.UserRepository;
import com.capstore.security.JwtService;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.conflict("Email already registered");

        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPhone(r.phone());
        u.setPasswordHash(encoder.encode(r.password()));
        if (r.businessAccount()) {
            if (isBlank(r.companyName()) || isBlank(r.gstNumber()))
                throw ApiException.badRequest("Company name and GST number are required for business accounts");
            u.setRole(Role.B2B_USER);
            u.setCompanyName(r.companyName().trim());
            u.setGstNumber(r.gstNumber().trim().toUpperCase());
            u.setBusinessApproved(false); // admin must approve before bulk pricing applies
        } else {
            u.setRole(Role.USER);
        }
        users.save(u);
        return tokens(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest r) {
        User u = users.findByEmail(r.email().trim().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));
        if (!encoder.matches(r.password(), u.getPasswordHash()))
            throw ApiException.unauthorized("Invalid email or password");
        if (!u.isEnabled()) throw ApiException.unauthorized("Account disabled");
        return tokens(u);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(RefreshRequest r) {
        Claims c = jwt.parse(r.refreshToken());
        if (!"refresh".equals(c.get("type", String.class))) throw ApiException.unauthorized("Invalid token type");
        User u = users.findByEmail(c.getSubject())
                .filter(User::isEnabled)
                .orElseThrow(() -> ApiException.unauthorized("User not found"));
        return tokens(u);
    }

    @Transactional
    public AuthResponse updateProfile(User u, UpdateProfileRequest r) {
        String email = r.email().trim().toLowerCase();
        if (!email.equals(u.getEmail()) && users.existsByEmailAndIdNot(email, u.getId()))
            throw ApiException.conflict("Email already registered");
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPhone(r.phone());
        return tokens(u);
    }

    public static UserInfo info(User u) {
        return new UserInfo(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole().name(),
                u.getCompanyName(), u.isBusinessApproved());
    }

    private AuthResponse tokens(User u) {
        return new AuthResponse(jwt.accessToken(u), jwt.refreshToken(u), info(u));
    }

    private boolean isBlank(String s) { return s == null || s.isBlank(); }
}
