package com.remark.platform.controller;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import org.junit.jupiter.api.Test;

import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;

import static org.assertj.core.api.Assertions.assertThat;

class JwksControllerTest {

    @Test
    void jwksReturnsPublicKeySet() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var keyPair = generator.generateKeyPair();
        RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                .keyID("key-id")
                .build();
        JwksController controller = new JwksController(new JWKSet(rsaKey));

        var response = controller.jwks();

        assertThat(response).containsKey("keys");
        assertThat(response.toString()).contains("key-id");
    }
}
