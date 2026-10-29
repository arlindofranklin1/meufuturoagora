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
import java.util.Arrays;
import java.util.List;

import mockwebserver3.RecordedRequest;

public class EscolaEDisciplinaRepositorioTest {

    private ServidorFalso falso;
    private EscolaRepositorioSupabase escola;
    private DisciplinaRepositorioSupabase disciplinas;

    @Before
    public void preparar() throws Exception {
        falso = new ServidorFalso();
        falso.logar();
        escola = new EscolaRepositorioSupabase(falso.rest);
        disciplinas = new DisciplinaRepositorioSupabase(falso.rest);
    }

    @After
    public void encerrar() throws Exception {
        falso.close();
    }

    // ===== Turmas =====

    @Test
    public void turmasCrudCompleto() throws Exception {
        falso.responder("[{\"id\":\"t1\",\"nome\":\"6º A\",\"serie\":6,\"ativo\":true},"
                + "{\"id\":\"t2\",\"nome\":\"7º A\",\"serie\":null,\"ativo\":false}]");
        falso.responder(201, "[{\"id\":\"t3\",\"nome\":\"8º A\",\"serie\":8}]");
        falso.responder("[{\"id\":\"t3\",\"nome\":\"8º B\",\"serie\":8,\"ativo\":false}]");
        falso.responder("[{\"id\":\"t3\"}]");

        List<Turma> turmas = escola.turmas();
        Turma criada = escola.criarTurma(" 8º A ", 8);
        Turma alterada = escola.atualizarTurma("t3", "8º B", 8, false);
        escola.excluirTurma("t3");

        assertEquals("nome", falso.proxima().getUrl().queryParameter("order"));
        assertEquals("8º A", new JSONObject(falso.proxima().getBody().utf8()).getString("nome"));
        assertEquals("PATCH", falso.proxima().getMethod());
        RecordedRequest excluir = falso.proxima();
        assertEquals("DELETE", excluir.getMethod());
        assertEquals("eq.t3", excluir.getUrl().queryParameter("id"));

        assertEquals(Integer.valueOf(6), turmas.get(0).serie);
        assertNull(turmas.get(1).serie);
        assertFalse(turmas.get(1).ativo);
        assertEquals("t3", criada.id);
        assertTrue(criada.ativo);
        assertFalse(alterada.ativo);
    }

    @Test
    public void turmaSemPermissao() {
        falso.responder("[]");
        falso.responder("[]");

        assertTrue(ServidorFalso.erroDe(() -> escola.atualizarTurma("t1", "x", null, true)).semPermissao());
        assertTrue(ServidorFalso.erroDe(() -> escola.excluirTurma("t1")).semPermissao());
    }

    // ===== Ano letivo e bimestres =====

    @Test
    public void anoAtualEBimestres() throws Exception {
        falso.responder("[{\"ano\":2026,\"atual\":true,\"iniciado_em\":\"2026-02-01T10:00:00+00:00\",\"encerrado_em\":null}]");
        falso.responder("[]");
        falso.responder("[{\"ano\":2027,\"atual\":true},{\"ano\":2026,\"atual\":false}]");
        falso.responder("[{\"ano\":2026,\"numero\":1,\"inicio\":\"2026-02-02\",\"fim\":\"2026-04-30\"}]");

        AnoLetivo atual = escola.anoAtual();
        AnoLetivo nenhum = escola.anoAtual();
        List<AnoLetivo> anos = escola.anos();
        List<Bimestre> bimestres = escola.bimestres(2026);

        assertEquals("eq.true", falso.proxima().getUrl().queryParameter("atual"));
        falso.proxima();
        assertEquals("ano.desc", falso.proxima().getUrl().queryParameter("order"));
        assertEquals("eq.2026", falso.proxima().getUrl().queryParameter("ano"));

        assertEquals(2026, atual.ano);
        assertTrue(atual.atual);
        assertNull(atual.encerradoEm);
        assertNull(nenhum);
        assertEquals(2, anos.size());
        Bimestre primeiro = bimestres.get(0);
        assertEquals(LocalDate.of(2026, 2, 2), primeiro.inicio);
        assertTrue(primeiro.contem(LocalDate.of(2026, 4, 30)));
        assertFalse(primeiro.contem(LocalDate.of(2026, 5, 1)));
        assertFalse(primeiro.contem(null));
    }

