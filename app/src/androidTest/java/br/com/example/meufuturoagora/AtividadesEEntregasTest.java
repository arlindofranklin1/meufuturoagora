package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
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

import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Servicos;

// Módulo 5 da Fase 5: trilhas, entregas, avaliação e os arquivos no Storage.
// As regras (prazo, nota, permissões) são do servidor (pgTAP); aqui se confere
// o que a tela mostra e o que ela pede ao servidor.
@RunWith(AndroidJUnit4.class)
public class AtividadesEEntregasTest {

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

    private static Intent da(Class<?> tela) {
        return new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), tela);
    }

    private static Atividade trilha(String id, String nome, String anexo) {
        return new Atividade(id, "d1", nome, "Resolva a lista", 8, LocalDate.of(2026, 11, 20), null,
                anexo, anexo != null ? "lista.pdf" : null, 2026, 4, OffsetDateTime.now());
    }

    private void entrarComo(String id, String tipo) {
        falsos.eu = RepositoriosFalsos.perfil(id, "Pessoa " + id, tipo, false, "t1");
        falsos.perfis.add(falsos.eu);
    }

    // ===== Lista e exclusão de trilhas (professor) =====

    @Test
    public void professorVeAsTrilhasDaDisciplina() {
        entrarComo("p1", "professor");
        falsos.atividades.add(trilha("a1", "Frações", null));

        ActivityScenario.launch(da(TelaDetalhesDisciplinaProfessor.class).putExtra("disciplinaId", "d1"));

        Espera.ate(() -> onView(withText("Frações")).check(matches(isDisplayed())));
        onView(withText("8 pts")).check(matches(isDisplayed()));
        onView(withText("Prazo: 20/11/2026")).check(matches(isDisplayed()));
    }

    @Test
    public void excluirTrilhaAvisaQuantasEntregasEApagaArquivosAntesDaLinha() {
        entrarComo("p1", "professor");
        falsos.atividades.add(trilha("a1", "Frações", "d1/a1/anexo.pdf"));
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", "a1/s1/um.pdf", false));
        falsos.entregas.add(RepositoriosFalsos.entrega("e2", "a1", "s2", "a1/s2/dois.pdf", true));

        ActivityScenario.launch(da(TelaDetalhesDisciplinaProfessor.class).putExtra("disciplinaId", "d1"));

        Espera.ate(() -> onView(withText("Frações")).check(matches(isDisplayed())));
        onView(withId(R.id.btnMenuAtividade)).perform(click());
        onView(withText("Excluir")).inRoot(isPlatformPopup()).perform(click());

        Espera.ate(() -> onView(withText(containsString("As entregas de 2 alunos e os arquivos delas")))
                .check(matches(isDisplayed())));
        onView(withText("Excluir")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("atividades.excluir a1")));
        assertTrue(falsos.chamou("arquivos.apagar atividades d1/a1/anexo.pdf"));
        assertTrue(falsos.chamou("arquivos.apagar entregas a1/s1/um.pdf,a1/s2/dois.pdf"));
        assertTrue("os arquivos saem antes da linha",
                falsos.chamadas.indexOf("arquivos.apagar entregas a1/s1/um.pdf,a1/s2/dois.pdf")
                        < falsos.chamadas.indexOf("atividades.excluir a1"));
    }

    @Test
    public void cancelarAExclusaoNaoApagaNada() {
        entrarComo("p1", "professor");
        falsos.atividades.add(trilha("a1", "Frações", null));

        ActivityScenario.launch(da(TelaDetalhesDisciplinaProfessor.class).putExtra("disciplinaId", "d1"));

        Espera.ate(() -> onView(withText("Frações")).check(matches(isDisplayed())));
        onView(withId(R.id.btnMenuAtividade)).perform(click());
        onView(withText("Excluir")).inRoot(isPlatformPopup()).perform(click());
        Espera.ate(() -> onView(withText(containsString("Nenhum aluno entregou"))).check(matches(isDisplayed())));
        onView(withText("Cancelar")).perform(click());

        assertEquals(0, falsos.chamadas.size());
    }

    // ===== Criar trilha =====

    @Test
    public void trilhaCriadaVaiComPontosPrazoEDisciplina() {
        entrarComo("p1", "professor");
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d1", "Matemática", "t1", "p1"));

        ActivityScenario.launch(da(CriarAtividade.class).putExtra("disciplinaId", "d1"));

        Espera.ate(() -> onView(withId(R.id.edtNomeAtividade)).check(matches(isDisplayed())));
        onView(withId(R.id.edtNomeAtividade)).perform(replaceText("Lista 1"));
        onView(withId(R.id.edtPontos)).perform(replaceText("7"), closeSoftKeyboard());
        onView(withId(R.id.edtPrazo)).perform(replaceText("20/11/2026"));
        onView(withId(R.id.btnSalvar)).perform(scrollTo(), click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("atividades.criar Lista 1")));
    }

    @Test
    public void pontosForaDeUmADezOuSemPrazoNaoChamamOServidor() {
        entrarComo("p1", "professor");
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d1", "Matemática", "t1", "p1"));

        ActivityScenario.launch(da(CriarAtividade.class).putExtra("disciplinaId", "d1"));

        Espera.ate(() -> onView(withId(R.id.edtNomeAtividade)).check(matches(isDisplayed())));
        onView(withId(R.id.edtNomeAtividade)).perform(replaceText("Lista 1"));
        onView(withId(R.id.edtPrazo)).perform(replaceText("20/11/2026"));

        for (String invalida : new String[]{"0", "11", "abc"}) {
            onView(withId(R.id.edtPontos)).perform(replaceText(invalida), closeSoftKeyboard());
            onView(withId(R.id.btnSalvar)).perform(scrollTo(), click());
        }

        onView(withId(R.id.edtPontos)).perform(replaceText("5"), closeSoftKeyboard());
        onView(withId(R.id.edtPrazo)).perform(replaceText("31/02/2026"));
        onView(withId(R.id.btnSalvar)).perform(scrollTo(), click());

        assertEquals(0, falsos.chamadas.size());
    }

    @Test
    public void professorSemDisciplinaNaoCriaTrilha() {
        entrarComo("p1", "professor");
        // A disciplina de outro professor não entra na lista
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d9", "Física", "t1", "outro"));

        ActivityScenario<CriarAtividade> cena = ActivityScenario.launch(da(CriarAtividade.class));

        Espera.ate(() -> assertEquals(androidx.lifecycle.Lifecycle.State.DESTROYED, cena.getState()));
    }

    // ===== Entrega (aluno) =====

    @Test
    public void alunoSemEntregaVeNaoEntregueEEnviaSoComComentario() {
        entrarComo("s1", "aluno");
        falsos.atividades.add(trilha("a1", "Frações", null));

        ActivityScenario.launch(da(TelaAtividadeAluno.class).putExtra("atividadeId", "a1"));

        Espera.ate(() -> onView(withText("Não entregue")).check(matches(isDisplayed())));
        onView(withText("8 pts")).check(matches(isDisplayed()));

        // Sem comentário nem arquivo: nada vai ao servidor
        onView(withId(R.id.btnFazerEntrega)).perform(scrollTo(), click());
        assertEquals(0, falsos.chamadas.size());

        onView(withId(R.id.edtComentarioAluno)).perform(replaceText("Terminei"), closeSoftKeyboard());
        onView(withId(R.id.btnFazerEntrega)).perform(scrollTo(), click());
        onView(withText(containsString("não poderá ser modificada"))).check(matches(isDisplayed()));
        onView(withText("Enviar")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("atividades.entregar a1")));
    }

    @Test
    public void entregaJaFeitaFicaTravadaESemBotaoDeEnviar() {
        entrarComo("s1", "aluno");
        falsos.atividades.add(trilha("a1", "Frações", null));
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", null, false));

        ActivityScenario.launch(da(TelaAtividadeAluno.class).putExtra("atividadeId", "a1"));

        Espera.ate(() -> onView(withText("Entregue — aguardando avaliação")).check(matches(isDisplayed())));
        onView(withId(R.id.btnFazerEntrega)).check(matches(not(isDisplayed())));
    }

    @Test
    public void entregaAtrasadaMostraOsDiasDeAtraso() {
        entrarComo("s1", "aluno");
        falsos.atividades.add(trilha("a1", "Frações", null));
        falsos.entregas.add(new Entrega("e1", "a1", "s1", "ok", null, null, OffsetDateTime.now(), 2,
                null, null, null));

        ActivityScenario.launch(da(TelaAtividadeAluno.class).putExtra("atividadeId", "a1"));

        Espera.ate(() -> onView(withText("Entregue — aguardando avaliação (2 dias de atraso)"))
                .check(matches(isDisplayed())));
    }

    @Test
    public void entregaAvaliadaMostraANotaEOComentarioDoProfessor() {
        entrarComo("s1", "aluno");
        falsos.atividades.add(trilha("a1", "Frações", null));
        falsos.entregas.add(new Entrega("e1", "a1", "s1", "ok", null, null, OffsetDateTime.now(), 0,
                7.5, "Muito bem", OffsetDateTime.now()));

        ActivityScenario.launch(da(TelaAtividadeAluno.class).putExtra("atividadeId", "a1"));

        Espera.ate(() -> onView(withText("Avaliada")).check(matches(isDisplayed())));
        onView(withText("7.5")).check(matches(isDisplayed()));
        onView(withText("Muito bem")).check(matches(isDisplayed()));
    }

    @Test
    public void listaDoAlunoSeparaPendenteEntregueEAvaliada() {
        entrarComo("s1", "aluno");
        falsos.atividades.add(trilha("a1", "Frações", null));
        falsos.atividades.add(trilha("a2", "Geometria", null));
        falsos.atividades.add(trilha("a3", "Álgebra", null));
        falsos.entregas.add(RepositoriosFalsos.entrega("e2", "a2", "s1", null, false));
        falsos.entregas.add(RepositoriosFalsos.entrega("e3", "a3", "s1", null, true));

        ActivityScenario.launch(da(TelaDetalhesDisciplinaAluno.class).putExtra("disciplinaId", "d1"));

        Espera.ate(() -> onView(withText("Pendente")).check(matches(isDisplayed())));
        onView(withText("Entregue")).check(matches(isDisplayed()));
        onView(withText("Nota: 8.0")).check(matches(isDisplayed()));
    }

    // ===== Avaliação (professor) =====

    @Test
    public void notaVaiParaOServidorComVirgulaOuPonto() {
        entrarComo("p1", "professor");
        falsos.atividades.add(trilha("a1", "Frações", null));
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", null, false));

        ActivityScenario.launch(da(AvaliarEntregaActivity.class)
                .putExtra("atividadeId", "a1").putExtra("alunoId", "s1").putExtra("alunoNome", "Ana"));

        Espera.ate(() -> onView(withText("ok")).check(matches(isDisplayed())));
        onView(withId(R.id.edtNota)).perform(replaceText("7,5"), closeSoftKeyboard());
        onView(withId(R.id.btnAvaliar)).perform(scrollTo(), click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("atividades.avaliar e1 7.5")));
    }

    @Test
    public void notaEmBrancoOuInvalidaNaoChamaOServidor() {
        entrarComo("p1", "professor");
        falsos.atividades.add(trilha("a1", "Frações", null));
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", null, false));

        ActivityScenario.launch(da(AvaliarEntregaActivity.class)
                .putExtra("atividadeId", "a1").putExtra("alunoId", "s1").putExtra("alunoNome", "Ana"));

        Espera.ate(() -> onView(withText("ok")).check(matches(isDisplayed())));

        for (String invalida : new String[]{"", "abc"}) {
            onView(withId(R.id.edtNota)).perform(replaceText(invalida), closeSoftKeyboard());
            onView(withId(R.id.btnAvaliar)).perform(scrollTo(), click());
        }

        assertEquals(0, falsos.chamadas.size());
    }

    // ===== Entregas da trilha (professor) =====

    private void turmaComDoisAlunos() {
        falsos.atividades.add(trilha("a1", "Frações", null));
        falsos.participantes.add(new Participante("p1", "Paula", "professor", null, null, null));
        falsos.participantes.add(new Participante("s1", "Ana", "aluno", null, null, null));
        falsos.participantes.add(new Participante("s2", "Bruno", "aluno", null, null, null));
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", null, true));
    }

    @Test
    public void listaDeEntregasMostraSoAlunosComSeuStatus() {
        entrarComo("p1", "professor");
        turmaComDoisAlunos();

        ActivityScenario.launch(da(QuestoesDoLivroActivity.class)
                .putExtra("atividadeId", "a1").putExtra("disciplinaId", "d1"));

        Espera.ate(() -> onView(withId(R.id.tabEntregas)).check(matches(isDisplayed())));
        onView(withId(R.id.tabEntregas)).perform(click());

        Espera.ate(() -> onView(withText("Ana")).check(matches(isDisplayed())));
        onView(withText("Avaliado")).check(matches(isDisplayed()));
        onView(withText("Bruno")).check(matches(isDisplayed()));
        onView(withText("Não entregue")).check(matches(isDisplayed()));
        onView(withText("Paula")).check(doesNotExist());
    }

    @Test
    public void refazerAvaliacaoPedeAoServidorEAbreATelaDeNota() {
        entrarComo("p1", "professor");
        turmaComDoisAlunos();

        ActivityScenario.launch(da(QuestoesDoLivroActivity.class)
                .putExtra("atividadeId", "a1").putExtra("disciplinaId", "d1"));

        Espera.ate(() -> onView(withId(R.id.tabEntregas)).check(matches(isDisplayed())));
        onView(withId(R.id.tabEntregas)).perform(click());
        Espera.ate(() -> onView(withText("Ana")).check(matches(isDisplayed())));

        onView(allOf(withId(R.id.btnMenuEntrega), isDisplayed())).perform(click());
        onView(withText("Refazer avaliação")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Refazer")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("atividades.refazer e1")));
        Espera.ate(() -> onView(withId(R.id.btnAvaliar)).check(matches(isDisplayed())));
    }
}
