package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDescendantOfA;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.HistoricoAluno;
import br.com.example.meufuturoagora.dados.HistoricoEntrega;
import br.com.example.meufuturoagora.dados.HistoricoTrilha;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Turma;

// Módulo 3 da Fase 5: ano letivo (sem senha, decisão N5), bimestres, criação
// de disciplina pelo professor (D1) e histórico do admin. Repositórios falsos.
@RunWith(AndroidJUnit4.class)
public class EstruturaEscolarTest {

    private RepositoriosFalsos falsos;

    @Before
    public void preparar() {
        falsos = new RepositoriosFalsos();
        falsos.turmas.add(new Turma("t1", "6º ano A", 6, true));
        falsos.instalar();
    }

    @After
    public void encerrar() {
        Servicos.substituir(null);
    }

    private void anoAtual(int ano) {
        falsos.anoAtual = new AnoLetivo(ano, true, OffsetDateTime.now(), null);
    }

    private static ViewInteraction inicioDoBimestre(int cartao) {
        return onView(allOf(withId(R.id.edtPrazoInicial), isDescendantOfA(withId(cartao))));
    }

    private static ViewInteraction fimDoBimestre(int cartao) {
        return onView(allOf(withId(R.id.edtPrazoFinal), isDescendantOfA(withId(cartao))));
    }

    // ===== Ano letivo =====

