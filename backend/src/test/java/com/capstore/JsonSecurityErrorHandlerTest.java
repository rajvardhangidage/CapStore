package com.capstore;

import com.capstore.security.JsonSecurityErrorHandler;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.InsufficientAuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonSecurityErrorHandlerTest {
    private final JsonSecurityErrorHandler handler = new JsonSecurityErrorHandler();

    @Test
    void unauthenticatedRequestsGetJsonErrorBody() throws Exception {
        var res = new MockHttpServletResponse();
        handler.commence(new MockHttpServletRequest(), res, new InsufficientAuthenticationException("no token"));
        assertEquals(401, res.getStatus());
        assertTrue(res.getContentType().startsWith("application/json"));
        assertTrue(res.getContentAsString().contains("\"status\":401"));
        assertTrue(res.getContentAsString().contains("\"message\":\"Unauthorized\""));
        assertTrue(res.getContentAsString().matches(".*\"timestamp\":\"\\d{4}-.*"));
    }

    @Test
    void forbiddenRequestsGetJsonErrorBody() throws Exception {
        var res = new MockHttpServletResponse();
        handler.handle(new MockHttpServletRequest(), res, new AccessDeniedException("nope"));
        assertEquals(403, res.getStatus());
        assertTrue(res.getContentAsString().contains("\"message\":\"Access denied\""));
    }
}
