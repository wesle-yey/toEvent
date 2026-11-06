package com.wesleyey.toEvent;

import com.wesleyey.toEvent.config.RateLimitingFilter;
import com.wesleyey.toEvent.services.RateLimitingService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitingTest {

    private RateLimitingService rateLimitingService;
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
        rateLimitingFilter = new RateLimitingFilter(rateLimitingService);
    }

    @Test
    void testRegisterRateLimit_10RequestsAllowed_11thBlocked() {
        String ip = "192.168.1.10";
        for (int i = 0; i < 10; i++) {
            ConsumptionProbe probe = rateLimitingService.tryConsumeRegister(ip);
            assertTrue(probe.isConsumed(), "Request " + (i + 1) + " should be allowed");
        }

        ConsumptionProbe blockedProbe = rateLimitingService.tryConsumeRegister(ip);
        assertFalse(blockedProbe.isConsumed(), "11th request should be blocked");
        assertTrue(blockedProbe.getNanosToWaitForRefill() > 0);
    }

    @Test
    void testLoginRateLimit_15RequestsAllowed_16thBlocked() {
        String ip = "192.168.1.20";
        for (int i = 0; i < 15; i++) {
            ConsumptionProbe probe = rateLimitingService.tryConsumeLogin(ip);
            assertTrue(probe.isConsumed(), "Login request " + (i + 1) + " should be allowed");
        }

        ConsumptionProbe blockedProbe = rateLimitingService.tryConsumeLogin(ip);
        assertFalse(blockedProbe.isConsumed(), "16th login request should be blocked");
    }

    @Test
    void testEventCreateRateLimit_20RequestsAllowed_21stBlocked() {
        String userId = "user_abc123";
        for (int i = 0; i < 20; i++) {
            ConsumptionProbe probe = rateLimitingService.tryConsumeEventCreate(userId);
            assertTrue(probe.isConsumed(), "Event creation request " + (i + 1) + " should be allowed");
        }

        ConsumptionProbe blockedProbe = rateLimitingService.tryConsumeEventCreate(userId);
        assertFalse(blockedProbe.isConsumed(), "21st event creation request should be blocked");
    }

    @Test
    void testFilter_AppliesGeneralEventRateLimitToUpdateAndDelete() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);

        for (String method : new String[] {"PUT", "DELETE"}) {
            rateLimitingService.resetAll();
            for (int i = 0; i < 60; i++) {
                assertTrue(rateLimitingService.tryConsumeEventGeneral("127.0.0.1").isConsumed());
            }

            MockHttpServletRequest request = new MockHttpServletRequest(method, "/api/events/event-1");
            request.setServletPath("/api/events/event-1");
            MockHttpServletResponse response = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(request, response, filterChain);

            assertEquals(429, response.getStatus(), method + " request should be rate limited");
        }

        verifyNoInteractions(filterChain);
    }

    @Test
    void testFilter_BlocksWith429WhenRegisterLimitExceeded() throws ServletException, IOException {
        String clientIp = "203.0.113.50";
        FilterChain filterChain = mock(FilterChain.class);

        // Consume all 10 tokens
        for (int i = 0; i < 10; i++) {
            MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/auth/register");
            req.setServletPath("/api/auth/register");
            req.addHeader("X-Forwarded-For", clientIp + ", 10.0.0.1");
            MockHttpServletResponse res = new MockHttpServletResponse();

            rateLimitingFilter.doFilter(req, res, filterChain);
            assertEquals(200, res.getStatus());
        }

        verify(filterChain, times(10)).doFilter(any(), any());

        // 11th request: must return HTTP 429 with Retry-After header and JSON body
        MockHttpServletRequest req11 = new MockHttpServletRequest("POST", "/api/auth/register");
        req11.setServletPath("/api/auth/register");
        req11.addHeader("X-Forwarded-For", clientIp + ", 10.0.0.1");
        MockHttpServletResponse res11 = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(req11, res11, filterChain);

        assertEquals(429, res11.getStatus());
        assertNotNull(res11.getHeader("Retry-After"));
        assertTrue(res11.getContentAsString().contains("TOO_MANY_REQUESTS"));
        // Ensure filterChain was NOT called an 11th time
        verify(filterChain, times(10)).doFilter(any(), any());
    }

    @Test
    void testFilter_OptionsPreflightIsNeverRateLimited() throws ServletException, IOException {
        FilterChain filterChain = mock(FilterChain.class);
        MockHttpServletRequest req = new MockHttpServletRequest("OPTIONS", "/api/auth/register");
        req.setServletPath("/api/auth/register");
        MockHttpServletResponse res = new MockHttpServletResponse();

        rateLimitingFilter.doFilter(req, res, filterChain);

        verify(filterChain, times(1)).doFilter(req, res);
        assertEquals(200, res.getStatus());
    }

    @Test
    void testExtractClientIp_XForwardedForPriority() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        req.setRemoteAddr("10.0.0.1");

        String ip = RateLimitingFilter.extractClientIp(req, true);
        assertEquals("203.0.113.195", ip);
        assertEquals("10.0.0.1", RateLimitingFilter.extractClientIp(req, false));
    }
}
