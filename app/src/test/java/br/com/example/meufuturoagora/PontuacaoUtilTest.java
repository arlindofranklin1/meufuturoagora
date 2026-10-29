package br.com.example.meufuturoagora;

import static br.com.example.meufuturoagora.DocumentoFalso.mapa;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.google.firebase.Timestamp;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

// Caracterização da regra do ranking (PontuacaoUtil.java:21): soma das notas das
// trilhas avaliadas - 5 por dia de falta, só no ano letivo atual. Na Fase 3 os
// mesmos casos viram testes pgTAP da função pontuacao_aluno().
@RunWith(RobolectricTestRunner.class)
public class PontuacaoUtilTest {

    private FirestoreFalso banco;

    @Before
    public void prepararBanco() {

        banco = new FirestoreFalso();

        banco.gravar("configuracoes", "geral", mapa(
                "anoLetivo", "2026",
                "bimestre1Inicio", "02/02/2026", "bimestre1Fim", "30/04/2026",
                "bimestre4Inicio", "01/10/2026", "bimestre4Fim", "18/12/2026"
        ));

        banco.gravar("alunos", "ana", mapa("nome", "Ana", "pontuacao", 0L));
        banco.gravar("disciplinas", "mat", mapa("nome", "Matemática", "ativo", true));
        banco.gravar("atividades", "t1", mapa("disciplinaId", "mat", "ativo", true));
        banco.gravar("atividades", "t2", mapa("disciplinaId", "mat", "ativo", true));
    }

