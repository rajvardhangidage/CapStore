package com.capstore.security;

import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AuthFacade {
    private final UserRepository users;

    public User currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        if (a == null || !a.isAuthenticated() || "anonymousUser".equals(a.getPrincipal())) {
            throw ApiException.unauthorized("Not authenticated");
        }
        return users.findByEmail(a.getName()).orElseThrow(() -> ApiException.unauthorized("User not found"));
    }
}
