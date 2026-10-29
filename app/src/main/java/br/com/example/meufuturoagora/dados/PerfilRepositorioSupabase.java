package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;

import java.util.List;

public class PerfilRepositorioSupabase implements PerfilRepositorio {

    private final RestSupabase rest;
    private final GerenciadorSessao sessao;

    public PerfilRepositorioSupabase(RestSupabase rest, GerenciadorSessao sessao) {
        this.rest = rest;
        this.sessao = sessao;
    }

    @Override
    public Perfil meu() throws ErroApi {

        Sessao atual = sessao.atual();

        if (atual == null) {
            throw ErroApi.sessaoExpirada();
        }

        return Json.um(rest.selecionarUm("perfis",
                Consulta.campos("*").igual("usuario_id", atual.usuarioId)), Perfil::deJson);
    }

    @Override
    public List<Perfil> listar(String tipo) throws ErroApi {
        return Json.lista(rest.selecionar("perfis",
                Consulta.campos("*").igual("tipo", tipo).ordenar("nome")), Perfil::deJson);
    }

    // email é citext no banco: o "eq" do PostgREST já não diferencia maiúsculas
    @Override
    public Perfil buscarPorEmail(String email) throws ErroApi {
        return Json.um(rest.selecionarUm("perfis", Consulta.campos("*").igual("email", email.trim())),
                Perfil::deJson);
    }

    @Override
    public Perfil cadastrar(String email, String nome, String tipo, String turmaId) throws ErroApi {
        return Json.um(rest.inserir("perfis", Json.objeto(
                "email", email.trim(),
                "nome", nome.trim(),
                "tipo", tipo,
                "turma_id", turmaId
        )), Perfil::deJson);
    }

    @Override
    public Perfil atualizar(String id, String email, String nome, String turmaId, boolean ativo,
                            boolean admin) throws ErroApi {

        JSONArray linhas = rest.atualizar("perfis", Consulta.campos("*").igual("id", id), Json.objeto(
                "email", email.trim(),
                "nome", nome.trim(),
                "turma_id", turmaId,
                "ativo", ativo,
                "admin", admin
        ));

        return primeiraOuSemPermissao(linhas);
    }

    @Override
    public void definirFoto(String perfilId, String fotoPath) throws ErroApi {
        primeiraOuSemPermissao(rest.atualizar("perfis", Consulta.campos("id").igual("id", perfilId),
                Json.objeto("foto_path", fotoPath)));
    }

    @Override
    public void excluir(String perfilId) throws ErroApi {
        rest.rpc("excluir_usuario", Json.objeto("p_perfil", perfilId));
    }

    @Override
    public int transferirDisciplinas(String deProfessorId, String paraProfessorId) throws ErroApi {
        Object total = rest.rpc("transferir_disciplinas",
                Json.objeto("p_de", deProfessorId, "p_para", paraProfessorId));
        return total instanceof Number ? ((Number) total).intValue() : 0;
    }

    @Override
    public String solicitarExclusao(String motivo) throws ErroApi {
        Object id = rest.rpc("solicitar_exclusao", Json.objeto("p_motivo", motivo));
        return id != null ? String.valueOf(id) : null;
    }

    @Override
    public List<SolicitacaoExclusao> solicitacoes() throws ErroApi {
        return Json.lista(rest.selecionar("solicitacoes_exclusao",
                Consulta.campos("*").ordenar("criado_em.desc")), SolicitacaoExclusao::deJson);
    }

    @Override
    public void decidirExclusao(String solicitacaoId, boolean aprovar) throws ErroApi {
        rest.rpc("decidir_exclusao", Json.objeto("p_solicitacao", solicitacaoId, "p_aprovar", aprovar));
    }

    // O RLS não dá erro ao filtrar: nenhuma linha alterada quer dizer sem permissão
    private static Perfil primeiraOuSemPermissao(JSONArray linhas) throws ErroApi {

        if (linhas.length() == 0) {
            throw ErroApi.daResposta(403, "");
        }

        return Json.um(linhas.optJSONObject(0), Perfil::deJson);
    }
}
