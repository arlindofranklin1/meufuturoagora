package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.OffsetDateTime;

import br.com.example.meufuturoagora.dados.EntradaDiario;
import br.com.example.meufuturoagora.dados.Servicos;

// Módulo 6 da Fase 5: diário do aluno. Sem tempo real (N2): a lista recarrega
// ao abrir e depois de cada gravação. Quem pode ler é do RLS (pgTAP e E2E).
@RunWith(AndroidJUnit4.class)
public class DiarioTest {

    private RepositoriosFalsos falsos;

    @Before
    public void preparar() {
        falsos = new RepositoriosFalsos();
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana Souza", "aluno", false, "t1");
        falsos.instalar();
    }

    @After
    public void encerrar() {
        Servicos.substituir(null);
    }

    private static EntradaDiario entrada(String id, String texto) {
        return new EntradaDiario(id, texto, OffsetDateTime.parse("2026-10-20T12:00:00Z"),
                OffsetDateTime.parse("2026-10-20T12:00:00Z"));
    }

    private static String textoDaFolha(ViewGroup folhas, int posicao) {
        return ((TextView) folhas.getChildAt(posicao).findViewById(R.id.tvTextoMeta)).getText().toString();
    }

    @Test
    public void mostraSaudacaoComOPrimeiroNomeEAsPaginasMaisNovasPrimeiro() {
        falsos.diario.add(entrada("e1", "Passar no vestibular"));
        falsos.diario.add(entrada("e2", "Aprender violão"));

        ActivityScenario.launch(DiarioAlunoActivity.class);

        Espera.ate(() -> onView(withText("Oi, Ana! Um cantinho só seu para sonhar alto.")).check(matches(isDisplayed())));
        // A mais nova vem primeiro; a mais antiga fica mais abaixo, na rolagem
        onView(withId(R.id.containerMetas)).check((view, semView) -> {
            ViewGroup folhas = (ViewGroup) view;
            assertEquals(2, folhas.getChildCount());
            assertEquals("Aprender violão", textoDaFolha(folhas, 0));
            assertEquals("Passar no vestibular", textoDaFolha(folhas, 1));
        });
    }

    @Test
    public void semPaginasMostraOEstadoVazio() {
        ActivityScenario.launch(DiarioAlunoActivity.class);

        Espera.ate(() -> onView(withId(R.id.layoutDiarioVazio)).check(matches(isDisplayed())));
    }

    @Test
    public void guardarGravaLimpaOCampoEMostraAPagina() {
        ActivityScenario.launch(DiarioAlunoActivity.class);
        Espera.ate(() -> onView(withId(R.id.layoutDiarioVazio)).check(matches(isDisplayed())));

        onView(withId(R.id.etNovaMeta)).perform(replaceText("Fazer intercâmbio"), closeSoftKeyboard());
        onView(withId(R.id.btnGuardarMeta)).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamou("diario.escrever Fazer intercâmbio")));
        Espera.ate(() -> onView(withText("Fazer intercâmbio")).check(matches(isDisplayed())));
        onView(withId(R.id.etNovaMeta)).check(matches(withText("")));
    }

    @Test
    public void textoEmBrancoNaoVaiAoServidor() {
        ActivityScenario.launch(DiarioAlunoActivity.class);
        Espera.ate(() -> onView(withId(R.id.layoutDiarioVazio)).check(matches(isDisplayed())));

        onView(withId(R.id.etNovaMeta)).perform(replaceText("   "), closeSoftKeyboard());
        onView(withId(R.id.btnGuardarMeta)).perform(click());

        assertEquals(0, falsos.chamadas.size());
    }

    @Test
    public void seAGravacaoFalharOTextoDigitadoFicaNoCampo() {
        falsos.erroNoDiario = RepositoriosFalsos.erro(500, "XX000", "falhou");
        ActivityScenario.launch(DiarioAlunoActivity.class);
        Espera.ate(() -> onView(withId(R.id.layoutDiarioVazio)).check(matches(isDisplayed())));

        onView(withId(R.id.etNovaMeta)).perform(replaceText("Não perder isto"), closeSoftKeyboard());
        onView(withId(R.id.btnGuardarMeta)).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamou("diario.escrever Não perder isto")));
        // Nada foi gravado: o texto continua no campo, o botão volta e a lista segue vazia
        Espera.ate(() -> onView(withId(R.id.btnGuardarMeta)).check(matches(isEnabled())));
        onView(withId(R.id.etNovaMeta)).check(matches(withText("Não perder isto")));
        assertEquals(0, falsos.diario.size());
    }

    @Test
    public void editarPaginaTrocaOTexto() {
        falsos.diario.add(entrada("e1", "Passar no vestibular"));
        ActivityScenario.launch(DiarioAlunoActivity.class);
        Espera.ate(() -> onView(withText("Passar no vestibular")).check(matches(isDisplayed())));

        onView(withText("Passar no vestibular")).perform(click());
        onView(instanceOf(EditText.class)).inRoot(isDialog())
                .perform(replaceText("Passar em medicina"), closeSoftKeyboard());
        onView(withText("Salvar")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("diario.editar e1 Passar em medicina")));
        Espera.ate(() -> onView(withText("Passar em medicina")).check(matches(isDisplayed())));
        onView(withText("20 de outubro de 2026")).check(matches(isDisplayed()));
        onView(withText("Passar no vestibular")).check(doesNotExist());
    }

    @Test
    public void apagarPedeConfirmacaoEDepoisSomeAPagina() {
        falsos.diario.add(entrada("e1", "Passar no vestibular"));
        ActivityScenario.launch(DiarioAlunoActivity.class);
        Espera.ate(() -> onView(withText("Passar no vestibular")).check(matches(isDisplayed())));

        onView(withId(R.id.btnApagarMeta)).perform(click());
        onView(withText("Apagar esta página?")).check(matches(isDisplayed()));
        onView(withText("Cancelar")).perform(click());
        assertEquals(0, falsos.chamadas.size());

        onView(withId(R.id.btnApagarMeta)).perform(click());
        onView(withText("Apagar")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamou("diario.apagar e1")));
        Espera.ate(() -> onView(withText("Passar no vestibular")).check(doesNotExist()));
        onView(withId(R.id.layoutDiarioVazio)).check(matches(isDisplayed()));
    }
}
