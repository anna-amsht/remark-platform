package com.remark.platform.service;

import com.remark.platform.client.AuthenticatedUserResponse;
import com.remark.platform.client.RoleCode;
import com.remark.platform.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtProperties jwtProperties = new JwtProperties(
            "remark-platform",
            "test-key",
            Duration.ofMinutes(20),
            Duration.ofDays(7),
            "private.pem",
            "public.pem"
    );

    @Test
    void createAccessTokenBuildsExpectedClaims() {
        AtomicReference<JwtClaimsSet> capturedClaims = new AtomicReference<>();
        JwtEncoder jwtEncoder = parameters -> {
            capturedClaims.set(parameters.getClaims());
            return new Jwt(
                    "encoded-token",
                    Instant.now(),
                    Instant.now().plus(Duration.ofMinutes(20)),
                    java.util.Map.of("alg", "RS256"),
                    java.util.Map.of("sub", "11111111-1111-1111-1111-111111111111")
            );
        };
        JwtService jwtService = new JwtService(jwtEncoder, jwtProperties);

        String token = jwtService.createAccessToken(userWithBranch());

        assertThat(token).isEqualTo("encoded-token");
        JwtClaimsSet claims = capturedClaims.get();
        assertThat(claims.<String>getClaim("iss")).isEqualTo("remark-platform");
        assertThat(claims.getSubject()).isEqualTo("11111111-1111-1111-1111-111111111111");
        assertThat(claims.<String>getClaim("email")).isEqualTo("user@example.com");
        assertThat(claims.<String>getClaim("fullName")).isEqualTo("Test User");
        assertThat(claims.<String>getClaim("position")).isEqualTo("Engineer");
        assertThat(claims.<String>getClaim("branchId")).isEqualTo("22222222-2222-2222-2222-222222222222");
        assertThat(claims.<List<String>>getClaim("roles")).containsExactlyInAnyOrder("APPLICANT", "MASTER");
    }

    @Test
    void createAccessTokenSkipsBranchClaimWhenBranchIsMissing() {
        AtomicReference<JwtClaimsSet> capturedClaims = new AtomicReference<>();
        JwtEncoder jwtEncoder = parameters -> {
            capturedClaims.set(parameters.getClaims());
            return new Jwt(
                    "encoded-token",
                    Instant.now(),
                    Instant.now().plus(Duration.ofMinutes(20)),
                    java.util.Map.of("alg", "RS256"),
                    java.util.Map.of("sub", "11111111-1111-1111-1111-111111111111")
            );
        };
        JwtService jwtService = new JwtService(jwtEncoder, jwtProperties);

        jwtService.createAccessToken(userWithoutBranch());

        assertThat(capturedClaims.get().getClaims()).doesNotContainKey("branchId");
    }

    @Test
    void accessTokenTtlSecondsReturnsConfiguredTtl() {
        JwtService jwtService = new JwtService(parameters -> null, jwtProperties);

        assertThat(jwtService.accessTokenTtlSeconds()).isEqualTo(1200L);
    }

    @Test
    void toUserInfoMapsAuthenticatedUser() {
        var userInfo = JwtService.toUserInfo(userWithBranch());

        assertThat(userInfo.email()).isEqualTo("user@example.com");
        assertThat(userInfo.fullName()).isEqualTo("Test User");
        assertThat(userInfo.roles()).containsExactlyInAnyOrder(RoleCode.APPLICANT, RoleCode.MASTER);
    }

    private static AuthenticatedUserResponse userWithBranch() {
        return new AuthenticatedUserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "user@example.com",
                "Test User",
                "Engineer",
                UUID.fromString("22222222-2222-2222-2222-222222222222"),
                "ACTIVE",
                Set.of(RoleCode.APPLICANT, RoleCode.MASTER)
        );
    }

    private static AuthenticatedUserResponse userWithoutBranch() {
        return new AuthenticatedUserResponse(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "user@example.com",
                "Test User",
                "Engineer",
                null,
                "ACTIVE",
                Set.of(RoleCode.APPLICANT)
        );
    }
}
