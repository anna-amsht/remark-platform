package com.remark.platform.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class RsaKeyLoaderTest {

    @TempDir
    Path tempDir;

    @Test
    void loadOrGenerateCreatesKeyPairWhenPathsAreBlank() throws Exception {
        JwtKeyConfig.RsaKeyPair keyPair = RsaKeyLoader.loadOrGenerate(properties(null, " "));

        assertThat(keyPair.privateKey()).isNotNull();
        assertThat(keyPair.publicKey()).isNotNull();
        assertThat(keyPair.privateKey().getAlgorithm()).isEqualTo("RSA");
    }

    @Test
    void loadOrGenerateWritesAndReadsPemFiles() throws Exception {
        Path privateKey = tempDir.resolve("keys/private.pem");
        Path publicKey = tempDir.resolve("keys/public.pem");
        JwtProperties properties = properties(privateKey.toString(), publicKey.toString());

        JwtKeyConfig.RsaKeyPair generated = RsaKeyLoader.loadOrGenerate(properties);
        JwtKeyConfig.RsaKeyPair loaded = RsaKeyLoader.loadOrGenerate(properties);

        assertThat(Files.exists(privateKey)).isTrue();
        assertThat(Files.exists(publicKey)).isTrue();
        assertThat(loaded.privateKey().getEncoded()).isEqualTo(generated.privateKey().getEncoded());
        assertThat(loaded.publicKey().getEncoded()).isEqualTo(generated.publicKey().getEncoded());
    }

    private static JwtProperties properties(String privateKeyPath, String publicKeyPath) {
        return new JwtProperties(
                "remark-platform",
                "key-id",
                Duration.ofMinutes(15),
                Duration.ofDays(7),
                privateKeyPath,
                publicKeyPath
        );
    }
}
