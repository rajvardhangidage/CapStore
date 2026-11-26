package com.capstore;

import com.capstore.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void clientErrorsFromSpringMvcKeepTheirStatusInsteadOfBecoming500() {
        assertEquals(400, handler.typeMismatch(new MethodArgumentTypeMismatchException("abc", Long.class, "categoryId", null, null)).getStatusCode().value());
        assertEquals(400, handler.missingParam(new MissingServletRequestParameterException("q", "String")).getStatusCode().value());
        assertEquals(413, handler.tooLarge(new MaxUploadSizeExceededException(5)).getStatusCode().value());
        assertEquals(404, handler.noResource(new NoResourceFoundException(HttpMethod.GET, "nope")).getStatusCode().value());
        assertEquals(405, handler.method(new HttpRequestMethodNotSupportedException("PATCH")).getStatusCode().value());
    }

    @Test
    void otherFrameworkClientErrorsAreNotReportedAsServerErrors() {
        var e = new HttpMediaTypeNotSupportedException(MediaType.TEXT_PLAIN, List.of(MediaType.APPLICATION_JSON));
        assertEquals(415, handler.other(e).getStatusCode().value());
        assertEquals(500, handler.other(new IllegalStateException("boom")).getStatusCode().value());
    }

    @Test
    void typeMismatchNamesTheOffendingParameter() {
        var body = handler.typeMismatch(new MethodArgumentTypeMismatchException("abc", Long.class, "categoryId", null, null)).getBody();
        assertEquals("Invalid value for 'categoryId'", body.message());
    }

    @Test
    void concurrentModificationIsAConflictNotAServerError() {
        var e = new ObjectOptimisticLockingFailureException(Object.class, 1L);
        assertEquals(409, handler.concurrentUpdate(e).getStatusCode().value());
    }
}
