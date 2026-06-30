package com.remark.platform.service;

import com.remark.platform.config.JwtProperties;
import com.remark.platform.entity.RefreshToken;
import com.remark.platform.exceptions.InvalidRefreshTokenException;
import com.remark.platform.repository.RefreshTokenRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RefreshTokenServiceTest {

    private final RefreshTokenRepository refreshTokenRepository = mock(RefreshTokenRepository.class);
    private final JwtProperties jwtProperties = new JwtProperties(
            "remark-platform",
            "test-key",
            Duration.ofMinutes(15),
            Duration.ofDays(7),
            "private.pem",
            "public.pem"
    );
    private final RefreshTokenService refreshTokenService = new RefreshTokenService(refreshTokenRepository, jwtProperties);

    @Test
    void createRefreshTokenStoresHashAndReturnsRawToken() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);

        String rawToken = refreshTokenService.createRefreshToken(userId);

        assertThat(rawToken).isNotBlank();
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        RefreshToken savedToken = tokenCaptor.getValue();
        assertThat(savedToken.getUserId()).isEqualTo(userId);
        assertThat(savedToken.getTokenHash()).hasSize(64);
        assertThat(savedToken.getTokenHash()).doesNotContain(rawToken);
        assertThat(savedToken.getExpiresAt()).isAfter(Instant.now().plus(Duration.ofDays(6)));
    }

    @Test
    void validateAndGetUserIdReturnsUserIdForActiveToken() {
        UUID userId = UUID.fromString("11111111-1111-1111-1111-111111111111");
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(userId);
        refreshToken.setExpiresAt(Instant.now().plus(Duration.ofHours(1)));

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(refreshToken));

        UUID actualUserId = refreshTokenService.validateAndGetUserId("raw-token");

        assertThat(actualUserId).isEqualTo(userId);
    }

    @Test
    void validateAndGetUserIdRejectsMissingToken() {
        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validateAndGetUserId("missing-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void validateAndGetUserIdRejectsExpiredToken() {
        RefreshToken refreshToken = new RefreshToken();
        refreshToken.setUserId(UUID.randomUUID());
        refreshToken.setExpiresAt(Instant.now().minus(Duration.ofMinutes(1)));

        when(refreshTokenRepository.findByTokenHashAndRevokedAtIsNull(anyString()))
                .thenReturn(Optional.of(refreshToken));

        assertThatThrownBy(() -> refreshTokenService.validateAndGetUserId("expired-token"))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void revokeRevokesTokenByHash() {
        refreshTokenService.revoke("raw-token");

        verify(refreshTokenRepository).revokeByTokenHash(anyString(), any(Instant.class));
    }
}
