package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;

import mockwebserver3.RecordedRequest;

// Atividades, entregas, frequência, ranking, diário e novidades
public class AtividadeEFrequenciaRepositorioTest {

    private static final String ATIVIDADE = "{\"id\":\"t1\",\"disciplina_id\":\"d1\",\"nome\":\"Frações\","
            + "\"descricao\":\"\",\"pontos\":10,\"prazo\":\"2026-11-05\",\"link\":null,"
            + "\"arquivo_path\":\"d1/t1/x.pdf\",\"arquivo_nome\":\"enunciado.pdf\",\"ano_letivo\":2026,"
            + "\"bimestre\":4,\"criado_em\":\"2026-10-29T13:00:00+00:00\"}";

    private static final String ENTREGA = "{\"id\":\"e1\",\"atividade_id\":\"t1\",\"aluno_id\":\"a1\","
            + "\"comentario_aluno\":\"Minha resposta\",\"arquivo_path\":\"t1/a1/r.pdf\",\"arquivo_nome\":\"r.pdf\","
            + "\"enviado_em\":\"2026-11-08T12:00:00+00:00\",\"dias_atraso\":3,\"nota\":8.5,"
            + "\"comentario_professor\":\"Bom\",\"avaliado_em\":\"2026-11-09T12:00:00+00:00\"}";

    private static final String MEU_PERFIL = "[{\"id\":\"a1\"}]";

    private ServidorFalso falso;
    private AtividadeRepositorioSupabase atividades;

    @Before
    public void preparar() throws Exception {
        falso = new ServidorFalso();
        falso.logar();
        atividades = new AtividadeRepositorioSupabase(falso.rest, falso.sessao);
    }

    @After
    public void encerrar() throws Exception {
        falso.close();
    }

    // ===== Atividades =====

    @Test
    public void listarBuscarECriarAtividade() throws Exception {
        falso.responder("[" + ATIVIDADE + "]");
        falso.responder("[" + ATIVIDADE + "]");
        falso.responder(201, "[" + ATIVIDADE + "]");

        List<Atividade> lista = atividades.daDisciplina("d1");
        Atividade uma = atividades.buscar("t1");
        atividades.criar("t1", "d1", " Frações ", null, 10, LocalDate.of(2026, 11, 5), "  ",
                "d1/t1/x.pdf", "enunciado.pdf");

        RecordedRequest listar = falso.proxima();
        assertEquals("eq.d1", listar.getUrl().queryParameter("disciplina_id"));
        assertEquals("criado_em.desc", listar.getUrl().queryParameter("order"));
        falso.proxima();
        JSONObject criar = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals("t1", criar.getString("id"));
        assertEquals("Frações", criar.getString("nome"));
        assertEquals("", criar.getString("descricao"));
        assertEquals("2026-11-05", criar.getString("prazo"));
        assertTrue("link em branco vai como null", criar.isNull("link"));

        Atividade a = lista.get(0);
        assertEquals(10, a.pontos);
        assertEquals(LocalDate.of(2026, 11, 5), a.prazo);
        assertTrue(a.temArquivo());
        assertNull(a.link);
        assertEquals("t1", uma.id);
    }

    @Test
    public void atualizarAtividade() throws Exception {
        falso.responder("[" + ATIVIDADE + "]");
        falso.responder("[]");

        atividades.atualizar("t1", "Frações", "nova", 5, LocalDate.of(2026, 12, 1), "https://x.test", null, null);
        ErroApi erro = ServidorFalso.erroDe(() ->
                atividades.atualizar("t2", "X", "", 5, LocalDate.of(2026, 12, 1), null, null, null));

        JSONObject corpo = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals(5, corpo.getInt("pontos"));
        assertEquals("https://x.test", corpo.getString("link"));
        assertTrue(corpo.isNull("arquivo_path"));
        assertTrue(erro.semPermissao());
    }

