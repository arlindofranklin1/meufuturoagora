package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class AtividadeRepositorioSupabase implements AtividadeRepositorio {

    private final RestSupabase rest;
    private final GerenciadorSessao sessao;

    public AtividadeRepositorioSupabase(RestSupabase rest, GerenciadorSessao sessao) {
        this.rest = rest;
        this.sessao = sessao;
    }

    @Override
    public List<Atividade> daDisciplina(String disciplinaId) throws ErroApi {
        return Json.lista(rest.selecionar("atividades",
                Consulta.campos("*").igual("disciplina_id", disciplinaId).ordenar("criado_em.desc")),
                Atividade::deJson);
    }

    @Override
    public Atividade buscar(String id) throws ErroApi {
        return Json.um(rest.selecionarUm("atividades", Consulta.campos("*").igual("id", id)),
                Atividade::deJson);
    }

    @Override
    public Atividade criar(String id, String disciplinaId, String nome, String descricao, int pontos,
                           LocalDate prazo, String link, String arquivoPath, String arquivoNome)
            throws ErroApi {

        return Json.um(rest.inserir("atividades", Json.objeto(
                "id", id,
                "disciplina_id", disciplinaId,
                "nome", nome.trim(),
                "descricao", descricao != null ? descricao.trim() : "",
                "pontos", pontos,
                "prazo", prazo.toString(),
                "link", vazioViraNulo(link),
                "arquivo_path", arquivoPath,
                "arquivo_nome", arquivoNome
        )), Atividade::deJson);
    }

    @Override
    public Atividade atualizar(String id, String nome, String descricao, int pontos, LocalDate prazo,
                               String link, String arquivoPath, String arquivoNome) throws ErroApi {

        JSONArray linhas = rest.atualizar("atividades", Consulta.campos("*").igual("id", id), Json.objeto(
                "nome", nome.trim(),
                "descricao", descricao != null ? descricao.trim() : "",
                "pontos", pontos,
                "prazo", prazo.toString(),
                "link", vazioViraNulo(link),
                "arquivo_path", arquivoPath,
                "arquivo_nome", arquivoNome
        ));

        if (linhas.length() == 0) {
            throw ErroApi.daResposta(403, "");
        }

        return Json.um(linhas.optJSONObject(0), Atividade::deJson);
    }

    @Override
    public ArquivosDaAtividade arquivosDaAtividade(String atividadeId) throws ErroApi {

        Atividade atividade = buscar(atividadeId);

        if (atividade == null) {
            throw ErroApi.daResposta(404, "");
        }

        List<String> entregas = new ArrayList<>();

        for (Entrega entrega : entregasDaAtividade(atividadeId)) {
            if (entrega.arquivoPath != null) {
                entregas.add(entrega.arquivoPath);
            }
        }

        return new ArquivosDaAtividade(atividade.arquivoPath, entregas);
    }

    @Override
    public void excluir(String atividadeId) throws ErroApi {

        if (rest.apagar("atividades", Consulta.campos("id").igual("id", atividadeId)).length() == 0) {
            throw ErroApi.daResposta(403, "");
        }
    }

    @Override
    public List<Entrega> entregasDaAtividade(String atividadeId) throws ErroApi {
        return Json.lista(rest.selecionar("entregas",
                Consulta.campos("*").igual("atividade_id", atividadeId).ordenar("enviado_em")),
                Entrega::deJson);
    }

    @Override
    public Entrega minhaEntrega(String atividadeId) throws ErroApi {
        return Json.um(rest.selecionarUm("entregas", Consulta.campos("*")
                .igual("atividade_id", atividadeId)
                .igual("aluno_id", meuPerfilId())), Entrega::deJson);
    }

    @Override
    public List<Entrega> minhasEntregas() throws ErroApi {
        return Json.lista(rest.selecionar("entregas",
                Consulta.campos("*").igual("aluno_id", meuPerfilId()).ordenar("enviado_em.desc")),
                Entrega::deJson);
    }

    @Override
    public Entrega entregar(String atividadeId, String comentario, String arquivoPath, String arquivoNome)
            throws ErroApi {

        return Json.um(rest.inserir("entregas", Json.objeto(
                "atividade_id", atividadeId,
                "comentario_aluno", comentario != null ? comentario.trim() : "",
                "arquivo_path", arquivoPath,
                "arquivo_nome", arquivoNome
        )), Entrega::deJson);
    }

    @Override
    public void avaliar(String entregaId, double nota, String comentario) throws ErroApi {
        rest.rpc("avaliar_entrega", Json.objeto("p_entrega", entregaId, "p_nota", nota, "p_comentario", comentario));
    }

    @Override
    public void refazerAvaliacao(String entregaId) throws ErroApi {
        rest.rpc("refazer_avaliacao", Json.objeto("p_entrega", entregaId));
    }

    // Os filtros "do aluno logado" precisam do id do perfil, que é diferente do
    // id da conta: vem da tabela perfis pelo usuario_id da sessão
    private String meuPerfilId() throws ErroApi {

        Sessao atual = sessao.atual();

        if (atual == null) {
            throw ErroApi.sessaoExpirada();
        }

        Perfil perfil = Json.um(rest.selecionarUm("perfis",
                Consulta.campos("id").igual("usuario_id", atual.usuarioId)), Perfil::deJson);

        if (perfil == null) {
            throw ErroApi.daResposta(403, "");
        }

        return perfil.id;
    }

    private static String vazioViraNulo(String texto) {
        return texto == null || texto.trim().isEmpty() ? null : texto.trim();
    }
}
