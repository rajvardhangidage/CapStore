package com.capstore;

import com.capstore.config.SecurityConfig;
import com.capstore.security.JwtAuthFilter;
import com.capstore.security.JwtService;
import com.capstore.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

class SecurityConfigTest {
    @Test
    void corsAllowsOnlyConfiguredExplicitOrigins() {
        SecurityConfig config = config("https://store.example,https://admin.example");

        var cors = cors(config).getCorsConfiguration(
                new org.springframework.mock.web.MockHttpServletRequest("GET", "/api/v1/products"));

        assertEquals(List.of("https://store.example", "https://admin.example"), cors.getAllowedOrigins());
        assertEquals(List.of("Authorization", "Content-Type", "X-Razorpay-Signature", "X-Signature"),
                cors.getAllowedHeaders());
    }

    @Test
    void corsRejectsWildcardOrigins() {
        SecurityConfig config = config("*");

        assertThrows(IllegalStateException.class, () -> cors(config));
    }

    @Test
    void corsRejectsOriginsContainingPaths() {
        SecurityConfig config = config("https://store.example/app");

        assertThrows(IllegalStateException.class, () -> cors(config));
    }

    @Test
    void corsRejectsEmptyEntries() {
        SecurityConfig config = config("https://store.example,");

        assertThrows(IllegalStateException.class, () -> cors(config));
    }

    @Test
    void productionCorsRejectsHttpOrigins() {
        SecurityConfig config = config("http://store.example");
        ReflectionTestUtils.setField(config, "activeProfiles", "prod");

        assertThrows(IllegalStateException.class, () -> cors(config));
    }

    private SecurityConfig config(String origins) {
        SecurityConfig config = new SecurityConfig(
                new JwtAuthFilter(new JwtService("test-signing-key-for-security-config-tests", 15, 5),
                        mock(UserRepository.class)));
        ReflectionTestUtils.setField(config, "allowedOrigins", origins);
        return config;
    }

    private CorsConfigurationSource cors(SecurityConfig config) {
        return ReflectionTestUtils.invokeMethod(config, "corsConfigurationSource");
    }
}
