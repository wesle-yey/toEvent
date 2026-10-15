package com.wesleyey.toEvent.services;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.wesleyey.toEvent.models.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
public class TokenService {

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
                    .getSubject(); // Retorna o email do usuário
        } catch (JWTVerificationException exception) {
            return null; // Token inválido ou expirado
        }
    }

    private Instant generateExpirationDate() {
        // Expira em 2 horas no fuso horário do Brasil (-03:00)
        return LocalDateTime.now().plusHours(2).toInstant(ZoneOffset.of("-03:00"));
    }
}
