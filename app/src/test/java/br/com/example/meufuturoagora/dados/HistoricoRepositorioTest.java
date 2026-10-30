package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import mockwebserver3.RecordedRequest;

public class HistoricoRepositorioTest {

    private ServidorFalso falso;
    private HistoricoRepositorioSupabase historico;

    @Before
    public void preparar() throws Exception {
        falso = new ServidorFalso();
        falso.logar();
        historico = new HistoricoRepositorioSupabase(falso.rest);
    }

    @After
    public void encerrar() throws Exception {
        falso.close();
    }

    @Test
    public void anosVemComoLista() throws Exception {
        falso.responder("[{\"ano\":2027},{\"ano\":2026}]");

        List<Integer> anos = historico.anos();

        RecordedRequest requisicao = falso.proxima();
        assertEquals("/rest/v1/rpc/historico_anos", requisicao.getUrl().encodedPath());
        assertEquals("{}", requisicao.getBody().utf8());
        assertEquals(Arrays.asList(2027, 2026), anos);
    }

    @Test
    public void alunosPedemOPeriodoELeemAsLinhas() throws Exception {
        falso.responder("[{\"aluno_id\":\"a1\",\"nome\":\"Ana\",\"turma_nome\":\"6º ano A\",\"entregas\":2,"
                + "\"dias_falta\":1,\"pontos\":-2},"
                + "{\"aluno_id\":\"a2\",\"nome\":\"Bruno\",\"turma_nome\":null,\"entregas\":0,\"dias_falta\":3,\"pontos\":-15}]");

        List<HistoricoAluno> alunos = historico.alunos(2026, 4);

        RecordedRequest requisicao = falso.proxima();
        assertEquals("/rest/v1/rpc/historico_alunos", requisicao.getUrl().encodedPath());
        JSONObject corpo = new JSONObject(requisicao.getBody().utf8());
        assertEquals(2026, corpo.getInt("p_ano"));
        assertEquals(4, corpo.getInt("p_bimestre"));

        assertEquals(2, alunos.size());
        assertEquals("Ana", alunos.get(0).nome);
        assertEquals(-2, alunos.get(0).pontos);
        assertEquals(1, alunos.get(0).diasFalta);
        assertNull("aluno sem turma", alunos.get(1).turmaNome);
        assertEquals(-15, alunos.get(1).pontos);
    }

    @Test
    public void anoInteiroVaiComoBimestreZero() throws Exception {
        falso.responder("[]");

        historico.trilhas(2026, 0);

        assertEquals(0, new JSONObject(falso.proxima().getBody().utf8()).getInt("p_bimestre"));
    }

    @Test
    public void trilhasLeemDisciplinaProfessorPrazoEEntregas() throws Exception {
        falso.responder("[{\"atividade_id\":\"t1\",\"nome\":\"Frações\",\"disciplina_nome\":\"Matemática\","
                + "\"professor_nome\":\"Paula\",\"turma_nome\":\"6º ano A\",\"prazo\":\"2026-11-05\",\"pontos\":10,"
                + "\"entregas\":7,\"criado_em\":\"2026-10-29T13:00:00.123456+00:00\"}]");

        HistoricoTrilha trilha = historico.trilhas(2026, 4).get(0);

        assertEquals("/rest/v1/rpc/historico_trilhas", falso.proxima().getUrl().encodedPath());
        assertEquals("Frações", trilha.nome);
        assertEquals("Paula", trilha.professorNome);
        assertEquals(LocalDate.of(2026, 11, 5), trilha.prazo);
        assertEquals(7, trilha.entregas);
        assertEquals(2026, trilha.criadoEm.getYear());
    }

    @Test
    public void entregasPendentesTemNotaNulaEAtrasoVemJunto() throws Exception {
        falso.responder("[{\"entrega_id\":\"e1\",\"aluno_nome\":\"Ana\",\"atividade_nome\":\"Frações\","
                + "\"disciplina_nome\":\"Matemática\",\"turma_nome\":\"6º ano A\","
                + "\"enviado_em\":\"2026-11-08T12:00:00+00:00\",\"dias_atraso\":3,\"nota\":8.5},"
                + "{\"entrega_id\":\"e2\",\"aluno_nome\":\"Bruno\",\"atividade_nome\":\"Frações\","
                + "\"disciplina_nome\":\"Matemática\",\"turma_nome\":\"6º ano A\","
                + "\"enviado_em\":\"2026-11-06T12:00:00+00:00\",\"dias_atraso\":0,\"nota\":null}]");

        List<HistoricoEntrega> entregas = historico.entregas(2026, 0);

        assertEquals("/rest/v1/rpc/historico_entregas", falso.proxima().getUrl().encodedPath());
        assertTrue(entregas.get(0).avaliada());
        assertEquals(8.5, entregas.get(0).nota, 0.0001);
        assertEquals(3, entregas.get(0).diasAtraso);
        assertFalse(entregas.get(1).avaliada());
        assertNull(entregas.get(1).nota);
    }

    @Test
    public void quemNaoEAdminRecebeRecusaDoServidor() {
        falso.responder(403, "{\"code\":\"42501\",\"message\":\"Apenas administradores podem fazer isto.\"}");

        ErroApi erro = ServidorFalso.erroDe(() -> historico.alunos(2026, 0));

        assertTrue(erro.semPermissao());
        assertEquals("Apenas administradores podem fazer isto.", erro.getMessage());
    }

    @Test
    public void linhaMalformadaViraRespostaInvalida() {
        falso.responder("[{\"prazo\":\"amanhã\"}]");

        ErroApi erro = ServidorFalso.erroDe(() -> historico.trilhas(2026, 0));

        assertEquals("O servidor mandou uma resposta inesperada.", erro.getMessage());
    }
}