    @Test
    public void arquivosDaAtividadeJuntaAnexoEEntregas() throws Exception {
        falso.responder("[" + ATIVIDADE + "]");
        falso.responder("[" + ENTREGA + ",{\"id\":\"e2\",\"atividade_id\":\"t1\",\"aluno_id\":\"a2\",\"arquivo_path\":null}]");

        AtividadeRepositorio.ArquivosDaAtividade arquivos = atividades.arquivosDaAtividade("t1");

        assertEquals("d1/t1/x.pdf", arquivos.anexo);
        assertEquals(Arrays.asList("t1/a1/r.pdf"), arquivos.entregas);
    }

    @Test
    public void arquivosDeAtividadeInexistente() {
        falso.responder("[]");

        assertEquals(404, ServidorFalso.erroDe(() -> atividades.arquivosDaAtividade("t9")).codigoHttp());
    }

    @Test
    public void excluirAtividade() throws Exception {
        falso.responder("[{\"id\":\"t1\"}]");
        falso.responder("[]");

        atividades.excluir("t1");
        ErroApi erro = ServidorFalso.erroDe(() -> atividades.excluir("t2"));

        assertEquals("DELETE", falso.proxima().getMethod());
        assertTrue(erro.semPermissao());
    }

    // ===== Entregas =====

    @Test
    public void entregaLidaComAtrasoENota() throws Exception {
        falso.responder("[" + ENTREGA + "]");

        Entrega e = atividades.entregasDaAtividade("t1").get(0);

        assertEquals("enviado_em", falso.proxima().getUrl().queryParameter("order"));
        assertEquals(3, e.diasAtraso);
        assertTrue(e.atrasada());
        assertTrue(e.avaliada());
        assertEquals(8.5, e.nota, 0.0001);
        assertEquals(OffsetDateTime.parse("2026-11-09T12:00:00+00:00"), e.avaliadoEm);
    }

    @Test
    public void minhaEntregaFiltraPeloPerfilDoAlunoLogado() throws Exception {
        falso.responder(MEU_PERFIL);
        falso.responder("[]");
        falso.responder(MEU_PERFIL);
        falso.responder("[" + ENTREGA + "]");

        assertNull(atividades.minhaEntrega("t1"));
        List<Entrega> minhas = atividades.minhasEntregas();

        assertEquals("eq." + ServidorFalso.USUARIO_ID, falso.proxima().getUrl().queryParameter("usuario_id"));
        RecordedRequest minha = falso.proxima();
        assertEquals("eq.t1", minha.getUrl().queryParameter("atividade_id"));
        assertEquals("eq.a1", minha.getUrl().queryParameter("aluno_id"));
        falso.proxima();
        assertEquals("enviado_em.desc", falso.proxima().getUrl().queryParameter("order"));
        assertEquals(1, minhas.size());
    }

    @Test
    public void minhaEntregaSemPerfilOuSemSessao() throws Exception {
        falso.responder("[]");

        assertTrue(ServidorFalso.erroDe(() -> atividades.minhaEntrega("t1")).semPermissao());

        falso.responder(204, "");
        falso.sessao.sair();
        assertEquals(401, ServidorFalso.erroDe(() -> atividades.minhasEntregas()).codigoHttp());
    }

    @Test
    public void entregasDeUmAlunoParaOAdmin() throws Exception {
        falso.responder("[" + ENTREGA + "]");

        List<Entrega> entregas = atividades.entregasDoAluno("a1");

        assertEquals("eq.a1", falso.proxima().getUrl().queryParameter("aluno_id"));
        assertEquals("t1/a1/r.pdf", entregas.get(0).arquivoPath);
    }

