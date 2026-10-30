package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// A conta do desempenho por bimestre: pontos no bimestre do PRAZO da atividade,
// faltas no bimestre da DATA da aula (regra herdada do app antigo).
public class DesempenhoTest {

    private static final double FOLGA = 0.0001;

    private List<Bimestre> bimestres;

    @Before
    public void preparar() {
        bimestres = Arrays.asList(
                new Bimestre(2026, 1, LocalDate.of(2026, 2, 2), LocalDate.of(2026, 4, 30)),
                new Bimestre(2026, 2, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 7, 10)),
                new Bimestre(2026, 4, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 12, 18))
        );
    }

    private static Atividade atividade(String id, String disciplinaId, LocalDate prazo) {
        return new Atividade(id, disciplinaId, "T" + id, "", 10, prazo, null, null, null, 2026, 1,
                OffsetDateTime.now());
    }

    private static Entrega entrega(String atividadeId, String alunoId, Double nota) {
        return new Entrega(atividadeId + alunoId, atividadeId, alunoId, "ok", null, null,
                OffsetDateTime.now(), 0, nota, null, nota != null ? OffsetDateTime.now() : null);
    }

    private static Aula aula(String id, String disciplinaId, LocalDate data) {
        return new Aula(id, disciplinaId, data, 1, "", 1);
    }

    @Test
    public void bimestreDaDataUsaOsPeriodosDefinidos() {
        Desempenho d = new Desempenho(bimestres, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(), Collections.emptyList());

        assertEquals(1, d.bimestreDa(LocalDate.of(2026, 2, 2)));
        assertEquals("o último dia conta", 1, d.bimestreDa(LocalDate.of(2026, 4, 30)));
        assertEquals(2, d.bimestreDa(LocalDate.of(2026, 5, 4)));
        assertEquals("bimestre 3 não foi definido", 0, d.bimestreDa(LocalDate.of(2026, 8, 15)));
        assertEquals(4, d.bimestreDa(LocalDate.of(2026, 12, 18)));
        assertEquals(0, d.bimestreDa(null));
    }

    @Test
    public void pontosEntramNoBimestreDoPrazoESomamSoEntregasAvaliadas() {
        List<Atividade> atividades = Arrays.asList(
                atividade("a1", "d1", LocalDate.of(2026, 3, 10)),
                atividade("a2", "d1", LocalDate.of(2026, 3, 20)),
                atividade("a3", "d1", LocalDate.of(2026, 6, 1)));
        List<Entrega> entregas = Arrays.asList(
                entrega("a1", "s1", 7.5),
                entrega("a2", "s1", null),
                entrega("a3", "s1", 9.0),
                entrega("a1", "s2", 4.0));

        Desempenho d = new Desempenho(bimestres, atividades, entregas, Collections.emptyList(),
                Collections.emptyList());

        assertEquals(7.5, d.doAluno("s1", 1).pontos, FOLGA);
        assertEquals("a entrega sem nota não conta", 7.5, d.doAluno("s1", 1).pontos, FOLGA);
        assertEquals(9.0, d.doAluno("s1", 2).pontos, FOLGA);
        assertEquals(4.0, d.doAluno("s2", 1).pontos, FOLGA);
        assertEquals(0.0, d.doAluno("s1", 4).pontos, FOLGA);
    }

    @Test
    public void atividadeComPrazoForaDosPeriodosNaoContaEmBimestreNenhum() {
        List<Atividade> atividades = Collections.singletonList(
                atividade("a1", "d1", LocalDate.of(2026, 8, 15)));
        List<Entrega> entregas = Collections.singletonList(entrega("a1", "s1", 8.0));

        Desempenho d = new Desempenho(bimestres, atividades, entregas, Collections.emptyList(),
                Collections.emptyList());

        for (int n = 1; n <= 4; n++) {
            assertEquals(0.0, d.doAluno("s1", n).pontos, FOLGA);
        }
    }

    @Test
    public void entregaDeAtividadeForaDaTelaNaoEntra() {
        Desempenho d = new Desempenho(bimestres, Collections.emptyList(),
                Collections.singletonList(entrega("a9", "s1", 10.0)), Collections.emptyList(),
                Collections.emptyList());

        assertEquals(0.0, d.doAluno("s1", 1).pontos, FOLGA);
    }

    @Test
    public void faltasEntramNoBimestreDaAulaEJustificadasFicamSeparadas() {
        List<Aula> aulas = Arrays.asList(
                aula("au1", "d1", LocalDate.of(2026, 3, 3)),
                aula("au2", "d1", LocalDate.of(2026, 3, 4)),
                aula("au3", "d1", LocalDate.of(2026, 6, 2)));
        List<Frequencia> frequencias = Arrays.asList(
                new Frequencia("au1", "s1", Frequencia.FALTA),
                new Frequencia("au2", "s1", Frequencia.FALTA_JUSTIFICADA),
                new Frequencia("au3", "s1", Frequencia.FALTA),
                new Frequencia("au1", "s2", Frequencia.PRESENTE));

        Desempenho d = new Desempenho(bimestres, Collections.emptyList(), Collections.emptyList(), aulas,
                frequencias);

        assertEquals(1, d.doAluno("s1", 1).faltas);
        assertEquals(1, d.doAluno("s1", 1).justificadas);
        assertEquals(1, d.doAluno("s1", 2).faltas);
        assertEquals(0, d.doAluno("s1", 2).justificadas);
        assertEquals("presente não é falta", 0, d.doAluno("s2", 1).faltas);
    }

    @Test
    public void duasFaltasNoMesmoDiaContamDuasNaTela() {
        // O desconto de 5 pontos (uma vez por dia) é do servidor; aqui só se contam as faltas
        List<Aula> aulas = Arrays.asList(
                aula("au1", "d1", LocalDate.of(2026, 3, 3)),
                aula("au2", "d1", LocalDate.of(2026, 3, 3)));
        List<Frequencia> frequencias = Arrays.asList(
                new Frequencia("au1", "s1", Frequencia.FALTA),
                new Frequencia("au2", "s1", Frequencia.FALTA));

        Desempenho d = new Desempenho(bimestres, Collections.emptyList(), Collections.emptyList(), aulas,
                frequencias);

        assertEquals(2, d.doAluno("s1", 1).faltas);
    }

    @Test
    public void frequenciaDeAulaDesconhecidaNaoEntra() {
        Desempenho d = new Desempenho(bimestres, Collections.emptyList(), Collections.emptyList(),
                Collections.emptyList(),
                Collections.singletonList(new Frequencia("sumiu", "s1", Frequencia.FALTA)));

        assertEquals(0, d.doAluno("s1", 1).faltas);
    }

    @Test
    public void porDisciplinaSeparaPontosAvaliadasETotalDeAtividades() {
        List<Atividade> atividades = Arrays.asList(
                atividade("a1", "d1", LocalDate.of(2026, 3, 10)),
                atividade("a2", "d1", LocalDate.of(2026, 3, 20)),
                atividade("a3", "d2", LocalDate.of(2026, 3, 25)),
                atividade("a4", "d1", LocalDate.of(2026, 6, 1)));
        List<Entrega> entregas = Arrays.asList(
                entrega("a1", "s1", 6.0),
                entrega("a3", "s1", 3.0));
        List<Aula> aulas = Arrays.asList(
                aula("au1", "d1", LocalDate.of(2026, 3, 3)),
                aula("au2", "d2", LocalDate.of(2026, 3, 3)));
        List<Frequencia> frequencias = Arrays.asList(
                new Frequencia("au1", "s1", Frequencia.FALTA),
                new Frequencia("au2", "s1", Frequencia.FALTA_JUSTIFICADA));

        Desempenho d = new Desempenho(bimestres, atividades, entregas, aulas, frequencias);

        Desempenho.Resumo d1 = d.daDisciplina("s1", "d1", 1);
        assertEquals(6.0, d1.pontos, FOLGA);
        assertEquals(1, d1.avaliadas);
        assertEquals("a4 tem prazo no 2º bimestre", 2, d1.atividades);
        assertEquals(1, d1.faltas);
        assertEquals(0, d1.justificadas);

        Desempenho.Resumo d2 = d.daDisciplina("s1", "d2", 1);
        assertEquals(3.0, d2.pontos, FOLGA);
        assertEquals(1, d2.atividades);
        assertEquals(1, d2.justificadas);

        Desempenho.Resumo total = d.doAluno("s1", 1);
        assertEquals(9.0, total.pontos, FOLGA);
        assertEquals(1, total.faltas);
        assertEquals(1, total.justificadas);
    }
}
