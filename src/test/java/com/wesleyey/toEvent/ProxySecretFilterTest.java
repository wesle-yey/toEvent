package com.wesleyey.toEvent;

import com.wesleyey.toEvent.config.ProxySecretFilter;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProxySecretFilterTest {

    private static final String SECRET = "segredo-de-teste";

    private MockHttpServletResponse run(ProxySecretFilter filter, MockHttpServletRequest request, MockFilterChain chain)
            throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        filter.doFilter(request, response, chain);
        return response;
    }

    @Test
    void allowsApiRequestWithCorrectSecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/login");
        request.addHeader("X-Origin-Secret", SECRET);
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = run(new ProxySecretFilter(SECRET), request, chain);

        assertEquals(200, response.getStatus());
        assertNotNull(chain.getRequest(), "a requisição deve seguir para a aplicação");
    }

    @Test
    void blocksApiRequestWithoutSecret() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        MockHttpServletResponse response = run(new ProxySecretFilter(SECRET),
                new MockHttpServletRequest("POST", "/api/auth/login"), chain);

        assertEquals(403, response.getStatus());
        assertTrue(response.getContentAsString().contains("DIRECT_ACCESS_FORBIDDEN"));
        assertNull(chain.getRequest(), "a requisição não pode chegar à aplicação");
    }

    @Test
    void blocksApiRequestWithWrongSecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/events");
        request.addHeader("X-Origin-Secret", "chute");
        MockFilterChain chain = new MockFilterChain();

        assertEquals(403, run(new ProxySecretFilter(SECRET), request, chain).getStatus());
        assertNull(chain.getRequest());
    }

    @Test
    void leavesHealthCheckOpen() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        run(new ProxySecretFilter(SECRET), new MockHttpServletRequest("GET", "/health"), chain);

        assertNotNull(chain.getRequest());
    }

    @Test
    void isDisabledWhenSecretIsNotConfigured() throws Exception {
        MockFilterChain chain = new MockFilterChain();

        run(new ProxySecretFilter(""), new MockHttpServletRequest("POST", "/api/auth/login"), chain);

        assertNotNull(chain.getRequest());
    }
}