    @Test
    public void entregarAvaliarERefazer() throws Exception {
        falso.responder(201, "[" + ENTREGA + "]");
        falso.responder(204, "");
        falso.responder(204, "");

        atividades.entregar("t1", "  Minha resposta  ", "t1/a1/r.pdf", "r.pdf");
        atividades.avaliar("e1", 8.5, "Bom");
        atividades.refazerAvaliacao("e1");

        JSONObject entregar = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals("Minha resposta", entregar.getString("comentario_aluno"));
        assertFalse("o aluno é o logado (default no banco)", entregar.has("aluno_id"));
        assertFalse("nota nunca vai do app", entregar.has("nota"));
        JSONObject avaliar = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals(8.5, avaliar.getDouble("p_nota"), 0.0001);
        assertEquals("Bom", avaliar.getString("p_comentario"));
        assertEquals("/rest/v1/rpc/refazer_avaliacao", falso.proxima().getUrl().encodedPath());
    }

    // ===== Frequência =====

    @Test
    public void aulasEFrequencia() throws Exception {
        FrequenciaRepositorioSupabase frequencias = new FrequenciaRepositorioSupabase(falso.rest);
        falso.responder("[{\"id\":\"au1\",\"disciplina_id\":\"d1\",\"data\":\"2026-10-20\",\"quantidade\":2,\"descricao\":\"\",\"bimestre\":4}]");
        falso.responder(201, "[{\"id\":\"au2\",\"disciplina_id\":\"d1\",\"data\":\"2026-10-21\",\"quantidade\":1}]");
        falso.responder("[{\"aula_id\":\"au1\",\"aluno_id\":\"a1\",\"status\":\"falta\"}]");
        falso.responder("[]");
        falso.responder("2");

        Aula aula = frequencias.aulas("d1").get(0);
        frequencias.cadastrarAula("d1", LocalDate.of(2026, 10, 21), 1, null);
        Frequencia registro = frequencias.frequenciaDaAula("au1").get(0);
        assertTrue(frequencias.frequencias().isEmpty());
        int gravados = frequencias.registrar("au1", Arrays.asList(
                new Frequencia("au1", "a1", Frequencia.FALTA),
                new Frequencia("au1", "a2", Frequencia.PRESENTE)));

        assertEquals("data.desc", falso.proxima().getUrl().queryParameter("order"));
        JSONObject cadastrar = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals("2026-10-21", cadastrar.getString("data"));
        assertEquals("", cadastrar.getString("descricao"));
        assertEquals("eq.au1", falso.proxima().getUrl().queryParameter("aula_id"));
        falso.proxima();
        RecordedRequest registrar = falso.proxima();
        assertEquals("/rest/v1/rpc/registrar_frequencia", registrar.getUrl().encodedPath());
        JSONArray lista = new JSONObject(registrar.getBody().utf8()).getJSONArray("p_registros");
        assertEquals("falta", lista.getJSONObject(0).getString("status"));
        assertEquals("a2", lista.getJSONObject(1).getString("aluno_id"));

        assertEquals(LocalDate.of(2026, 10, 20), aula.data);
        assertEquals(2, aula.quantidade);
        assertEquals(Frequencia.FALTA, registro.status);
        assertEquals(2, gravados);
    }

    @Test
    public void excluirAulaApagaPeloIdESemLinhaApagadaESemPermissao() throws Exception {
        FrequenciaRepositorioSupabase frequencias = new FrequenciaRepositorioSupabase(falso.rest);
        falso.responder("[{\"id\":\"au1\"}]");
        falso.responder("[]");

        frequencias.excluirAula("au1");
        ErroApi erro = ServidorFalso.erroDe(() -> frequencias.excluirAula("au2"));

        RecordedRequest apagar = falso.proxima();
        assertEquals("DELETE", apagar.getMethod());
        assertEquals("/rest/v1/aulas", apagar.getUrl().encodedPath());
        assertEquals("eq.au1", apagar.getUrl().queryParameter("id"));
        assertTrue("o RLS não dá erro: nenhuma linha apagada é recusa", erro.semPermissao());
    }

    // ===== Ranking =====

