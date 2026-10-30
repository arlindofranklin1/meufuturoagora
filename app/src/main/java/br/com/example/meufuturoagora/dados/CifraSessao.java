package br.com.example.meufuturoagora.dados;

import java.security.GeneralSecurityException;

// Separada do armazenamento para os testes JVM trocarem o Android Keystore
// (que só existe no aparelho) por uma cifra de software
public interface CifraSessao {

    byte[] cifrar(byte[] texto) throws GeneralSecurityException;

    byte[] decifrar(byte[] cifrado) throws GeneralSecurityException;
}
