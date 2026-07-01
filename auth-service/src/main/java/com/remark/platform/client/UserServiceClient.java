package com.remark.platform.client;

import com.remark.platform.exceptions.InvalidCredentialsException;
import com.remark.platform.exceptions.InvalidRefreshTokenException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class UserServiceClient {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";

    private final RestClient userServiceRestClient;
    private final String internalSecret;

    public UserServiceClient(
            RestClient userServiceRestClient,
            @Value("${app.internal.secret}") String internalSecret
    ) {
        this.userServiceRestClient = userServiceRestClient;
        this.internalSecret = internalSecret;
    }

    public AuthenticatedUserResponse authenticate(String email, String password) {
        try {
            AuthenticatedUserResponse response = userServiceRestClient.post()
                    .uri("/internal/users/authenticate")
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .body(new AuthenticateRequest(email, password))
                    .retrieve()
                    .body(AuthenticatedUserResponse.class);
            if (response == null) {
                throw new InvalidCredentialsException();
            }
            return response;
        } catch (HttpClientErrorException.Unauthorized | HttpClientErrorException.Forbidden ex) {
            throw new InvalidCredentialsException();
        }
    }

    public AuthenticatedUserResponse getActiveUserById(java.util.UUID userId) {
        try {
            AuthenticatedUserResponse response = userServiceRestClient.get()
                    .uri("/internal/users/{userId}", userId)
                    .header(INTERNAL_SECRET_HEADER, internalSecret)
                    .retrieve()
                    .body(AuthenticatedUserResponse.class);
            if (response == null) {
                throw new InvalidRefreshTokenException();
            }
            return response;
        } catch (HttpClientErrorException.Unauthorized
                 | HttpClientErrorException.Forbidden
                 | HttpClientErrorException.NotFound ex) {
            throw new InvalidRefreshTokenException();
        }
    }
}
