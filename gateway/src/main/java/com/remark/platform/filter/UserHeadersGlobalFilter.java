package com.remark.platform.filter;

import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class UserHeadersGlobalFilter implements GlobalFilter, Ordered {

    public static final String USER_ID_HEADER = "X-User-Id";
    public static final String BRANCH_ID_HEADER = "X-Branch-Id";
    public static final String USER_ROLES_HEADER = "X-User-Roles";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return ReactiveSecurityContextHolder.getContext()
                .map(securityContext -> securityContext.getAuthentication())
                .filter(authentication -> authentication instanceof JwtAuthenticationToken
                        && authentication.isAuthenticated())
                .cast(JwtAuthenticationToken.class)
                .map(JwtAuthenticationToken::getToken)
                .flatMap(jwt -> {
                    ServerHttpRequest.Builder requestBuilder = exchange.getRequest().mutate()
                            .header(USER_ID_HEADER, jwt.getSubject());

                    String branchId = jwt.getClaimAsString("branchId");
                    if (branchId != null && !branchId.isBlank()) {
                        requestBuilder.header(BRANCH_ID_HEADER, branchId);
                    }

                    String roles = extractRoles(jwt.getClaim("roles"));
                    if (!roles.isBlank()) {
                        requestBuilder.header(USER_ROLES_HEADER, roles);
                    }

                    ServerWebExchange mutatedExchange = exchange.mutate()
                            .request(requestBuilder.build())
                            .build();
                    return chain.filter(mutatedExchange);
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private String extractRoles(Object rolesClaim) {
        if (rolesClaim instanceof Collection<?> collection) {
            return collection.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }
        if (rolesClaim instanceof List<?> list) {
            return list.stream()
                    .map(Object::toString)
                    .collect(Collectors.joining(","));
        }
        if (rolesClaim instanceof String rolesString) {
            return rolesString;
        }
        return "";
    }

    @Override
    public int getOrder() {
        return Ordered.LOWEST_PRECEDENCE;
    }
}
