package com.gameshowcenter.backend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.*;

class RateLimitingFilterTest {

    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter();
    }

    @Test
    void testDownloadRateLimitExceeded() throws Exception {
        String testIp = "192.168.1.50";

        // LIMIT_DOWNLOAD is 5 requests per minute
        for (int i = 1; i <= 5; i++) {
            MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/download/offline-template");
            request.setRemoteAddr(testIp);
            MockHttpServletResponse response = new MockHttpServletResponse();
            MockFilterChain chain = new MockFilterChain();

            filter.doFilter(request, response, chain);
            assertEquals(200, response.getStatus());
            assertEquals(String.valueOf(5 - i), response.getHeader("X-RateLimit-Remaining"));
        }

        // 6th request: Rate limit exceeded -> 429 Too Many Requests
        MockHttpServletRequest request6 = new MockHttpServletRequest("GET", "/api/download/offline-template");
        request6.setRemoteAddr(testIp);
        MockHttpServletResponse response6 = new MockHttpServletResponse();
        MockFilterChain chain6 = new MockFilterChain();

        filter.doFilter(request6, response6, chain6);
        assertEquals(429, response6.getStatus());
        assertEquals("0", response6.getHeader("X-RateLimit-Remaining"));
        assertNotNull(response6.getHeader("Retry-After"));
        assertTrue(response6.getContentAsString().contains("Demasiadas peticiones"));
    }

    @Test
    void testOptionsPreflightBypassesRateLimit() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/payment/verify-promo");
        request.setRemoteAddr("10.0.0.1");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();

        filter.doFilter(request, response, chain);
        assertEquals(200, response.getStatus());
    }
}
