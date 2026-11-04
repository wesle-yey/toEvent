package com.wesleyey.toEvent.config;

import com.wesleyey.toEvent.models.User;
import com.wesleyey.toEvent.services.RateLimitingService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitingService rateLimitingService;

    public RateLimitingFilter(RateLimitingService rateLimitingService) {
        this.rateLimitingService = rateLimitingService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflight OPTIONS nunca é limitado por rate-limit
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = resolvePath(request);
        // Ignorar assets e docs
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui")
                || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = resolvePath(request);
        String method = request.getMethod();
        ConsumptionProbe probe = null;

        if ("/api/auth/register".equals(path) && "POST".equalsIgnoreCase(method)) {
            String clientIp = extractClientIp(request);
            probe = rateLimitingService.tryConsumeRegister(clientIp);
        } else if ("/api/auth/login".equals(path) && "POST".equalsIgnoreCase(method)) {
            String clientIp = extractClientIp(request);
            probe = rateLimitingService.tryConsumeLogin(clientIp);
        } else if (path.startsWith("/api/events")) {
            String identity = resolveUserIdentity(request);
            if ("/api/events".equals(path) && "POST".equalsIgnoreCase(method)) {
                probe = rateLimitingService.tryConsumeEventCreate(identity);
            } else {
                probe = rateLimitingService.tryConsumeEventGeneral(identity);
            }
        }

        if (probe != null && !probe.isConsumed()) {
            long nanosToWait = probe.getNanosToWaitForRefill();
            long retryAfterSeconds = Math.max(1, TimeUnit.NANOSECONDS.toSeconds(nanosToWait));

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));
            response.getWriter().write(String.format(
                    "{\"error\":\"TOO_MANY_REQUESTS\",\"message\":\"Muitas tentativas em pouco tempo. Aguarde antes de tentar novamente.\",\"retryAfterSeconds\":%d}",
                    retryAfterSeconds
            ));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String resolvePath(HttpServletRequest request) {
        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        return path != null ? path : "";
    }

    public static String extractClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            // Em proxies como Render, o primeiro IP é o cliente real
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }
        return request.getRemoteAddr() != null ? request.getRemoteAddr() : "unknown-ip";
    }

    private String resolveUserIdentity(HttpServletRequest request) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() && !(authentication instanceof AnonymousAuthenticationToken)) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof User user && user.getId() != null) {
                return user.getId();
            }
            if (authentication.getName() != null && !authentication.getName().isBlank()) {
                return authentication.getName();
            }
        }
        return extractClientIp(request);
    }
}
