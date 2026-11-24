package com.wesleyey.toEvent;

import com.auth0.jwt.JWT;
import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.services.TokenService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.Instant;
import java.util.TimeZone;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TokenServiceTest {

    private final TimeZone originalTimeZone = TimeZone.getDefault();

    @AfterEach
    void restoreTimeZone() {
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    void tokenExpiresInTwoHoursRegardlessOfServerTimeZone() {
        // Render roda em UTC; antes da correção o token durava 5h nesse fuso
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        assertExpiresInTwoHours();

        TimeZone.setDefault(TimeZone.getTimeZone("America/Sao_Paulo"));
        assertExpiresInTwoHours();
    }

    @Test
    void generatedTokenIsValidAndCarriesEmailAsSubject() {
        TokenService service = newService();
        String token = service.generateToken(newUser());

        assertEquals("ana@example.com", service.validateToken(token));
    }

    private void assertExpiresInTwoHours() {
        Instant before = Instant.now();
        String token = newService().generateToken(newUser());
        Instant expiresAt = JWT.decode(token).getExpiresAtAsInstant();

        Duration ttl = Duration.between(before, expiresAt);
        assertTrue(ttl.compareTo(Duration.ofMinutes(119)) > 0 && ttl.compareTo(Duration.ofMinutes(121)) < 0,
                "TTL esperado ~2h, obtido " + ttl);
    }

    private static TokenService newService() {
        TokenService service = new TokenService();
        ReflectionTestUtils.setField(service, "secret", "test-secret-with-enough-length-for-hmac");
        return service;
    }

    private static User newUser() {
        User user = new User("Ana", "ana@example.com", "hash");
        user.setId("user-1");
        return user;
    }
}
