package com.remark.platform.controller;

import com.remark.platform.client.RoleCode;
import com.remark.platform.config.CookieProperties;
import com.remark.platform.config.JwtProperties;
import com.remark.platform.dto.LoginRequest;
import com.remark.platform.dto.LoginResponse;
import com.remark.platform.dto.TokenRefreshResponse;
import com.remark.platform.dto.UserInfo;
import com.remark.platform.exceptions.InvalidRefreshTokenException;
import com.remark.platform.service.AuthService;
import com.remark.platform.service.AuthSession;
import com.remark.platform.service.TokenPair;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {

    private final AuthService authService = mock(AuthService.class);
    private final CookieProperties cookieProperties = new CookieProperties(true);
    private final JwtProperties jwtProperties = new JwtProperties(
            "remark-platform",
            "key-id",
            Duration.ofMinutes(15),
            Duration.ofDays(7),
            null,
            null
    );
    private final AuthController controller = new AuthController(authService, cookieProperties, jwtProperties);

    @Test
    void loginReturnsBodyAndRefreshCookie() {
        LoginRequest request = new LoginRequest("user@example.com", "password");
        LoginResponse loginResponse = loginResponse();
        when(authService.login(request)).thenReturn(new AuthSession(loginResponse, "refresh-token"));

        var response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(loginResponse);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=refresh-token")
                .contains("HttpOnly")
                .contains("Secure")
                .contains("Max-Age=604800");
    }

    @Test
    void refreshRejectsMissingCookie() {
        assertThatThrownBy(() -> controller.refresh(" "))
                .isInstanceOf(InvalidRefreshTokenException.class);
    }

    @Test
    void refreshReturnsNewAccessTokenAndCookie() {
        TokenRefreshResponse tokenResponse = new TokenRefreshResponse("new-access", "Bearer", 900);
        when(authService.refresh("old-refresh")).thenReturn(new TokenPair(tokenResponse, "new-refresh"));

        var response = controller.refresh("old-refresh");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(tokenResponse);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE)).contains("refreshToken=new-refresh");
    }

    @Test
    void logoutRevokesTokenAndClearsCookie() {
        var response = controller.logout("refresh-token");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.SET_COOKIE))
                .contains("refreshToken=")
                .contains("Max-Age=0");
        verify(authService).logout("refresh-token");
    }

    @Test
    void refreshCookieSupportCreatesNonSecureCookieWhenConfigured() {
        var cookie = RefreshCookieSupport.createRefreshCookie(
                "refresh-token",
                new CookieProperties(false),
                jwtProperties
        );

        assertThat(cookie.toString()).contains("refreshToken=refresh-token");
        assertThat(cookie.isSecure()).isFalse();
        assertThat(cookie.getPath()).isEqualTo(RefreshCookieSupport.AUTH_PATH);
    }

    private static LoginResponse loginResponse() {
        return new LoginResponse(
                "access-token",
                "Bearer",
                900,
                new UserInfo(
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        "user@example.com",
                        "User",
                        "Engineer",
                        UUID.fromString("22222222-2222-2222-2222-222222222222"),
                        Set.of(RoleCode.APPLICANT)
                )
        );
    }
}
