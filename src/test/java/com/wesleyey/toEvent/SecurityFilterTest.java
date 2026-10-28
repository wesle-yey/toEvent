package com.wesleyey.toEvent;

import com.wesleyey.toEvent.config.SecurityFilter;
import com.wesleyey.toEvent.repositories.UserRepository;
import com.wesleyey.toEvent.services.TokenService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SecurityFilterTest {

    @Mock
    private TokenService tokenService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private SecurityFilter securityFilter;

    private boolean invokeShouldNotFilter(MockHttpServletRequest request) throws Exception {
        Method method = SecurityFilter.class.getDeclaredMethod("shouldNotFilter", jakarta.servlet.http.HttpServletRequest.class);
        method.setAccessible(true);
        return (boolean) method.invoke(securityFilter, request);
    }

    @Test
    @DisplayName("Preflight OPTIONS deve ser ignorado pelo filtro de autenticação")
    void shouldNotFilterOptionsRequest() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/events");
        assertTrue(invokeShouldNotFilter(request));
    }

    @Test
    @DisplayName("Swagger endpoints devem exigir autenticação quando swaggerEnabled = false")
    void shouldFilterSwaggerWhenDisabled() throws Exception {
        ReflectionTestUtils.setField(securityFilter, "swaggerEnabled", false);

        MockHttpServletRequest requestDocs = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletRequest requestUi = new MockHttpServletRequest("GET", "/swagger-ui/index.html");

        assertFalse(invokeShouldNotFilter(requestDocs));
        assertFalse(invokeShouldNotFilter(requestUi));
    }

    @Test
    @DisplayName("Swagger endpoints não devem exigir autenticação quando swaggerEnabled = true")
    void shouldNotFilterSwaggerWhenEnabled() throws Exception {
        ReflectionTestUtils.setField(securityFilter, "swaggerEnabled", true);

        MockHttpServletRequest requestDocs = new MockHttpServletRequest("GET", "/v3/api-docs");
        MockHttpServletRequest requestUi = new MockHttpServletRequest("GET", "/swagger-ui/index.html");

        assertTrue(invokeShouldNotFilter(requestDocs));
        assertTrue(invokeShouldNotFilter(requestUi));
    }

    @Test
    @DisplayName("Endpoints públicos de login e registro devem ser ignorados pelo filtro")
    void shouldNotFilterPublicAuthEndpoints() throws Exception {
        MockHttpServletRequest requestLogin = new MockHttpServletRequest("POST", "/api/auth/login");
        MockHttpServletRequest requestRegister = new MockHttpServletRequest("POST", "/api/auth/register");

        assertTrue(invokeShouldNotFilter(requestLogin));
        assertTrue(invokeShouldNotFilter(requestRegister));
    }

    @Test
    @DisplayName("Endpoints privados como /api/events devem passar pelo filtro de autenticação")
    void shouldFilterPrivateEndpoints() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        assertFalse(invokeShouldNotFilter(request));
    }
}
