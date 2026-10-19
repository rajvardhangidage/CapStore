package com.capstore;

import com.capstore.entity.Role;
import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.security.JwtService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService jwt = new JwtService("test-secret-test-secret-test-secret-123456", 30, 7);

    private User user() {
        User u = new User();
        u.setEmail("a@b.com");
        u.setRole(Role.USER);
        return u;
    }

    @Test
    void accessTokenRoundTrip() {
        var claims = jwt.parse(jwt.accessToken(user()));
        assertEquals("a@b.com", claims.getSubject());
        assertEquals("access", claims.get("type", String.class));
    }

    @Test
    void refreshTokenIsTypedAsRefresh() {
        assertEquals("refresh", jwt.parse(jwt.refreshToken(user())).get("type", String.class));
    }

    @Test
    void tamperedTokenIsRejected() {
        String t = jwt.accessToken(user());
        assertThrows(ApiException.class, () -> jwt.parse(t.substring(0, t.length() - 3) + "abc"));
    }
}
