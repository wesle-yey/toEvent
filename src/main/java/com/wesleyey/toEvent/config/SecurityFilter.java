package com.wesleyey.toEvent.config;

import com.wesleyey.toEvent.exception.InvalidTokenException;
import com.wesleyey.toEvent.repositories.UserRepository;
import com.wesleyey.toEvent.services.TokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;

@Component
public class SecurityFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(SecurityFilter.class);

    private final TokenService tokenService;
    private final UserRepository userRepository;

    @Value("${springdoc.swagger-ui.enabled:false}")
    private boolean swaggerEnabled;

    @Value("${auth.cookie.name:toEventAuth}")
    private String authCookieName;

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    private String allowedOrigins;

    public SecurityFilter(TokenService tokenService, UserRepository userRepository) {
        this.tokenService = tokenService;
        this.userRepository = userRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Preflight CORS nunca deve ser bloqueado pela checagem de token
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String path = request.getServletPath();
        if (path == null || path.isEmpty()) {
            path = request.getRequestURI();
        }
        if (path == null) {
            path = "";
        }
        boolean isPublic = path.equals("/login")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/register")
                || path.equals("/api/auth/logout")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.equals("/error");

        if (swaggerEnabled && (path.startsWith("/v3/api-docs") || path.startsWith("/swagger-ui"))) {
            return true;
        }

        return isPublic;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String token = recoverToken(request);
        if (token == null) {
            logger.warn("Authentication rejected: missing token for {} {}", request.getMethod(), request.getServletPath());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Missing authentication token");
            return;
        }

        if (!hasBearerToken(request) && !isAllowedCookieRequest(request)) {
            logger.warn("Authentication rejected: untrusted origin for {} {}", request.getMethod(), request.getServletPath());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid request origin");
            return;
        }

        try {
            String email = tokenService.validateToken(token);
            UserDetails user = userRepository.findByEmail(email).orElse(null);
            if (user == null) {
                logger.warn("Authentication rejected: token subject not found for {} {}", request.getMethod(), request.getServletPath());
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
                return;
            }
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(user, null,
                    user.getAuthorities());
            SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (InvalidTokenException ex) {
            logger.warn("Authentication rejected: invalid or expired token for {} {}", request.getMethod(), request.getServletPath());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired JWT");
            return;
        }
        filterChain.doFilter(request, response);
    }

    private String recoverToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring("Bearer ".length());
        }
        if (request.getCookies() != null) {
            return Arrays.stream(request.getCookies())
                    .filter(cookie -> authCookieName.equals(cookie.getName()))
                    .map(jakarta.servlet.http.Cookie::getValue)
                    .findFirst()
                    .orElse(null);
        }
        return null;
    }

    private boolean hasBearerToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        return authHeader != null && authHeader.startsWith("Bearer ");
    }

    private boolean isAllowedCookieRequest(HttpServletRequest request) {
        String method = request.getMethod();
        if ("GET".equalsIgnoreCase(method) || "HEAD".equalsIgnoreCase(method)) {
            return true;
        }
        String origin = request.getHeader("Origin");
        return origin != null && Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .anyMatch(origin::equals);
    }
}