    @Test
    public void trocarOAnoLetivoNaoPedeSenhaEConfirma() {
        anoAtual(2026);

        ActivityScenario.launch(DefinirAnoLetivoActivity.class);

        Espera.ate(() -> onView(withId(R.id.edtAnoLetivo)).check(matches(withText("2026"))));
        onView(withId(R.id.edtAnoLetivo)).perform(replaceText("2027"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarAnoLetivo)).perform(click());

        onView(withText("Confirmar ano letivo")).check(matches(isDisplayed()));
        onView(withText(containsString("de 2026 para 2027"))).check(matches(isDisplayed()));
        // Decisão N5: sem senha do administrador
        onView(instanceOf(EditText.class)).inRoot(isDialog()).check(doesNotExist());

        onView(withText("Confirmar")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("escola.definirAnoLetivo 2027")));
    }

    @Test
    public void mesmoAnoLetivoNaoChamaOServidor() {
        anoAtual(2026);

        ActivityScenario.launch(DefinirAnoLetivoActivity.class);

        Espera.ate(() -> onView(withId(R.id.edtAnoLetivo)).check(matches(withText("2026"))));
        onView(withId(R.id.btnSalvarAnoLetivo)).perform(click());

        onView(withText("Confirmar ano letivo")).check(doesNotExist());
        assertEquals(0, falsos.chamadas.size());
    }

    @Test
    public void anoComMenosDeQuatroDigitosOuForaDoIntervaloNaoChamaOServidor() {
        ActivityScenario.launch(DefinirAnoLetivoActivity.class);

        onView(withId(R.id.edtAnoLetivo)).perform(replaceText("26"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarAnoLetivo)).perform(click());
        onView(withId(R.id.edtAnoLetivo)).perform(replaceText("1999"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarAnoLetivo)).perform(click());

        assertEquals(0, falsos.chamadas.size());
    }

    @Test
    public void primeiroAnoLetivoNaoFalaEmTroca() {
        ActivityScenario.launch(DefinirAnoLetivoActivity.class);

        onView(withId(R.id.edtAnoLetivo)).perform(replaceText("2026"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarAnoLetivo)).perform(click());

        onView(withText("Definir 2026 como ano letivo?")).check(matches(isDisplayed()));
        onView(withText("Confirmar")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamou("escola.definirAnoLetivo 2026")));
    }

    // ===== Bimestres =====

    @Test
    public void bimestresDoAnoVoltamPreenchidos() {
        anoAtual(2026);
        falsos.bimestres.add(new Bimestre(2026, 1, LocalDate.of(2026, 2, 2), LocalDate.of(2026, 4, 30)));
        falsos.bimestres.add(new Bimestre(2026, 2, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 7, 10)));

        ActivityScenario.launch(DefinirBimestresActivity.class);

        Espera.ate(() -> inicioDoBimestre(R.id.bimestre1).check(matches(withText("02/02/2026"))));
        fimDoBimestre(R.id.bimestre2).check(matches(withText("10/07/2026")));
        inicioDoBimestre(R.id.bimestre3).check(matches(withText("")));
        Captura.salvar("30_bimestres");
    }

    @Test
    public void bimestresValidosSaoEnviadosComOAno() {
        anoAtual(2026);

        ActivityScenario.launch(DefinirBimestresActivity.class);

        Espera.ate(() -> inicioDoBimestre(R.id.bimestre1).check(matches(isDisplayed())));
        inicioDoBimestre(R.id.bimestre1).perform(replaceText("02/02/2026"));
        fimDoBimestre(R.id.bimestre1).perform(replaceText("30/04/2026"));
        inicioDoBimestre(R.id.bimestre2).perform(replaceText("04/05/2026"));
        fimDoBimestre(R.id.bimestre2).perform(replaceText("10/07/2026"));
        onView(withId(R.id.btnSalvarBimestres)).perform(scrollTo(), click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("escola.definirBimestres 2026 2")));
    }

    @Test
    public void bimestresSobrepostosNaoChamamOServidor() {
        anoAtual(2026);

        ActivityScenario.launch(DefinirBimestresActivity.class);

        Espera.ate(() -> inicioDoBimestre(R.id.bimestre1).check(matches(isDisplayed())));
        inicioDoBimestre(R.id.bimestre1).perform(replaceText("02/02/2026"));
        fimDoBimestre(R.id.bimestre1).perform(replaceText("10/05/2026"));
        inicioDoBimestre(R.id.bimestre2).perform(replaceText("04/05/2026"));
        fimDoBimestre(R.id.bimestre2).perform(replaceText("10/07/2026"));
        onView(withId(R.id.btnSalvarBimestres)).perform(scrollTo(), click());

        assertEquals(0, falsos.chamadas.size());
    }

    @Test
    public void bimestresSemAnoLetivoNaoChamamOServidor() {
        ActivityScenario.launch(DefinirBimestresActivity.class);

        inicioDoBimestre(R.id.bimestre1).perform(replaceText("02/02/2026"));
        fimDoBimestre(R.id.bimestre1).perform(replaceText("30/04/2026"));
        onView(withId(R.id.btnSalvarBimestres)).perform(scrollTo(), click());

        assertEquals(0, falsos.chamadas.size());
    }

    // ===== Criar disciplina =====

    @Test
    public void professorCriaDisciplinaNaTurmaEscolhida() {
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula", Perfil.PROFESSOR, false, null);

        ActivityScenario.launch(CriarDisciplinaActivity.class);

        Espera.ate(() -> onView(withId(R.id.spinnerTurmaDisciplina)).check(matches(withSpinnerText("6º ano A"))));
        onView(withId(R.id.edtNomeDisciplina)).perform(replaceText("Física"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarDisciplina)).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("disciplinas.criar Física t1")));
    }

    @Test
    public void disciplinaSemNomeNaoChamaOServidor() {
        ActivityScenario.launch(CriarDisciplinaActivity.class);

        Espera.ate(() -> onView(withId(R.id.spinnerTurmaDisciplina)).check(matches(withSpinnerText("6º ano A"))));
        onView(withId(R.id.btnSalvarDisciplina)).perform(click());

        assertEquals(0, falsos.chamadas.size());
    }

    // ===== Histórico =====

    private void historicoComDados() {

        anoAtual(2026);
        falsos.historicoAnos.add(2026);
        falsos.bimestres.add(new Bimestre(2026, 2, LocalDate.of(2026, 5, 4), LocalDate.of(2026, 7, 10)));

        falsos.historicoAlunos.add(new HistoricoAluno("a1", "Ana Souza", "6º ano A", 2, 1, 14));
        falsos.historicoAlunos.add(new HistoricoAluno("a2", "Bruno Lima", null, 0, 2, -10));

        falsos.historicoTrilhas.add(new HistoricoTrilha("t1", "Frações", "Matemática", "Paula", "6º ano A",
                LocalDate.of(2026, 11, 5), 10, 3, OffsetDateTime.now()));

        falsos.historicoEntregas.add(new HistoricoEntrega("e1", "Ana Souza", "Frações", "Matemática", "6º ano A",
                OffsetDateTime.now(), 3, 8.5));
        falsos.historicoEntregas.add(new HistoricoEntrega("e2", "Bruno Lima", "Frações", "Matemática", null,
                OffsetDateTime.now(), 0, null));
    }

    @Test
    public void historicoMostraOsAlunosAgrupadosPorTurma() {
        historicoComDados();

        ActivityScenario.launch(HistoricoAnoLetivoActivity.class);

        Espera.ate(() -> onView(withText("Ana Souza")).check(matches(isDisplayed())));
        onView(withText("6º ano A")).check(matches(isDisplayed()));
        onView(withText("Sem turma")).check(matches(isDisplayed()));
        onView(withText("14 pts")).check(matches(isDisplayed()));
        onView(withText("2 entrega(s) • 1 dia(s) com falta")).check(matches(isDisplayed()));
        onView(withId(R.id.tvResumoHistorico)).check(matches(withText("2 registros em 2026 — ano inteiro")));
        assertTrue(falsos.chamadas.toString(), falsos.chamou("historico.alunos 2026 0"));
        Captura.salvar("31_historico_alunos");
    }

    @Test
    public void pesquisaFiltraSemConsultarOServidorDeNovo() {
        historicoComDados();

        ActivityScenario.launch(HistoricoAnoLetivoActivity.class);

        Espera.ate(() -> onView(withText("Ana Souza")).check(matches(isDisplayed())));
        onView(withId(R.id.edtBuscarHistorico)).perform(replaceText("bruno"), closeSoftKeyboard());

        Espera.ate(() -> onView(withText("Ana Souza")).check(doesNotExist()));
        onView(withText("Bruno Lima")).check(matches(isDisplayed()));
        assertEquals("só a consulta inicial", 1, falsos.chamadas.size());
    }

    @Test
    public void trocarOBimestrePedeOPeriodoAoServidor() {
        historicoComDados();

        ActivityScenario.launch(HistoricoAnoLetivoActivity.class);

        Espera.ate(() -> onView(withText("Ana Souza")).check(matches(isDisplayed())));
        onView(withId(R.id.spinnerBimestreHistorico)).perform(click());
        onView(withText("2º bimestre")).inRoot(isPlatformPopup()).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("historico.alunos 2026 2")));
        Espera.ate(() -> onView(withId(R.id.tvResumoHistorico))
                .check(matches(withText("2 registros em 2026 — 2º bimestre (04/05/2026 a 10/07/2026)"))));
    }

    @Test
    public void abaDeTrilhasMostraDisciplinaProfessorEPrazo() {
        historicoComDados();

        ActivityScenario.launch(HistoricoAnoLetivoActivity.class);

        Espera.ate(() -> onView(withText("Ana Souza")).check(matches(isDisplayed())));
        onView(withId(R.id.abaTrilhas)).perform(click());

        Espera.ate(() -> onView(withText("Frações")).check(matches(isDisplayed())));
        onView(withText("Matemática • Prof. Paula\nPrazo: 05/11/2026 • 3 entrega(s)")).check(matches(isDisplayed()));
        onView(withText("10 pts")).check(matches(isDisplayed()));
        assertTrue(falsos.chamou("historico.trilhas 2026 0"));
    }

    @Test
    public void abaDeEntregasMostraNotaPendenteEAtraso() {
        historicoComDados();

        ActivityScenario.launch(HistoricoAnoLetivoActivity.class);

        Espera.ate(() -> onView(withText("Ana Souza")).check(matches(isDisplayed())));
        onView(withId(R.id.abaEntregas)).perform(click());

        Espera.ate(() -> onView(withText("8.5 pts")).check(matches(isDisplayed())));
        onView(withText("Pendente")).check(matches(isDisplayed()));
        onView(withText(containsString("3 dia(s) de atraso"))).check(matches(isDisplayed()));
        assertTrue(falsos.chamou("historico.entregas 2026 0"));
    }

    @Test
    public void periodoSemRegistrosAvisa() {
        anoAtual(2026);
        falsos.historicoAnos.add(2026);

        ActivityScenario.launch(HistoricoAnoLetivoActivity.class);

        Espera.ate(() -> onView(withId(R.id.tvResumoHistorico))
                .check(matches(withText("Nenhum registro em 2026 — ano inteiro"))));
    }
}
