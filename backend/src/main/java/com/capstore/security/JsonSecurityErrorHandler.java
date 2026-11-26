package com.capstore.security;

import com.capstore.exception.GlobalExceptionHandler.ErrorBody;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.time.Instant;

/**
 * Errors raised inside the Spring Security filter chain never reach {@code @RestControllerAdvice}.
 * This writes them in the same {@link ErrorBody} shape the {@code GlobalExceptionHandler} uses,
 * so clients see one consistent error format.
 */
public class JsonSecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {
    private static final ObjectMapper MAPPER = JsonMapper.builder()
            .findAndAddModules()
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .build();

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse res, AuthenticationException e) throws IOException {
        write(res, HttpStatus.UNAUTHORIZED, "Unauthorized");
    }

    @Override
    public void handle(HttpServletRequest req, HttpServletResponse res, AccessDeniedException e) throws IOException {
        write(res, HttpStatus.FORBIDDEN, "Access denied");
    }

    public static void write(HttpServletResponse res, HttpStatus status, String message) throws IOException {
        if (res.isCommitted()) return;
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_JSON_VALUE);
        res.setCharacterEncoding("UTF-8");
        MAPPER.writeValue(res.getWriter(), new ErrorBody(Instant.now(), status.value(), message, null));
    }
}
