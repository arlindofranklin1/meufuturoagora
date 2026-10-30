package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;

import java.util.List;

public class RankingRepositorioSupabase implements RankingRepositorio {

    private final RestSupabase rest;

    public RankingRepositorioSupabase(RestSupabase rest) {
        this.rest = rest;
    }

    @Override
    public List<LinhaRanking> ranking(String turmaId) throws ErroApi {
        return Json.lista(rest.rpcLista("ranking_turma", Json.objeto("p_turma", turmaId)),
                LinhaRanking::deJson);
    }

    @Override
    public List<Turma> turmasDoRanking() throws ErroApi {
        return Json.lista(rest.rpcLista("turmas_do_ranking", null), Turma::deJson);
    }

    @Override
    public PosicaoRanking minhaPosicao() throws ErroApi {
        JSONArray linhas = rest.rpcLista("minha_posicao_no_ranking", null);
        return linhas.length() > 0 ? Json.um(linhas.optJSONObject(0), PosicaoRanking::deJson) : null;
    }

    @Override
    public int pontuacao(String alunoId, Integer ano) throws ErroApi {
        Object pontos = rest.rpc("pontuacao", Json.objeto("p_aluno", alunoId, "p_ano", ano));
        return pontos instanceof Number ? ((Number) pontos).intValue() : 0;
    }
}
