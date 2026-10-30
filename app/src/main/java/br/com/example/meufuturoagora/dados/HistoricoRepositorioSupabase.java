package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class HistoricoRepositorioSupabase implements HistoricoRepositorio {

    private final RestSupabase rest;

    public HistoricoRepositorioSupabase(RestSupabase rest) {
        this.rest = rest;
    }

    @Override
    public List<Integer> anos() throws ErroApi {

        JSONArray linhas = rest.rpcLista("historico_anos", null);
        List<Integer> anos = new ArrayList<>();

        for (int i = 0; i < linhas.length(); i++) {
            anos.add(linhas.optJSONObject(i).optInt("ano"));
        }

        return anos;
    }

    @Override
    public List<HistoricoAluno> alunos(int ano, int bimestre) throws ErroApi {
        return Json.lista(rest.rpcLista("historico_alunos", periodo(ano, bimestre)), HistoricoAluno::deJson);
    }

    @Override
    public List<HistoricoTrilha> trilhas(int ano, int bimestre) throws ErroApi {
        return Json.lista(rest.rpcLista("historico_trilhas", periodo(ano, bimestre)), HistoricoTrilha::deJson);
    }

    @Override
    public List<HistoricoEntrega> entregas(int ano, int bimestre) throws ErroApi {
        return Json.lista(rest.rpcLista("historico_entregas", periodo(ano, bimestre)), HistoricoEntrega::deJson);
    }

    private static JSONObject periodo(int ano, int bimestre) {
        return Json.objeto("p_ano", ano, "p_bimestre", bimestre);
    }
}
