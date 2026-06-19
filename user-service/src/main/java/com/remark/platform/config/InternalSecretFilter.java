package com.remark.platform.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class InternalSecretFilter extends OncePerRequestFilter {

    public static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final String internalSecret;

    public InternalSecretFilter(@Value("${app.internal.secret}") String internalSecret) {
        this.internalSecret = internalSecret;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().startsWith("/internal");
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String providedSecret = request.getHeader(INTERNAL_SECRET_HEADER);
        if (providedSecret == null || !internalSecret.equals(providedSecret)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Invalid internal secret");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
