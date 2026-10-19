package com.capstore.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/** Simple per-IP limiter for sensitive public endpoints (10 requests/minute). Use a gateway/Redis for multi-node setups. */
@Component
public class RateLimitFilter extends OncePerRequestFilter {
    @Value("${app.security.rate-limit.enabled:false}")
    private boolean enabled;

    private static final Set<String> LIMITED = Set.of("/api/v1/auth/login", "/api/v1/auth/register",
            "/api/v1/auth/forgot-password", "/api/v1/auth/reset-password");
    private static final String CONTACT_PATH = "/api/v1/contact";
    private static final int MAX = 10;
    private static final long WINDOW_MS = 60_000;
    private final Map<String, Deque<Long>> hits = new ConcurrentHashMap<>();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest req) {
        return !"POST".equals(req.getMethod())
                || (!CONTACT_PATH.equals(req.getRequestURI()) && (!enabled || !LIMITED.contains(req.getRequestURI())));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String key = req.getRemoteAddr() + "|" + req.getRequestURI();
        long now = System.currentTimeMillis();
        Deque<Long> q = hits.computeIfAbsent(key, k -> new ArrayDeque<>());
        boolean allowed;
        synchronized (q) {
            while (!q.isEmpty() && now - q.peekFirst() > WINDOW_MS) q.pollFirst();
            allowed = q.size() < MAX;
            if (allowed) q.addLast(now);
        }
        if (hits.size() > 10_000) hits.clear();
        if (!allowed) {
            res.setStatus(429);
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":429,\"message\":\"Too many attempts. Please wait a minute and try again.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
