package br.com.example.meufuturoagora.dados;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

// Protege o login com Google contra reuso do ID token: o Google recebe o hash
// (GetGoogleIdOption.setNonce) e põe no token; o Supabase recebe o valor cru e
// confere se o hash bate.
public final class Nonce {

    private static final SecureRandom ALEATORIO = new SecureRandom();

    public final String cru;
    public final String hash;

    private Nonce(String cru, String hash) {
        this.cru = cru;
        this.hash = hash;
    }

    public static Nonce gerar() {

        byte[] bytes = new byte[32];
        ALEATORIO.nextBytes(bytes);

        String cru = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        return new Nonce(cru, sha256Hex(cru));
    }

    static String sha256Hex(String texto) {

        try {
            byte[] resumo = MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder(resumo.length * 2);

            for (byte b : resumo) {
                hex.append(String.format("%02x", b));
            }

            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 existe em todo Android
            throw new IllegalStateException(e);
        }
    }
}
