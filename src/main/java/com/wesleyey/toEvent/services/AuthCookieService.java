package com.wesleyey.toEvent.services;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

@Service
public class AuthCookieService {

    private static final Duration TOKEN_TTL = Duration.ofHours(2);
    public static final String VERIFICATION_BROWSER_COOKIE = "toEventVerificationBrowser";
    private static final String VERIFICATION_COOKIE_PATH = "/api/auth";
    private static final Duration VERIFICATION_BROWSER_TTL = Duration.ofHours(24);
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

    /** Código aleatório que marca o navegador que pediu o link de confirmação (só ele entra automaticamente). */
    public ResponseCookie createVerificationBrowser(String browserCode) {
        return cookie(VERIFICATION_BROWSER_COOKIE, browserCode, VERIFICATION_COOKIE_PATH)
                .maxAge(VERIFICATION_BROWSER_TTL).build();
    }

    public ResponseCookie clearVerificationBrowser() {
        return cookie(VERIFICATION_BROWSER_COOKIE, "", VERIFICATION_COOKIE_PATH).maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String value) {
        return cookie(cookieName, value, "/");
    }

    private ResponseCookie.ResponseCookieBuilder cookie(String name, String value, String path) {
        if (!SAME_SITE_VALUES.contains(sameSite)) {
            throw new IllegalStateException("AUTH_COOKIE_SAME_SITE must be Lax, Strict, or None");
        }
        if ("None".equals(sameSite) && !secure) {
            throw new IllegalStateException("AUTH_COOKIE_SECURE must be true when SameSite is None");
        }

        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .path(path)
                .sameSite(sameSite);
    }
}
