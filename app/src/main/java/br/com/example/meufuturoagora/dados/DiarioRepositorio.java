package br.com.example.meufuturoagora.dados;

import java.util.List;

// Diário do aluno: só ele lê e escreve (decisão D8). Sem tempo real (N2):
// a tela recarrega ao abrir, ao voltar e ao puxar para atualizar.
public interface DiarioRepositorio {

    // Mais recentes primeiro (DiarioAlunoActivity.java:174)
    List<EntradaDiario> entradas() throws ErroApi;

    EntradaDiario escrever(String texto) throws ErroApi;

    void editar(String id, String texto) throws ErroApi;

    void apagar(String id) throws ErroApi;
}
