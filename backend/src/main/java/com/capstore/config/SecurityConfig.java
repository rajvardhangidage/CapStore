package com.capstore.config;

import com.capstore.security.JsonSecurityErrorHandler;
import com.capstore.security.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.net.URI;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthFilter jwtFilter;

    @Value("${app.cors.allowed-origins}")
    private String allowedOrigins;

    @Value("${spring.profiles.active:}")
    private String activeProfiles = "";

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(c -> c.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(e -> {
                JsonSecurityErrorHandler errors = new JsonSecurityErrorHandler();
                e.authenticationEntryPoint(errors).accessDeniedHandler(errors);
            })
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/v1/auth/**", "/api/v1/contact", "/api/v1/payments/webhook", "/webhooks/courier/**",
                                 "/actuator/health/**", "/actuator/prometheus",
                                 "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/products/**", "/api/v1/categories/**",
                                 "/api/v1/banners", "/api/v1/config", "/uploads/**").permitAll()
                .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(",", -1))
                .map(String::trim)
                .toList();
        boolean production = Arrays.stream(activeProfiles.split(","))
                .map(String::trim)
                .anyMatch("prod"::equals);
        if (origins.isEmpty() || origins.stream().anyMatch(origin -> !isAllowedOrigin(origin, production))) {
            throw new IllegalStateException("CORS_ORIGINS must contain explicit HTTP(S) origins; production origins must use HTTPS");
        }
        c.setAllowedOrigins(origins);
        c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization", "Content-Type", "X-Razorpay-Signature", "X-Signature"));
        c.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource src = new UrlBasedCorsConfigurationSource();
        src.registerCorsConfiguration("/**", c);
        return src;
    }

    private boolean isAllowedOrigin(String origin, boolean production) {
        try {
            URI uri = URI.create(origin);
            return !origin.equals("*")
                    && ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    && (!production || "https".equalsIgnoreCase(uri.getScheme()))
                    && uri.getHost() != null
                    && uri.getRawUserInfo() == null
                    && uri.getRawQuery() == null
                    && uri.getRawFragment() == null
                    && (uri.getRawPath() == null || uri.getRawPath().isEmpty());
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
