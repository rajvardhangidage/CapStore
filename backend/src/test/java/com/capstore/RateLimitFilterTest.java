package com.capstore;

import com.capstore.security.RateLimitFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RateLimitFilterTest {

    @Test
    void disabledRateLimitAllowsMoreThanTenLoginAttempts() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "enabled", false);

        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);

            assertEquals(200, response.getStatus());
        }
    }

    @Test
    void contactFormRemainsRateLimitedWhenAuthRateLimitIsDisabled() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "enabled", false);

        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/contact");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(i < 10 ? 200 : 429, response.getStatus());
        }
    }

    @Test
    void enabledRateLimitRejectsTheEleventhLoginAttempt() throws Exception {
        RateLimitFilter filter = new RateLimitFilter();
        ReflectionTestUtils.setField(filter, "enabled", true);

        for (int i = 0; i < 11; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
            MockHttpServletResponse response = new MockHttpServletResponse();

            filter.doFilter(request, response, new MockFilterChain());

            assertEquals(i < 10 ? 200 : 429, response.getStatus());
        }
    }
}
