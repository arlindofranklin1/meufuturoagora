package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.ViewInteraction;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.LocalDate;

import br.com.example.meufuturoagora.dados.Aula;
import br.com.example.meufuturoagora.dados.Frequencia;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Servicos;

// Módulo 4 da Fase 5: cadastro de aula, lista de aulas (com exclusão) e
// frequência. O desconto de pontos por falta é do servidor (pgTAP 060 e 120).
@RunWith(AndroidJUnit4.class)
public class AulasEFrequenciaTest {

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

    private static Intent daDisciplina(Class<?> tela) {
        return new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), tela)
                .putExtra("disciplinaId", "d1");
    }

    private static Intent daAula() {
        return daDisciplina(FrequenciaAulaActivity.class)
                .putExtra("aulaId", "au1")
                .putExtra("aulaDescricao", "Frações");
    }

    private static Participante participante(String id, String nome, String tipo) {
        return new Participante(id, nome, tipo, null, null, null);
    }

    private static ViewInteraction statusDe(String nome) {
        return onView(allOf(withId(R.id.spinnerStatusFrequencia), withParent(hasDescendant(withText(nome)))));
    }

    // ===== Cadastrar aula =====

    @Test
    public void aulaCadastradaVaiComDataQuantidadeEDescricao() {
        ActivityScenario.launch(daDisciplina(CadastrarAulaActivity.class));

        onView(withId(R.id.edtQuantidadeAulas)).perform(replaceText("2"));
        onView(withId(R.id.edtDescricaoAula)).perform(replaceText("Frações equivalentes"), closeSoftKeyboard());
        onView(withId(R.id.edtDataAula)).perform(replaceText("20/10/2026"));
        onView(withId(R.id.btnSalvarAula)).perform(scrollTo(), click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(),
                falsos.chamou("frequencias.cadastrarAula d1 2026-10-20 2")));
    }

    @Test
    public void quantidadeForaDeUmAVinteNaoChamaOServidor() {
        ActivityScenario.launch(daDisciplina(CadastrarAulaActivity.class));

        onView(withId(R.id.edtDataAula)).perform(replaceText("20/10/2026"));

        for (String invalida : new String[]{"0", "21", "-1"}) {
            onView(withId(R.id.edtQuantidadeAulas)).perform(replaceText(invalida), closeSoftKeyboard());
            onView(withId(R.id.btnSalvarAula)).perform(scrollTo(), click());
        }

        assertEquals(0, falsos.chamadas.size());
    }

    @Test
    public void semDataOuComDataImpossivelNaoChamaOServidor() {
        ActivityScenario.launch(daDisciplina(CadastrarAulaActivity.class));

        onView(withId(R.id.edtQuantidadeAulas)).perform(replaceText("1"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarAula)).perform(scrollTo(), click());

        onView(withId(R.id.edtDataAula)).perform(replaceText("31/02/2026"));
        onView(withId(R.id.btnSalvarAula)).perform(scrollTo(), click());

        assertEquals(0, falsos.chamadas.size());
    }

    // ===== Lista de aulas =====

    @Test
    public void listaMostraDescricaoEDataDasAulas() {
        falsos.aulas.add(new Aula("au1", "d1", LocalDate.of(2026, 10, 20), 2, "Frações", 4));
        falsos.aulas.add(new Aula("au2", "d1", LocalDate.of(2026, 10, 21), 1, "", 4));

        ActivityScenario.launch(daDisciplina(RegistrarFrequenciaActivity.class));

        Espera.ate(() -> onView(withText("Frações")).check(matches(isDisplayed())));
        onView(withText("20/10/2026")).check(matches(isDisplayed()));
        onView(withText("Aula")).check(matches(isDisplayed()));
        onView(withText("21/10/2026")).check(matches(isDisplayed()));
    }

    @Test
    public void listaVaziaAvisa() {
        ActivityScenario.launch(daDisciplina(RegistrarFrequenciaActivity.class));

        Espera.ate(() -> onView(withId(R.id.tvSemAulas)).check(matches(isDisplayed())));
    }

    @Test
    public void excluirAulaAvisaDaFrequenciaEDepoisApaga() {
        falsos.aulas.add(new Aula("au1", "d1", LocalDate.of(2026, 10, 20), 2, "Frações", 4));

        ActivityScenario.launch(daDisciplina(RegistrarFrequenciaActivity.class));

        Espera.ate(() -> onView(withText("Frações")).check(matches(isDisplayed())));
        onView(withId(R.id.btnMenuAula)).perform(click());
        onView(withText("Excluir")).inRoot(isPlatformPopup()).perform(click());

        onView(withText(containsString("recuperam os pontos descontados"))).check(matches(isDisplayed()));
        onView(withText("Excluir")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamou("frequencias.excluirAula au1")));
        Espera.ate(() -> onView(withText("Frações")).check(doesNotExist()));
    }

    @Test
    public void cancelarNaoApagaAAula() {
        falsos.aulas.add(new Aula("au1", "d1", LocalDate.of(2026, 10, 20), 2, "Frações", 4));

        ActivityScenario.launch(daDisciplina(RegistrarFrequenciaActivity.class));

        Espera.ate(() -> onView(withText("Frações")).check(matches(isDisplayed())));
        onView(withId(R.id.btnMenuAula)).perform(click());
        onView(withText("Excluir")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Cancelar")).perform(click());

        assertEquals(0, falsos.chamadas.size());
    }

    // ===== Frequência =====

    private void turmaComDoisAlunos() {
        falsos.participantes.add(participante("p1", "Paula", "professor"));
        falsos.participantes.add(participante("a1", "Ana", "aluno"));
        falsos.participantes.add(participante("a2", "Bruno", "aluno"));
    }

    @Test
    public void frequenciaListaSoOsAlunosEComecaEmPresente() {
        turmaComDoisAlunos();

        ActivityScenario.launch(daAula());

        Espera.ate(() -> onView(withText("Ana")).check(matches(isDisplayed())));
        onView(withText("Bruno")).check(matches(isDisplayed()));
        onView(withText("Paula")).check(doesNotExist());
        statusDe("Ana").check(matches(withSpinnerText("Presente")));
        statusDe("Bruno").check(matches(withSpinnerText("Presente")));
        Captura.salvar("40_frequencia_aula");
    }

    @Test
    public void frequenciaJaSalvaVoltaComOStatusCerto() {
        turmaComDoisAlunos();
        falsos.frequencias.add(new Frequencia("au1", "a1", Frequencia.FALTA));
        falsos.frequencias.add(new Frequencia("au1", "a2", Frequencia.FALTA_JUSTIFICADA));
        // Frequência de outra aula não interfere
        falsos.frequencias.add(new Frequencia("au2", "a1", Frequencia.PRESENTE));

        ActivityScenario.launch(daAula());

        Espera.ate(() -> statusDe("Ana").check(matches(withSpinnerText("Falta"))));
        statusDe("Bruno").check(matches(withSpinnerText("Falta justificada")));
    }

    @Test
    public void salvarEnviaAListaToda() {
        turmaComDoisAlunos();

        ActivityScenario.launch(daAula());

        Espera.ate(() -> onView(withText("Ana")).check(matches(isDisplayed())));
        statusDe("Bruno").perform(click());
        onView(withText("Falta")).inRoot(isPlatformPopup()).perform(click());
        onView(withId(R.id.btnSalvarFrequencia)).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(),
                falsos.chamou("frequencias.registrar au1 a1=presente a2=falta")));
    }

    @Test
    public void semAlunosNaoChamaOServidor() {
        falsos.participantes.add(participante("p1", "Paula", "professor"));

        ActivityScenario.launch(daAula());

        Espera.ate(() -> onView(withId(R.id.tvSemAlunos)).check(matches(isDisplayed())));
        onView(withId(R.id.btnSalvarFrequencia)).perform(click());

        assertEquals(0, falsos.chamadas.size());
    }
}
