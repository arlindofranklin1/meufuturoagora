package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.util.List;

public class DisciplinaRepositorioSupabase implements DisciplinaRepositorio {

    // Turma embutida para as telas mostrarem o nome sem outra consulta
    private static final String CAMPOS = "*,turmas(nome)";

    private final RestSupabase rest;

    public DisciplinaRepositorioSupabase(RestSupabase rest) {
        this.rest = rest;
    }

    @Override
    public List<Disciplina> listar() throws ErroApi {
        return Json.lista(rest.selecionar("disciplinas", Consulta.campos(CAMPOS).ordenar("nome")),
                Disciplina::deJson);
    }

    @Override
    public Disciplina buscar(String id) throws ErroApi {
        return Json.um(rest.selecionarUm("disciplinas", Consulta.campos(CAMPOS).igual("id", id)),
                Disciplina::deJson);
    }

    @Override
    public Disciplina criar(String nome, String turmaId) throws ErroApi {
        return Json.um(rest.inserir("disciplinas", Json.objeto("nome", nome.trim(), "turma_id", turmaId)),
                Disciplina::deJson);
    }

    @Override
    public void renomear(String id, String nome) throws ErroApi {
        alterar(id, Json.objeto("nome", nome.trim()));
    }

    @Override
    public void desativar(String id) throws ErroApi {
        alterar(id, Json.objeto("ativo", false));
    }

    @Override
    public List<String> alunosMatriculados(String disciplinaId) throws ErroApi {
        return Json.lista(rest.selecionar("matriculas",
                        Consulta.campos("aluno_id").igual("disciplina_id", disciplinaId)),
                linha -> linha.getString("aluno_id"));
    }

    @Override
    public List<Participante> participantes(String disciplinaId) throws ErroApi {
        return Json.lista(rest.rpcLista("participantes_disciplina", Json.objeto("p_disciplina", disciplinaId)),
                Participante::deJson);
    }

    private void alterar(String id, JSONObject dados) throws ErroApi {

        if (rest.atualizar("disciplinas", Consulta.campos("id").igual("id", id), dados).length() == 0) {
            throw ErroApi.daResposta(403, "");
        }
    }
}
