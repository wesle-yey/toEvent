package com.wesleyey.toEvent.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * Só aceita chamadas à API que passaram pelo proxy da Vercel.
 * A Vercel acrescenta o cabeçalho {@value #HEADER} com um segredo que só ela e a API conhecem;
 * quem chama o Render direto não tem o segredo e recebe 403 antes de chegar ao rate limit.
 * Sem o segredo configurado (desenvolvimento local), o filtro fica desligado.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ProxySecretFilter extends OncePerRequestFilter {

    static final String HEADER = "X-Origin-Secret";
    private static final Logger logger = LoggerFactory.getLogger(ProxySecretFilter.class);

    private final byte[] expectedSecret;

    public ProxySecretFilter(@Value("${proxy.shared-secret:}") String sharedSecret) {
        this.expectedSecret = sharedSecret == null || sharedSecret.isBlank()
                ? null
                : sharedSecret.trim().getBytes(StandardCharsets.UTF_8);
        if (expectedSecret == null) {
            logger.warn("PROXY_SHARED_SECRET não configurado: a API aceita chamadas diretas (use só em desenvolvimento).");
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // /health e / ficam abertos para o health check do Render
        return expectedSecret == null || !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String provided = request.getHeader(HEADER);
        if (provided != null && MessageDigest.isEqual(expectedSecret, provided.getBytes(StandardCharsets.UTF_8))) {
            chain.doFilter(request, response);
            return;
        }
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write("{\"error\":\"DIRECT_ACCESS_FORBIDDEN\",\"message\":\"Acesse a API pelo site do toEvent.\"}");
    }
}
