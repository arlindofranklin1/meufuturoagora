package br.com.example.meufuturoagora.dados;

import java.util.List;

// Histórico do administrador por ano letivo e bimestre. O servidor calcula e
// só o admin chama. bimestre 0 = ano inteiro.
public interface HistoricoRepositorio {

    // Anos com algum registro, do mais novo para o mais antigo
    List<Integer> anos() throws ErroApi;

    // Alunos com entrega ou falta no período, do maior para o menor número de pontos
    List<HistoricoAluno> alunos(int ano, int bimestre) throws ErroApi;

    // Mais recentes primeiro
    List<HistoricoTrilha> trilhas(int ano, int bimestre) throws ErroApi;

    // Mais recentes primeiro
    List<HistoricoEntrega> entregas(int ano, int bimestre) throws ErroApi;
}