    @Test
    public void definirAnoEBimestresChamamAsFuncoes() throws Exception {
        falso.responder(204, "");
        falso.responder(204, "");

        escola.definirAnoLetivo(2027);
        escola.definirBimestres(2027, Arrays.asList(
                new Bimestre(2027, 1, LocalDate.of(2027, 2, 1), LocalDate.of(2027, 4, 30)),
                new Bimestre(2027, 2, LocalDate.of(2027, 5, 3), LocalDate.of(2027, 7, 9))));

        assertEquals(2027, new JSONObject(falso.proxima().getBody().utf8()).getInt("p_ano"));
        RecordedRequest bimestres = falso.proxima();
        assertEquals("/rest/v1/rpc/definir_bimestres", bimestres.getUrl().encodedPath());
        JSONArray lista = new JSONObject(bimestres.getBody().utf8()).getJSONArray("p_bimestres");
        assertEquals(2, lista.length());
        assertEquals("2027-05-03", lista.getJSONObject(1).getString("inicio"));
        assertEquals(1, lista.getJSONObject(0).getInt("numero"));
    }

    // ===== Disciplinas =====

    private static final String DISCIPLINA = "{\"id\":\"d1\",\"nome\":\"Matemática\",\"turma_id\":\"t1\","
            + "\"professor_id\":\"p1\",\"ano_letivo\":2026,\"bimestre\":4,\"ativo\":true,"
            + "\"criado_em\":\"2026-10-29T13:00:00+00:00\",\"turmas\":{\"nome\":\"6º A\"}}";

    @Test
    public void listarEBuscarTrazemONomeDaTurma() throws Exception {
        falso.responder("[" + DISCIPLINA + "]");
        falso.responder("[]");

        List<Disciplina> lista = disciplinas.listar();
        Disciplina nenhuma = disciplinas.buscar("d9");

        assertEquals("*,turmas(nome)", falso.proxima().getUrl().queryParameter("select"));
        assertEquals("eq.d9", falso.proxima().getUrl().queryParameter("id"));

        Disciplina d = lista.get(0);
        assertEquals("6º A", d.turmaNome);
        assertEquals(2026, d.anoLetivo);
        assertEquals(Integer.valueOf(4), d.bimestre);
        assertNull(nenhuma);
    }

    @Test
    public void criarRenomearEDesativar() throws Exception {
        falso.responder(201, "[" + DISCIPLINA + "]");
        falso.responder("[{\"id\":\"d1\"}]");
        falso.responder("[{\"id\":\"d1\"}]");
        falso.responder("[]");

        disciplinas.criar(" Matemática ", "t1");
        disciplinas.renomear("d1", "Matemática I");
        disciplinas.desativar("d1");
        ErroApi erro = ServidorFalso.erroDe(() -> disciplinas.renomear("d2", "Invasão"));

        JSONObject criar = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals("Matemática", criar.getString("nome"));
        assertFalse("o professor é o logado (default no banco)", criar.has("professor_id"));
        assertEquals("Matemática I", new JSONObject(falso.proxima().getBody().utf8()).getString("nome"));
        assertFalse(new JSONObject(falso.proxima().getBody().utf8()).getBoolean("ativo"));
        assertTrue(erro.semPermissao());
    }

    @Test
    public void matriculadosEParticipantes() throws Exception {
        falso.responder("[{\"aluno_id\":\"a1\"},{\"aluno_id\":\"a2\"}]");
        falso.responder("[{\"perfil_id\":\"p1\",\"nome\":\"Paula\",\"tipo\":\"professor\",\"email\":\"p1@escola.test\"},"
                + "{\"perfil_id\":\"a1\",\"nome\":\"Ana\",\"tipo\":\"aluno\",\"email\":null}]");

        assertEquals(Arrays.asList("a1", "a2"), disciplinas.alunosMatriculados("d1"));
        List<Participante> participantes = disciplinas.participantes("d1");

        assertEquals("eq.d1", falso.proxima().getUrl().queryParameter("disciplina_id"));
        assertEquals("d1", new JSONObject(falso.proxima().getBody().utf8()).getString("p_disciplina"));
        assertEquals("p1@escola.test", participantes.get(0).email);
        assertNull("e-mail de colega não vem para aluno", participantes.get(1).email);
    }
}
