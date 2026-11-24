package com.wesleyey.toEvent.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.wesleyey.toEvent.exception.InvalidTokenException;
import com.wesleyey.toEvent.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    static final Duration TOKEN_TTL = Duration.ofHours(2);

    @Value("${auth.secret.token}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("toEvent-api")
                    .withSubject(user.getEmail())
                    .withClaim("id", user.getId())
                    .withClaim("name", user.getName())
                    .withExpiresAt(generateExpirationDate())
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("toEvent-api")
                    .build()
                    .verify(token)
                    .getSubject();
        } catch (JWTVerificationException ex) {
            throw new InvalidTokenException("Invalid or expired JWT", ex);
        }
    }


    private Instant generateExpirationDate() {
        // Instant é absoluto (UTC): o fuso horário do servidor não altera a duração do token
        return Instant.now().plus(TOKEN_TTL);
    }
}
