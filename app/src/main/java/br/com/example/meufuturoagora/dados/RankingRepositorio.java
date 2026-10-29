package br.com.example.meufuturoagora.dados;

import java.util.List;

// Pontuação e ranking, sempre calculados no servidor
public interface RankingRepositorio {

    // Professor: turma inteira (só turmas em que dá aula, D2); aluno: pódio e a própria linha
    List<LinhaRanking> ranking(String turmaId) throws ErroApi;

    // Turmas do seletor do ranking
    List<Turma> turmasDoRanking() throws ErroApi;

    // null se o aluno ainda não estiver no ranking da turma
    PosicaoRanking minhaPosicao() throws ErroApi;

    // ano null = ano letivo atual
    int pontuacao(String alunoId, Integer ano) throws ErroApi;
}
