package com.remark.platform.service;

import com.remark.platform.client.AuthenticatedUserResponse;
import com.remark.platform.config.JwtProperties;
import com.remark.platform.dto.UserInfo;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties jwtProperties) {
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    public String createAccessToken(AuthenticatedUserResponse user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.accessTokenTtl());

        List<String> roles = user.roles().stream()
                .map(Enum::name)
                .toList();

        JwtClaimsSet.Builder claimsBuilder = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.userId().toString())
                .claim("email", user.email())
                .claim("fullName", user.fullName())
                .claim("position", user.position())
                .claim("roles", roles);

        if (user.branchId() != null) {
            claimsBuilder.claim("branchId", user.branchId().toString());
        }

        return jwtEncoder.encode(JwtEncoderParameters.from(claimsBuilder.build()))
                .getTokenValue();
    }

    public long accessTokenTtlSeconds() {
        return jwtProperties.accessTokenTtl().toSeconds();
    }

    public static UserInfo toUserInfo(AuthenticatedUserResponse user) {
        return new UserInfo(
                user.userId(),
                user.email(),
                user.fullName(),
                user.position(),
                user.branchId(),
                user.roles()
        );
    }
}
