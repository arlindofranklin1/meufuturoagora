package br.com.example.meufuturoagora;

import static br.com.example.meufuturoagora.DocumentoFalso.mapa;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.os.Looper;

import com.google.firebase.firestore.FieldValue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.Map;

// Caracterização de PenalidadeFaltaUtil: no máximo uma penalidade de 5 pontos por
// aluno e dia, se houver ao menos uma "Falta" (a justificada não desconta).
@RunWith(RobolectricTestRunner.class)
public class PenalidadeFaltaUtilTest {

    private FirestoreFalso banco;

    @Before
    public void prepararBanco() throws Exception {

        EstadoAnoLetivo.limpar();
        AnoLetivoUtil.definirAnoAtual("2026");

        banco = new FirestoreFalso();
        banco.gravar("configuracoes", "geral", mapa("anoLetivo", "2026"));
        banco.gravar("alunos", "ana", mapa("nome", "Ana", "pontuacao", 0L));
    }

    private void frequencia(String aula, String data, String status) {
        banco.gravar("frequencias", aula + "_ana",
                mapa("aulaId", aula, "alunoId", "ana", "data", data, "status", status));
    }

    private void recalcular(String data) {
        PenalidadeFaltaUtil.recalcular(banco.db, "ana", data);
        shadowOf(Looper.getMainLooper()).idle();
    }

    private Object pontuacao() {
        return banco.ler("alunos", "ana").get("pontuacao");
    }

    @Test
    public void faltaCriaPenalidadeDoDiaERecalculaAPontuacao() {
        frequencia("aula1", "10/03/2026", "Falta");

        recalcular("10/03/2026");

        Map<String, Object> penalidade = banco.ler("penalidades", "ana_10032026");
        assertNotNull(penalidade);
        assertEquals("ana", penalidade.get("alunoId"));
        assertEquals("10/03/2026", penalidade.get("data"));
        assertEquals(-5, penalidade.get("pontos"));
        assertEquals("2026", penalidade.get("anoLetivo"));
        assertTrue(penalidade.get("criadoEm") instanceof FieldValue);
        assertFalse(penalidade.containsKey("bimestre"));
        assertEquals(-5L, pontuacao());
    }

    @Test
    public void faltaJustificadaNaoDesconta() {
        frequencia("aula1", "10/03/2026", "Falta justificada");

        recalcular("10/03/2026");

        assertEquals(0, banco.quantidade("penalidades"));
        assertEquals(0L, pontuacao());
    }

    @Test
    public void duasFaltasNoMesmoDiaDescontamUmaVezSo() {
        frequencia("aula1", "10/03/2026", "Falta");
        frequencia("aula2", "10/03/2026", "Falta");

        recalcular("10/03/2026");

        assertEquals(1, banco.quantidade("penalidades"));
        assertEquals(-5L, pontuacao());
    }

    @Test
    public void umaFaltaEUmaPresencaNoMesmoDiaDescontam() {
        frequencia("aula1", "10/03/2026", "Presente");
        frequencia("aula2", "10/03/2026", "Falta");

        recalcular("10/03/2026");

        assertEquals(1, banco.quantidade("penalidades"));
    }

    @Test
    public void faltaEmOutroDiaNaoEntraNaConta() {
        frequencia("aula1", "11/03/2026", "Falta");

        recalcular("10/03/2026");

        assertEquals(0, banco.quantidade("penalidades"));
    }

    @Test
    public void corrigirFaltaParaPresencaDevolveOsPontos() {
        banco.gravar("penalidades", "ana_10032026",
                mapa("alunoId", "ana", "data", "10/03/2026", "pontos", -5, "anoLetivo", "2026"));
        banco.ler("alunos", "ana").put("pontuacao", -5L);
        frequencia("aula1", "10/03/2026", "Presente");

        recalcular("10/03/2026");

        assertNull(banco.ler("penalidades", "ana_10032026"));
        assertEquals(0L, pontuacao());
    }

    @Test
    public void penalidadeJaExistenteNaoEhDuplicadaNemRecalcula() {
        banco.gravar("penalidades", "ana_10032026",
                mapa("alunoId", "ana", "data", "10/03/2026", "pontos", -5, "anoLetivo", "2026"));
        banco.ler("alunos", "ana").put("pontuacao", 123L);
        frequencia("aula1", "10/03/2026", "Falta");

        recalcular("10/03/2026");

        assertEquals(1, banco.quantidade("penalidades"));
        assertEquals(123L, pontuacao());
    }

    @Test
    public void semFrequenciaNoDiaNaoFazNada() {
        recalcular("10/03/2026");

        assertEquals(0, banco.quantidade("penalidades"));
        assertEquals(0L, pontuacao());
    }

    @Test
    public void dataOuAlunoAusenteNaoFazNada() {
        frequencia("aula1", "10/03/2026", "Falta");

        recalcular(null);
        recalcular("");
        PenalidadeFaltaUtil.recalcular(banco.db, null, "10/03/2026");
        shadowOf(Looper.getMainLooper()).idle();

        assertEquals(0, banco.quantidade("penalidades"));
    }

    @Test
    public void statusComOutraGrafiaNaoEhFalta() {
        // A comparação é exata: "falta" minúsculo não desconta
        frequencia("aula1", "10/03/2026", "falta");

        recalcular("10/03/2026");

        assertEquals(0, banco.quantidade("penalidades"));
    }
}
