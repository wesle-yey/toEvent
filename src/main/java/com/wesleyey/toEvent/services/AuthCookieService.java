package com.wesleyey.toEvent.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class AuthCookieService {

    private static final Duration TOKEN_TTL = Duration.ofHours(2);
    private static final Set<String> SAME_SITE_VALUES = Set.of("Lax", "Strict", "None");

    @Value("${auth.cookie.name:toEventAuth}")
    private String cookieName;

    @Value("${auth.cookie.secure:false}")
    private boolean secure;

    @Value("${auth.cookie.same-site:Lax}")
    private String sameSite;

    public ResponseCookie create(String token) {
        return cookie(token).maxAge(TOKEN_TTL).build();
    }

    public ResponseCookie clear() {
        return cookie("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        if (!SAME_SITE_VALUES.contains(sameSite)) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE must be Lax, Strict, or None");
        }
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalStateException("AUTH_COOKIE_SECURE must be true when SameSite is None");
        }

        return ResponseCookie.from(cookieName, value)
                .httpOnly(true)
                .secure(secure)
                .path("/")
                .sameSite(sameSite);
    }
}
