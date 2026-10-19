package com.capstore.service;

import com.capstore.dto.AdminDtos.CustomerResponse;
import com.capstore.dto.ExtraDtos.CreateAdminRequest;
import com.capstore.entity.Role;
import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdminUserService {
    private final UserRepository users;
    private final PasswordEncoder encoder;

    @Transactional
    public CustomerResponse createAdmin(CreateAdminRequest r) {
        String email = r.email().trim().toLowerCase();
        if (users.existsByEmail(email)) throw ApiException.conflict("Email already registered");
        User u = new User();
        u.setName(r.name().trim());
        u.setEmail(email);
        u.setPhone(r.phone());
        u.setPasswordHash(encoder.encode(r.password()));
        u.setRole(Role.ADMIN);
        users.save(u);
        return new CustomerResponse(u.getId(), u.getName(), u.getEmail(), u.getPhone(), u.getRole().name(),
                null, null, false, true, u.getCreatedAt());
    }
}
