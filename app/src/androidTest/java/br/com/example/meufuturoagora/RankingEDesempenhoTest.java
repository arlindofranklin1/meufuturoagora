package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onData;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.Aula;
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.Frequencia;
import br.com.example.meufuturoagora.dados.LinhaRanking;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Turma;

// Módulo 7 da Fase 5: ranking e desempenho. A pontuação e a ordem são do
// servidor (pgTAP e E2E); aqui se confere o que a tela mostra de cada resposta.
@RunWith(AndroidJUnit4.class)
public class RankingEDesempenhoTest {

    private RepositoriosFalsos falsos;

    @Before
    public void preparar() {
        falsos = new RepositoriosFalsos();
        falsos.instalar();
    }

    @After
    public void encerrar() {
        Servicos.substituir(null);
    }

    private static Intent da(Class<?> tela, String perfil) {
        return new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), tela)
                .putExtra("perfil", perfil);
    }

    private static LinhaRanking linha(String id, String nome, int pontos, int posicao, boolean euMesmo) {
        return new LinhaRanking(id, nome, null, null, pontos, posicao, euMesmo);
    }

    // ===== Ranking =====

    @Test
    public void professorVeATurmaInteiraEOPodio() {
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula", "professor", false, null);
        falsos.turmas.add(new Turma("t1", "1º ano A", 1, true));
        falsos.linhasRanking.add(linha("s1", "Ana", 30, 1, false));
        falsos.linhasRanking.add(linha("s2", "Bruno", 20, 2, false));
        falsos.linhasRanking.add(linha("s3", "Carla", 10, 3, false));
        falsos.linhasRanking.add(linha("s4", "Davi", 5, 4, false));

        ActivityScenario.launch(da(RankingActivity.class, RankingActivity.PERFIL_PROFESSOR));

        Espera.ate(() -> onView(withId(R.id.tvNomePrimeiro)).check(matches(withText("Ana"))));
        onView(withId(R.id.tvPontosPrimeiro)).check(matches(withText("30 pts")));
        onView(withId(R.id.tvNomeSegundo)).check(matches(withText("Bruno")));
        onView(withId(R.id.tvNomeTerceiro)).check(matches(withText("Carla")));
        // A lista abaixo do pódio tem a turma toda, inclusive o 4º
        onView(allOf(withId(R.id.tvNomeAluno), withText("Davi"))).check(matches(isDisplayed()));
        assertTrue(falsos.chamadas.toString(), falsos.chamou("ranking.ranking t1"));
    }

    @Test
    public void professorTrocaDeTurmaERecarregaORanking() {
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula", "professor", false, null);
        falsos.turmas.add(new Turma("t1", "1º ano A", 1, true));
        falsos.turmas.add(new Turma("t2", "2º ano B", 2, true));
        falsos.linhasRanking.add(linha("s1", "Ana", 30, 1, false));

        ActivityScenario.launch(da(RankingActivity.class, RankingActivity.PERFIL_PROFESSOR));
        Espera.ate(() -> assertTrue(falsos.chamou("ranking.ranking t1")));

        onView(withId(R.id.spinnerTurmaRanking)).perform(click());
        onData(allOf(is(instanceOf(String.class)), is("2º ano B"))).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("ranking.ranking t2")));
    }

    @Test
    public void alunoVeOPodioESuaPosicaoComoVoceNaQuartaLinha() {
        falsos.eu = RepositoriosFalsos.perfil("s9", "Eva", "aluno", false, "t1");
        falsos.turmas.add(new Turma("t1", "1º ano A", 1, true));
        falsos.linhasRanking.add(linha("s1", "Ana", 30, 1, false));
        falsos.linhasRanking.add(linha("s2", "Bruno", 20, 2, false));
        falsos.linhasRanking.add(linha("s3", "Carla", 10, 3, false));
        falsos.linhasRanking.add(linha("s9", "Eva", 2, 7, true));

        ActivityScenario.launch(da(RankingActivity.class, RankingActivity.PERFIL_ALUNO));

        Espera.ate(() -> onView(withId(R.id.tvNomePrimeiro)).check(matches(withText("Ana"))));
        // O aluno não troca de turma e a própria linha aparece como "Você"
        onView(withId(R.id.spinnerTurmaRanking)).check(matches(not(isEnabled())));
        onView(withText("Você")).check(matches(isDisplayed()));
        onView(withText("7")).check(matches(isDisplayed()));
        onView(withText("2 pts")).check(matches(isDisplayed()));
    }

    @Test
    public void alunoNoPodioAparecePeloNomeEAindaComoVoce() {
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana", "aluno", false, "t1");
        falsos.turmas.add(new Turma("t1", "1º ano A", 1, true));
        falsos.linhasRanking.add(linha("s1", "Ana", 30, 1, true));
        falsos.linhasRanking.add(linha("s2", "Bruno", 20, 2, false));

        ActivityScenario.launch(da(RankingActivity.class, RankingActivity.PERFIL_ALUNO));

        Espera.ate(() -> onView(withId(R.id.tvNomePrimeiro)).check(matches(withText("Ana"))));
        onView(withText("Você")).check(matches(isDisplayed()));
    }

    @Test
    public void turmaSemAlunosMostraOAviso() {
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula", "professor", false, null);
        falsos.turmas.add(new Turma("t1", "1º ano A", 1, true));

        ActivityScenario.launch(da(RankingActivity.class, RankingActivity.PERFIL_PROFESSOR));

        Espera.ate(() -> onView(withId(R.id.tvSemDados)).check(matches(isDisplayed())));
    }

    // ===== Desempenho =====

    // Bimestre 4 em volta de hoje (é o selecionado ao abrir) e o 1º no começo do ano
    private void anoComBimestres() {
        LocalDate hoje = LocalDate.now();
        falsos.anoAtual = new AnoLetivo(hoje.getYear(), true, OffsetDateTime.now(), null);
        falsos.bimestres.add(new Bimestre(hoje.getYear(), 4, hoje.minusDays(30), hoje.plusDays(30)));
    }

    private Atividade trilhaNoBimestreAtual() {
        return new Atividade("a1", "d1", "Frações", "", 10, LocalDate.now().plusDays(5), null, null, null,
                LocalDate.now().getYear(), 4, OffsetDateTime.now());
    }

    @Test
    public void alunoVeSeusPontosFaltasEOsPontosPorDisciplina() {
        anoComBimestres();
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana", "aluno", false, "t1");
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d1", "Matemática", "t1", "p1"));
        falsos.atividades.add(trilhaNoBimestreAtual());
        // entrega(...) avaliada vale 8.0
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", null, true));
        falsos.aulas.add(new Aula("au1", "d1", LocalDate.now(), 1, "", 4));
        falsos.aulas.add(new Aula("au2", "d1", LocalDate.now().minusDays(1), 1, "", 4));
        falsos.frequencias.add(new Frequencia("au1", "s1", Frequencia.FALTA));
        falsos.frequencias.add(new Frequencia("au2", "s1", Frequencia.FALTA_JUSTIFICADA));

        ActivityScenario.launch(da(DesempenhoActivity.class, DesempenhoActivity.PERFIL_ALUNO));

        Espera.ate(() -> onView(withId(R.id.tvPontosBimestre)).check(matches(withText("8 pts"))));
        onView(withId(R.id.tvFaltasBimestre)).check(matches(withText("1")));
        onView(withId(R.id.tvJustificadasBimestre)).check(matches(withText("1")));
        onView(withId(R.id.tvTituloBimestre)).check(matches(withText("4º bimestre")));
        onView(withText("Matemática")).check(matches(isDisplayed()));
        onView(withText("1 de 1 atividade(s) avaliada(s)")).check(matches(isDisplayed()));
        onView(withId(R.id.tvPontosDesempenho)).check(matches(withText("8 pts")));
    }

    @Test
    public void bimestreSemPeriodoDefinidoMostraOAvisoESemPontos() {
        anoComBimestres();
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana", "aluno", false, "t1");
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d1", "Matemática", "t1", "p1"));
        falsos.atividades.add(trilhaNoBimestreAtual());
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", null, true));

        ActivityScenario.launch(da(DesempenhoActivity.class, DesempenhoActivity.PERFIL_ALUNO));
        Espera.ate(() -> onView(withId(R.id.tvPontosBimestre)).check(matches(withText("8 pts"))));

        onView(withId(R.id.abaBimestre2)).perform(click());

        onView(withId(R.id.tvTituloBimestre)).check(matches(withText("2º bimestre")));
        onView(withId(R.id.tvPeriodoBimestreDesempenho)).check(matches(withText("Período não definido")));
        onView(withId(R.id.tvPontosBimestre)).check(matches(withText("0 pts")));
    }

    @Test
    public void professorVePontosEFaltasDeCadaAlunoDasSuasDisciplinas() {
        anoComBimestres();
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula", "professor", false, null);
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d1", "Matemática", "t1", "p1"));
        // Disciplina de outro professor: não entra na conta
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d2", "Física", "t1", "outro"));
        falsos.participantes.add(new Participante("s2", "Bruno", "aluno", null, null, null));
        falsos.participantes.add(new Participante("s1", "Ana", "aluno", null, null, null));
        falsos.participantes.add(new Participante("p1", "Paula", "professor", null, null, null));
        falsos.atividades.add(trilhaNoBimestreAtual());
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", null, true));
        falsos.aulas.add(new Aula("au1", "d1", LocalDate.now(), 1, "", 4));
        falsos.frequencias.add(new Frequencia("au1", "s2", Frequencia.FALTA));

        ActivityScenario.launch(da(DesempenhoActivity.class, DesempenhoActivity.PERFIL_PROFESSOR));

        // Por nome: Ana antes de Bruno; a professora não aparece na lista
        Espera.ate(() -> onView(withText("Ana")).check(matches(isDisplayed())));
        onView(withText("8")).check(matches(isDisplayed()));
        onView(withText("0 faltas · 0 justificada(s)")).check(matches(isDisplayed()));
        onView(withText("Bruno")).check(matches(isDisplayed()));
        onView(withText("1 falta · 0 justificada(s)")).check(matches(isDisplayed()));
        onView(withText("Paula")).check(androidx.test.espresso.assertion.ViewAssertions.doesNotExist());
    }

    @Test
    public void semAnoLetivoDefinidoNaoQuebraETodosOsBimestresFicamSemPeriodo() {
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana", "aluno", false, "t1");

        ActivityScenario.launch(da(DesempenhoActivity.class, DesempenhoActivity.PERFIL_ALUNO));

        Espera.ate(() -> onView(withId(R.id.tvSemDadosDesempenho)).check(matches(isDisplayed())));
        onView(withId(R.id.tvPeriodoBimestreDesempenho)).check(matches(withText("Período não definido")));
    }
}
