package br.com.example.meufuturoagora.dados;

import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.KeyStore;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// AES-GCM com a chave no Android Keystore: a chave nunca sai do hardware do
// aparelho, então o arquivo copiado para outro lugar não serve para nada.
// (EncryptedSharedPreferences foi descontinuado; ver o plano, Seção 4.2.)
public class CifraKeystore implements CifraSessao {

    private static final String PROVEDOR = "AndroidKeyStore";
    private static final String APELIDO = "meufuturoagora_sessao";
    private static final String TRANSFORMACAO = "AES/GCM/NoPadding";
    private static final int TAMANHO_TAG = 128;

    @Override
    public byte[] cifrar(byte[] texto) throws GeneralSecurityException {

        Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
        cifra.init(Cipher.ENCRYPT_MODE, chave());

        byte[] iv = cifra.getIV();
        byte[] cifrado = cifra.doFinal(texto);

        // Formato: tamanho do IV (1 byte) + IV + texto cifrado
        return ByteBuffer.allocate(1 + iv.length + cifrado.length)
                .put((byte) iv.length)
                .put(iv)
                .put(cifrado)
                .array();
    }

    @Override
    public byte[] decifrar(byte[] dados) throws GeneralSecurityException {

        if (dados.length < 2) {
            throw new GeneralSecurityException("Dados cifrados incompletos");
        }

        ByteBuffer buffer = ByteBuffer.wrap(dados);
        int tamanhoIv = buffer.get() & 0xFF;

        if (tamanhoIv == 0 || tamanhoIv > buffer.remaining()) {
            throw new GeneralSecurityException("IV inválido");
        }

        byte[] iv = new byte[tamanhoIv];
        buffer.get(iv);
        byte[] cifrado = new byte[buffer.remaining()];
        buffer.get(cifrado);

        Cipher cifra = Cipher.getInstance(TRANSFORMACAO);
        cifra.init(Cipher.DECRYPT_MODE, chave(), new GCMParameterSpec(TAMANHO_TAG, iv));

        return cifra.doFinal(cifrado);
    }

    private SecretKey chave() throws GeneralSecurityException {

        try {
            KeyStore keyStore = KeyStore.getInstance(PROVEDOR);
            keyStore.load(null);

            KeyStore.Entry entrada = keyStore.getEntry(APELIDO, null);

            if (entrada instanceof KeyStore.SecretKeyEntry) {
                return ((KeyStore.SecretKeyEntry) entrada).getSecretKey();
            }

            KeyGenerator gerador = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, PROVEDOR);
            gerador.init(new KeyGenParameterSpec.Builder(
                    APELIDO, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                    .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                    .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                    .setKeySize(256)
                    .build());

            return gerador.generateKey();

        } catch (java.io.IOException e) {
            throw new GeneralSecurityException("Keystore indisponível", e);
        }
    }
}