    private static Timestamp momento(String texto) throws ParseException {
        return new Timestamp(new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).parse(texto));
    }

    private void entrega(String id, String atividade, double nota, Object... extras) {

        Map<String, Object> dados = mapa(extras);
        dados.put("alunoId", "ana");
        dados.put("atividadeId", atividade);
        dados.put("avaliado", true);
        dados.put("nota", nota);
        banco.gravar("entregas", id, dados);
    }

    private void penalidade(String id, Object... extras) {

        Map<String, Object> dados = mapa(extras);
        dados.put("alunoId", "ana");
        dados.put("pontos", -5);
        banco.gravar("penalidades", id, dados);
    }

    private long recalcularAna() {
        PontuacaoUtil.recalcular(banco.db, "ana");
        shadowOf(Looper.getMainLooper()).idle();
        return (Long) banco.ler("alunos", "ana").get("pontuacao");
    }

    @Test
    public void somaNotasEDesconta5PorPenalidade() {
        entrega("e1", "t1", 8.0, "anoLetivo", "2026");
        entrega("e2", "t2", 6.0, "anoLetivo", "2026");
        penalidade("p1", "anoLetivo", "2026");

        assertEquals(9L, recalcularAna());
    }

    @Test
    public void arredondaMeioParaCima() {
        entrega("e1", "t1", 7.5, "anoLetivo", "2026");

        assertEquals(8L, recalcularAna());
    }

    @Test
    public void totalNegativoArredondaEmDirecaoAoMaior() {
        // Math.round(-2.5) = -2
        entrega("e1", "t1", 2.5, "anoLetivo", "2026");
        penalidade("p1", "anoLetivo", "2026");

        assertEquals(-2L, recalcularAna());
    }

    @Test
    public void soPenalidadesDeixaAPontuacaoNegativa() {
        penalidade("p1", "anoLetivo", "2026");
        penalidade("p2", "anoLetivo", "2026");

        assertEquals(-10L, recalcularAna());
    }

    @Test
    public void entregaNaoAvaliadaNaoConta() {
        entrega("e1", "t1", 9.0, "anoLetivo", "2026");
        banco.ler("entregas", "e1").put("avaliado", false);

        assertEquals(0L, recalcularAna());
    }

    @Test
    public void entregaAvaliadaSemNotaNaoConta() {
        banco.gravar("entregas", "e1", mapa(
                "alunoId", "ana", "atividadeId", "t1", "avaliado", true, "anoLetivo", "2026"));

        assertEquals(0L, recalcularAna());
    }

    @Test
    public void registrosDeOutroAnoLetivoNaoContam() {
        entrega("e1", "t1", 9.0, "anoLetivo", "2025");
        penalidade("p1", "anoLetivo", "2025");
        entrega("e2", "t2", 4.0, "anoLetivo", "2026");

        assertEquals(4L, recalcularAna());
    }

    @Test
    public void trilhaApagadaNaoConta() {
        entrega("e1", "apagada", 9.0, "anoLetivo", "2026");
        entrega("e2", "t1", 3.0, "anoLetivo", "2026");

        assertEquals(3L, recalcularAna());
    }

    @Test
    public void trilhaExcluidaComAtivoFalsoNaoConta() {
        banco.ler("atividades", "t1").put("ativo", false);
        entrega("e1", "t1", 9.0, "anoLetivo", "2026");

        assertEquals(0L, recalcularAna());
    }

    @Test
    public void trilhaSemCampoAtivoConta() {
        banco.ler("atividades", "t1").remove("ativo");
        entrega("e1", "t1", 9.0, "anoLetivo", "2026");

        assertEquals(9L, recalcularAna());
    }

    @Test
    public void disciplinaExcluidaNaoConta() {
        banco.ler("disciplinas", "mat").put("ativo", false);
        entrega("e1", "t1", 9.0, "anoLetivo", "2026");

        assertEquals(0L, recalcularAna());
    }

    @Test
    public void registroSemAnoLetivoContaSeADataCaiNoPeriodoDosBimestres() throws ParseException {
        // Entrega: vale "enviadoEm"; penalidade: vale o texto de "data"
        entrega("dentro", "t1", 5.0, "enviadoEm", momento("15/03/2026 10:00"));
        entrega("fora", "t2", 7.0, "enviadoEm", momento("20/01/2026 10:00"));
        penalidade("p1", "data", "10/03/2026");
        penalidade("p2", "data", "25/12/2026");

        assertEquals(0L, recalcularAna());
    }

    @Test
    public void entregaSemAnoLetivoESemEnvioUsaADataDaAvaliacao() throws ParseException {
        entrega("e1", "t1", 6.0, "avaliadoEm", momento("15/03/2026 10:00"));

        assertEquals(6L, recalcularAna());
    }

    @Test
    public void semAnoLetivoDefinidoEsemZeramentoTudoConta() {
        banco.gravar("configuracoes", "geral", mapa());
        entrega("e1", "t1", 9.0, "anoLetivo", "2019");
        penalidade("p1");

        assertEquals(4L, recalcularAna());
    }

    @Test
    public void semAnoLetivoDefinidoContaSoODepoisDoZeramento() throws ParseException {
        banco.gravar("configuracoes", "geral", mapa("pontuacaoZeradaEm", momento("01/06/2026 00:00")));
        entrega("antes", "t1", 9.0, "avaliadoEm", momento("10/05/2026 10:00"));
        entrega("depois", "t2", 3.0, "avaliadoEm", momento("10/07/2026 10:00"));
        penalidade("pAntes", "criadoEm", momento("10/05/2026 10:00"));
        penalidade("pDepois", "criadoEm", momento("10/07/2026 10:00"));
        penalidade("pSemData");

        assertEquals(-2L, recalcularAna());
    }

    @Test
    public void recalcularSubstituiAPontuacaoAntiga() {
        banco.ler("alunos", "ana").put("pontuacao", 999L);
        entrega("e1", "t1", 2.0, "anoLetivo", "2026");

        assertEquals(2L, recalcularAna());
    }

    @Test
    public void alunoNuloSoChamaOCallback() {
        AtomicBoolean chamado = new AtomicBoolean(false);

        PontuacaoUtil.recalcular(banco.db, null, () -> chamado.set(true));

        assertTrue(chamado.get());
    }

    @Test
    public void callbackRodaDepoisDeGravar() {
        entrega("e1", "t1", 5.0, "anoLetivo", "2026");
        long[] pontuacaoNoCallback = {Long.MIN_VALUE};

        PontuacaoUtil.recalcular(banco.db, "ana",
                () -> pontuacaoNoCallback[0] = (Long) banco.ler("alunos", "ana").get("pontuacao"));
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(5L, pontuacaoNoCallback[0]);
    }

    @Test
    public void callbackRodaMesmoSeAGravacaoFalhar() {
        AtomicBoolean chamado = new AtomicBoolean(false);

        // "bruno" não tem documento em "alunos": o update falha
        PontuacaoUtil.recalcular(banco.db, "bruno", () -> chamado.set(true));
        shadowOf(Looper.getMainLooper()).idle();

        assertTrue(chamado.get());
    }

    @Test
    public void recalcularTodosAtualizaCadaAluno() {
        banco.gravar("alunos", "bia", mapa("nome", "Bia", "pontuacao", 50L));
        entrega("e1", "t1", 7.0, "anoLetivo", "2026");

        PontuacaoUtil.recalcularTodos(banco.db);
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(7L, banco.ler("alunos", "ana").get("pontuacao"));
        assertEquals(0L, banco.ler("alunos", "bia").get("pontuacao"));
    }
}
