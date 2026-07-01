package com.remark.platform.config;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class InternalSecretFilterTest {

    private InternalSecretFilter filter;
    private MockHttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new InternalSecretFilter("expected-secret");
        response = new MockHttpServletResponse();
        filterChain = mock(FilterChain.class);
    }

    @Test
    void allowsInternalRequestWithValidSecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/users/1");
        request.addHeader(InternalSecretFilter.INTERNAL_SECRET_HEADER, "expected-secret");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(200);
    }

    @Test
    void rejectsInternalRequestWithInvalidSecret() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/internal/users/1");
        request.addHeader(InternalSecretFilter.INTERNAL_SECRET_HEADER, "wrong-secret");

        filter.doFilter(request, response, filterChain);

        verify(filterChain, never()).doFilter(request, response);
        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void skipsPublicApiRequests() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/users/me");

        filter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
