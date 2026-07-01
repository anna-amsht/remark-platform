package com.remark.platform.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

final class RsaKeyLoader {

    private RsaKeyLoader() {
    }

    static JwtKeyConfig.RsaKeyPair loadOrGenerate(JwtProperties jwtProperties)
            throws NoSuchAlgorithmException, IOException {
        String privateKeyPath = jwtProperties.privateKeyPath();
        String publicKeyPath = jwtProperties.publicKeyPath();

        if (isBlank(privateKeyPath) || isBlank(publicKeyPath)) {
            return generate();
        }

        Path privatePath = Path.of(privateKeyPath);
        Path publicPath = Path.of(publicKeyPath);

        if (Files.exists(privatePath) && Files.exists(publicPath)) {
            return new JwtKeyConfig.RsaKeyPair(
                    readPublicKey(publicPath),
                    readPrivateKey(privatePath)
            );
        }

        JwtKeyConfig.RsaKeyPair generated = generate();
        createParentDirectories(privatePath);
        createParentDirectories(publicPath);
        writePrivateKey(privatePath, generated.privateKey());
        writePublicKey(publicPath, generated.publicKey());
        return generated;
    }

    private static void createParentDirectories(Path path) throws IOException {
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
    }

    private static JwtKeyConfig.RsaKeyPair generate() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair keyPair = generator.generateKeyPair();
        return new JwtKeyConfig.RsaKeyPair(
                (RSAPublicKey) keyPair.getPublic(),
                (RSAPrivateKey) keyPair.getPrivate()
        );
    }

    private static RSAPrivateKey readPrivateKey(Path path) throws IOException {
        try {
            String pem = Files.readString(path)
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decoded);
            return (RSAPrivateKey) KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (GeneralSecurityException ex) {
            throw new IOException("Failed to read private key from " + path, ex);
        }
    }

    private static RSAPublicKey readPublicKey(Path path) throws IOException {
        try {
            String pem = Files.readString(path)
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] decoded = Base64.getDecoder().decode(pem);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decoded);
            return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (GeneralSecurityException ex) {
            throw new IOException("Failed to read public key from " + path, ex);
        }
    }

    private static void writePrivateKey(Path path, RSAPrivateKey privateKey) throws IOException {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(privateKey.getEncoded());
        Files.writeString(path, "-----BEGIN PRIVATE KEY-----\n" + encoded + "\n-----END PRIVATE KEY-----\n");
    }

    private static void writePublicKey(Path path, RSAPublicKey publicKey) throws IOException {
        String encoded = Base64.getMimeEncoder(64, "\n".getBytes())
                .encodeToString(publicKey.getEncoded());
        Files.writeString(path, "-----BEGIN PUBLIC KEY-----\n" + encoded + "\n-----END PUBLIC KEY-----\n");
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
