package com.capstore;

import com.capstore.entity.Role;
import com.capstore.entity.User;
import com.capstore.exception.ApiException;
import com.capstore.security.JwtService;
import org.junit.jupiter.api.Test;

import java.security.SecureRandom;
import java.util.HexFormat;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {
    private final JwtService jwt = new JwtService(testSecret(), 30, 7);

    private static String testSecret() {
        byte[] secret = new byte[32];
        new SecureRandom().nextBytes(secret);
        return HexFormat.of().formatHex(secret);
    }

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

    @Test
    void rejectsSigningKeysShorterThanThirtyTwoBytes() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService("too-short", 30, 7));
    }

    @Test
    void rejectsNonPositiveTokenLifetimes() {
        assertThrows(IllegalArgumentException.class, () -> new JwtService(testSecret(), 0, 7));
        assertThrows(IllegalArgumentException.class, () -> new JwtService(testSecret(), 30, -1));
    }
}