    @Test
    public void rankingPosicaoEPontuacao() throws Exception {
        RankingRepositorioSupabase ranking = new RankingRepositorioSupabase(falso.rest);
        falso.responder("[{\"perfil_id\":\"a5\",\"nome\":\"Elisa\",\"pontos\":9,\"posicao\":1,\"eh_voce\":false},"
                + "{\"perfil_id\":\"a1\",\"nome\":\"Ana\",\"pontos\":7,\"posicao\":2,\"eh_voce\":true}]");
        falso.responder("[{\"id\":\"t1\",\"nome\":\"6º A\"}]");
        falso.responder("[{\"posicao\":2,\"pontos\":7,\"total\":5}]");
        falso.responder("[]");
        falso.responder("-2");

        List<LinhaRanking> linhas = ranking.ranking("t1");
        List<Turma> turmas = ranking.turmasDoRanking();
        PosicaoRanking posicao = ranking.minhaPosicao();
        assertNull("aluno sem turma: sem posição", ranking.minhaPosicao());
        int pontos = ranking.pontuacao("a1", null);

        assertEquals("t1", new JSONObject(falso.proxima().getBody().utf8()).getString("p_turma"));
        assertEquals("/rest/v1/rpc/turmas_do_ranking", falso.proxima().getUrl().encodedPath());
        falso.proxima();
        falso.proxima();
        JSONObject pontuacao = new JSONObject(falso.proxima().getBody().utf8());
        assertTrue("ano nulo = ano atual", pontuacao.isNull("p_ano"));

        assertTrue(linhas.get(1).ehVoce);
        assertEquals(9, linhas.get(0).pontos);
        assertEquals("6º A", turmas.get(0).nome);
        assertEquals(5, posicao.total);
        assertEquals(-2, pontos);
    }

    // ===== Diário =====

    @Test
    public void diarioCompleto() throws Exception {
        DiarioRepositorioSupabase diario = new DiarioRepositorioSupabase(falso.rest);
        falso.responder("[{\"id\":\"n1\",\"texto\":\"Hoje\",\"criado_em\":\"2026-10-29T13:00:00+00:00\",\"atualizado_em\":\"2026-10-29T13:00:00+00:00\"}]");
        falso.responder(201, "[{\"id\":\"n2\",\"texto\":\"Amanhã\"}]");
        falso.responder("[{\"id\":\"n2\"}]");
        falso.responder("[]");
        falso.responder("[]");

        assertEquals("Hoje", diario.entradas().get(0).texto);
        assertEquals("n2", diario.escrever("  Amanhã ").id);
        diario.editar("n2", "Amanhã cedo");
        ErroApi erro = ServidorFalso.erroDe(() -> diario.editar("n9", "x"));
        diario.apagar("n2");

        assertEquals("criado_em.desc", falso.proxima().getUrl().queryParameter("order"));
        assertEquals("Amanhã", new JSONObject(falso.proxima().getBody().utf8()).getString("texto"));
        assertEquals("PATCH", falso.proxima().getMethod());
        falso.proxima();
        assertEquals("DELETE", falso.proxima().getMethod());
        assertEquals(404, erro.codigoHttp());
    }

    // ===== Novidades =====

    @Test
    public void novidadesDesdeUmMomento() throws Exception {
        NovidadeRepositorioSupabase novidades = new NovidadeRepositorioSupabase(falso.rest);
        falso.responder("[{\"tipo\":\"entrega\",\"item_id\":\"e1\",\"nome\":\"Frações\",\"disciplina_id\":\"d1\","
                + "\"disciplina_nome\":\"Matemática\",\"atividade_id\":\"t1\",\"aluno_nome\":\"Ana\","
                + "\"momento\":\"2026-11-08T12:00:00+00:00\"}]");

        List<Novidade> lista = novidades.desde(OffsetDateTime.parse("2026-11-01T00:00:00-03:00"));

        assertEquals("2026-11-01T00:00-03:00",
                new JSONObject(falso.proxima().getBody().utf8()).getString("p_desde"));
        assertEquals(Novidade.ENTREGA, lista.get(0).tipo);
        assertEquals("Ana", lista.get(0).alunoNome);
    }
}
