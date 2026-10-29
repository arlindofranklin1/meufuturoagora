package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;

import java.time.LocalDate;
import java.util.List;

public class FrequenciaRepositorioSupabase implements FrequenciaRepositorio {

    private final RestSupabase rest;

    public FrequenciaRepositorioSupabase(RestSupabase rest) {
        this.rest = rest;
    }

    @Override
    public List<Aula> aulas(String disciplinaId) throws ErroApi {
        return Json.lista(rest.selecionar("aulas",
                Consulta.campos("*").igual("disciplina_id", disciplinaId).ordenar("data.desc")),
                Aula::deJson);
    }

    @Override
    public Aula cadastrarAula(String disciplinaId, LocalDate data, int quantidade, String descricao)
            throws ErroApi {
        return Json.um(rest.inserir("aulas", Json.objeto(
                "disciplina_id", disciplinaId,
                "data", data.toString(),
                "quantidade", quantidade,
                "descricao", descricao != null ? descricao.trim() : ""
        )), Aula::deJson);
    }

    @Override
    public List<Frequencia> frequenciaDaAula(String aulaId) throws ErroApi {
        return Json.lista(rest.selecionar("frequencias", Consulta.campos("*").igual("aula_id", aulaId)),
                Frequencia::deJson);
    }

    @Override
    public List<Frequencia> frequencias() throws ErroApi {
        return Json.lista(rest.selecionar("frequencias", Consulta.campos("*")), Frequencia::deJson);
    }

    @Override
    public int registrar(String aulaId, List<Frequencia> registros) throws ErroApi {

        JSONArray lista = new JSONArray();

        for (Frequencia registro : registros) {
            lista.put(Json.objeto("aluno_id", registro.alunoId, "status", registro.status));
        }

        Object total = rest.rpc("registrar_frequencia", Json.objeto("p_aula", aulaId, "p_registros", lista));
        return total instanceof Number ? ((Number) total).intValue() : 0;
    }
}
