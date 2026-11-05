package com.wesleyey.toEvent;

import com.wesleyey.toEvent.services.AuthCookieService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseCookie;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCookieServiceTest {

    @Test
    void createsHttpOnlyCookieWithConfiguredCrossSiteAttributes() {
        AuthCookieService service = new AuthCookieService();
        ReflectionTestUtils.setField(service, "cookieName", "toEventAuth");
        ReflectionTestUtils.setField(service, "secure", true);
        ReflectionTestUtils.setField(service, "sameSite", "None");

        ResponseCookie cookie = service.create("jwt");

        assertEquals("jwt", cookie.getValue());
        assertEquals(Duration.ofHours(2), cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
        assertTrue(cookie.isSecure());
        assertTrue(cookie.toString().contains("SameSite=None"));
    }

    @Test
    void clearsCookieUsingTheSameAttributes() {
        AuthCookieService service = new AuthCookieService();
        ReflectionTestUtils.setField(service, "cookieName", "toEventAuth");
        ReflectionTestUtils.setField(service, "secure", false);
        ReflectionTestUtils.setField(service, "sameSite", "Lax");

        ResponseCookie cookie = service.clear();

        assertEquals("", cookie.getValue());
        assertEquals(Duration.ZERO, cookie.getMaxAge());
        assertTrue(cookie.isHttpOnly());
    }
}
