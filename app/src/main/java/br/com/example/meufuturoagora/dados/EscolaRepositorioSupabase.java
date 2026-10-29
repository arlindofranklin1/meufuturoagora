package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;

import java.util.List;

public class EscolaRepositorioSupabase implements EscolaRepositorio {

    private final RestSupabase rest;

    public EscolaRepositorioSupabase(RestSupabase rest) {
        this.rest = rest;
    }

    @Override
    public List<Turma> turmas() throws ErroApi {
        return Json.lista(rest.selecionar("turmas", Consulta.campos("*").ordenar("nome")), Turma::deJson);
    }

    @Override
    public Turma criarTurma(String nome, Integer serie) throws ErroApi {
        return Json.um(rest.inserir("turmas", Json.objeto("nome", nome.trim(), "serie", serie)),
                Turma::deJson);
    }

    @Override
    public Turma atualizarTurma(String id, String nome, Integer serie, boolean ativo) throws ErroApi {

        JSONArray linhas = rest.atualizar("turmas", Consulta.campos("*").igual("id", id),
                Json.objeto("nome", nome.trim(), "serie", serie, "ativo", ativo));

        if (linhas.length() == 0) {
            throw ErroApi.daResposta(403, "");
        }

        return Json.um(linhas.optJSONObject(0), Turma::deJson);
    }

    @Override
    public void excluirTurma(String id) throws ErroApi {

        if (rest.apagar("turmas", Consulta.campos("id").igual("id", id)).length() == 0) {
            throw ErroApi.daResposta(403, "");
        }
    }

    @Override
    public AnoLetivo anoAtual() throws ErroApi {
        return Json.um(rest.selecionarUm("anos_letivos", Consulta.campos("*").igual("atual", true)),
                AnoLetivo::deJson);
    }

    @Override
    public List<AnoLetivo> anos() throws ErroApi {
        return Json.lista(rest.selecionar("anos_letivos", Consulta.campos("*").ordenar("ano.desc")),
                AnoLetivo::deJson);
    }

    @Override
    public List<Bimestre> bimestres(int ano) throws ErroApi {
        return Json.lista(rest.selecionar("bimestres",
                Consulta.campos("*").igual("ano", ano).ordenar("numero")), Bimestre::deJson);
    }

    @Override
    public void definirAnoLetivo(int ano) throws ErroApi {
        rest.rpc("definir_ano_letivo", Json.objeto("p_ano", ano));
    }

    @Override
    public void definirBimestres(int ano, List<Bimestre> bimestres) throws ErroApi {

        JSONArray lista = new JSONArray();

        for (Bimestre bimestre : bimestres) {
            lista.put(Json.objeto(
                    "numero", bimestre.numero,
                    "inicio", bimestre.inicio.toString(),
                    "fim", bimestre.fim.toString()
            ));
        }

        rest.rpc("definir_bimestres", Json.objeto("p_ano", ano, "p_bimestres", lista));
    }
}
