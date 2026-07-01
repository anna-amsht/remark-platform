package com.remark.platform.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class UserHeadersGlobalFilterTest {

    private final UserHeadersGlobalFilter filter = new UserHeadersGlobalFilter();

    @Test
    void addsUserHeadersFromAuthenticatedJwt() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/remarks").build());
        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();
        GatewayFilterChain chain = nextExchange -> {
            filteredExchange.set(nextExchange);
            return reactor.core.publisher.Mono.empty();
        };
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt(Map.of(
                        "sub", "user-1",
                        "branchId", "branch-1",
                        "roles", List.of("APPLICANT", "MASTER")
                )),
                List.of(new SimpleGrantedAuthority("ROLE_APPLICANT"))
        );

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        ServerHttpRequest request = filteredExchange.get().getRequest();
        assertThat(request.getHeaders().getFirst(UserHeadersGlobalFilter.USER_ID_HEADER)).isEqualTo("user-1");
        assertThat(request.getHeaders().getFirst(UserHeadersGlobalFilter.BRANCH_ID_HEADER)).isEqualTo("branch-1");
        assertThat(request.getHeaders().getFirst(UserHeadersGlobalFilter.USER_ROLES_HEADER)).isEqualTo("APPLICANT,MASTER");
    }

    @Test
    void leavesRequestUnchangedWhenAuthenticationIsMissing() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/remarks").build());
        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();
        GatewayFilterChain chain = nextExchange -> {
            filteredExchange.set(nextExchange);
            return reactor.core.publisher.Mono.empty();
        };

        filter.filter(exchange, chain).block();

        assertThat(filteredExchange.get()).isSameAs(exchange);
    }

    @Test
    void keepsStringRolesClaimAsHeaderValue() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/remarks").build());
        AtomicReference<ServerWebExchange> filteredExchange = new AtomicReference<>();
        GatewayFilterChain chain = nextExchange -> {
            filteredExchange.set(nextExchange);
            return reactor.core.publisher.Mono.empty();
        };
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(
                jwt(Map.of("sub", "user-1", "roles", "ADMIN")),
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN"))
        );

        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertThat(filteredExchange.get().getRequest().getHeaders()
                .getFirst(UserHeadersGlobalFilter.USER_ROLES_HEADER)).isEqualTo("ADMIN");
        assertThat(filteredExchange.get().getRequest().getHeaders()
                .containsKey(UserHeadersGlobalFilter.BRANCH_ID_HEADER)).isFalse();
    }

    @Test
    void exposesLowestPrecedenceOrder() {
        assertThat(filter.getOrder()).isEqualTo(org.springframework.core.Ordered.LOWEST_PRECEDENCE);
    }

    private static Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
                "token",
                Instant.parse("2026-01-01T00:00:00Z"),
                Instant.parse("2026-01-01T01:00:00Z"),
                Map.of("alg", "none"),
                claims
        );
    }
}
