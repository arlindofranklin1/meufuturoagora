package br.com.example.meufuturoagora.dados;

import java.util.List;

// Estrutura da escola: turmas, ano letivo e bimestres
public interface EscolaRepositorio {

    // Admin e professor: todas; aluno: só a própria
    List<Turma> turmas() throws ErroApi;

    // Admin. Nome repetido (sem diferenciar maiúsculas): ErroApi com jaExiste()
    Turma criarTurma(String nome, Integer serie) throws ErroApi;

    Turma atualizarTurma(String id, String nome, Integer serie, boolean ativo) throws ErroApi;

    // Alunos e disciplinas dela ficam sem turma
    void excluirTurma(String id) throws ErroApi;

    // null se o admin ainda não definiu nenhum
    AnoLetivo anoAtual() throws ErroApi;

    List<AnoLetivo> anos() throws ErroApi;

    List<Bimestre> bimestres(int ano) throws ErroApi;

    // Admin. Sem senha (decisão N5); o anterior fica encerrado.
    void definirAnoLetivo(int ano) throws ErroApi;

    // Admin. Substitui todos os bimestres do ano; os ausentes ficam indefinidos.
    void definirBimestres(int ano, List<Bimestre> bimestres) throws ErroApi;
}
