package br.com.example.meufuturoagora.dados;

import java.util.List;

public class DiarioRepositorioSupabase implements DiarioRepositorio {

    private final RestSupabase rest;

    public DiarioRepositorioSupabase(RestSupabase rest) {
        this.rest = rest;
    }

    @Override
    public List<EntradaDiario> entradas() throws ErroApi {
        return Json.lista(rest.selecionar("diario_entradas",
                Consulta.campos("*").ordenar("criado_em.desc")), EntradaDiario::deJson);
    }

    @Override
    public EntradaDiario escrever(String texto) throws ErroApi {
        return Json.um(rest.inserir("diario_entradas", Json.objeto("texto", texto.trim())),
                EntradaDiario::deJson);
    }

    @Override
    public void editar(String id, String texto) throws ErroApi {

        if (rest.atualizar("diario_entradas", Consulta.campos("id").igual("id", id),
                Json.objeto("texto", texto.trim())).length() == 0) {
            throw ErroApi.daResposta(404, "");
        }
    }

    @Override
    public void apagar(String id) throws ErroApi {
        rest.apagar("diario_entradas", Consulta.campos("id").igual("id", id));
    }
}
