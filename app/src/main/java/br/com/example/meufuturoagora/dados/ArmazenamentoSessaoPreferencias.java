package br.com.example.meufuturoagora.dados;

import android.content.SharedPreferences;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Base64;

// Token de renovação cifrado (CifraSessao) dentro de um SharedPreferences.
// Sessão ilegível (outro aparelho, chave apagada, arquivo corrompido) é tratada
// como "sem sessão": o usuário só precisa entrar de novo.
public class ArmazenamentoSessaoPreferencias implements ArmazenamentoSessao {

    static final String CHAVE = "sessao_cifrada";

    private final SharedPreferences preferencias;
    private final CifraSessao cifra;

    public ArmazenamentoSessaoPreferencias(SharedPreferences preferencias, CifraSessao cifra) {
        this.preferencias = preferencias;
        this.cifra = cifra;
    }

    @Override
    public void salvar(String tokenDeRenovacao) {

        try {
            byte[] cifrado = cifra.cifrar(tokenDeRenovacao.getBytes(StandardCharsets.UTF_8));

            preferencias.edit()
                    .putString(CHAVE, Base64.getEncoder().encodeToString(cifrado))
                    .apply();

        } catch (GeneralSecurityException e) {
            // Sem Keystore não guarda nada: a sessão vale só enquanto o app estiver aberto
            apagar();
        }
    }

    @Override
    public String ler() {

        String salvo = preferencias.getString(CHAVE, null);

        if (salvo == null) {
            return null;
        }

        try {
            byte[] texto = cifra.decifrar(Base64.getDecoder().decode(salvo));
            return new String(texto, StandardCharsets.UTF_8);

        } catch (GeneralSecurityException | IllegalArgumentException e) {
            apagar();
            return null;
        }
    }

    @Override
    public void apagar() {
        preferencias.edit().remove(CHAVE).apply();
    }
}
