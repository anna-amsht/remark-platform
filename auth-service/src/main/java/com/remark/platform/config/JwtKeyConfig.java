package com.remark.platform.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;

@Configuration
@EnableConfigurationProperties({JwtProperties.class, CookieProperties.class})
public class JwtKeyConfig {

    @Bean
    RsaKeyPair rsaKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new RsaKeyPair(
                (RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate()
        );
    }

    @Bean
    JwtEncoder jwtEncoder(RsaKeyPair rsaKeyPair, JwtProperties jwtProperties) {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeyPair.publicKey())
                .privateKey(rsaKeyPair.privateKey())
                .keyID(jwtProperties.keyId())
                .build();
        JWKSource<SecurityContext> jwkSource = new ImmutableJWKSet<>(new JWKSet(rsaKey));
        return new NimbusJwtEncoder(jwkSource);
    }

    @Bean
    JWKSet jwkSet(RsaKeyPair rsaKeyPair, JwtProperties jwtProperties) {
        RSAKey rsaKey = new RSAKey.Builder(rsaKeyPair.publicKey())
                .keyID(jwtProperties.keyId())
                .build();
        return new JWKSet(rsaKey);
    }

    public record RsaKeyPair(RSAPublicKey publicKey, RSAPrivateKey privateKey) {
    }
}
