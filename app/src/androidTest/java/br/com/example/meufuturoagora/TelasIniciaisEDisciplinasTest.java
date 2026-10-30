package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.content.Intent;
import android.widget.EditText;

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
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.Frequencia;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.PosicaoRanking;
import br.com.example.meufuturoagora.dados.Servicos;

// Módulo 9 da Fase 5: telas iniciais, listas de disciplinas e o diálogo (i).
// O que cada perfil enxerga é do RLS; aqui se confere o que a tela mostra.
@RunWith(AndroidJUnit4.class)
public class TelasIniciaisEDisciplinasTest {

    private RepositoriosFalsos falsos;

    @Before
    public void preparar() {
        falsos = new RepositoriosFalsos();
        falsos.instalar();

        // Sem o pedido de permissão do Android por cima da tela inicial
        NotificacaoUtil.preferencias(InstrumentationRegistry.getInstrumentation().getTargetContext()).edit()
                .putBoolean(NotificacaoUtil.CHAVE_NOTIFICACOES, false).apply();
    }

    @After
    public void encerrar() {
        Servicos.substituir(null);
    }

    private static Intent da(Class<?> tela) {
        return new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(), tela);
    }

    private static Disciplina disciplina(String id, String nome, String professorId, boolean ativa) {
        return new Disciplina(id, nome, "t1", "1º ano A", professorId, 2026, 4, ativa, OffsetDateTime.now());
    }

    private void bimestreAtual() {
        LocalDate hoje = LocalDate.now();
        falsos.anoAtual = new AnoLetivo(hoje.getYear(), true, OffsetDateTime.now(), null);
        falsos.bimestres.add(new Bimestre(hoje.getYear(), 3, hoje.minusDays(10), hoje.plusDays(10)));
    }

    // ===== Tela inicial do aluno =====

    @Test
    public void inicialDoAlunoMostraSaudacaoPontosPosicaoFrequenciaETrilhasRecentes() {
        bimestreAtual();
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana Souza", "aluno", false, "t1");
        falsos.pontuacao = 42;
        falsos.posicao = new PosicaoRanking(3, 42, 20);
        falsos.disciplinas.add(disciplina("d1", "Matemática", "p1", true));
        falsos.disciplinas.add(disciplina("d2", "Antiga", "p1", false));

        OffsetDateTime agora = OffsetDateTime.now();
        falsos.atividades.add(new Atividade("a1", "d1", "Primeira", "", 5, LocalDate.of(2026, 11, 1), null,
                null, null, 2026, 4, agora.minusDays(3)));
        falsos.atividades.add(new Atividade("a2", "d1", "Segunda", "", 5, LocalDate.of(2026, 11, 2), null,
                null, null, 2026, 4, agora.minusDays(2)));
        falsos.atividades.add(new Atividade("a3", "d1", "Terceira", "", 5, LocalDate.of(2026, 11, 3), null,
                null, null, 2026, 4, agora.minusDays(1)));
        // Trilha de disciplina desativada não aparece
        falsos.atividades.add(new Atividade("a4", "d2", "Fantasma", "", 5, LocalDate.of(2026, 11, 4), null,
                null, null, 2026, 4, agora));

        // 4 registros, 1 falta: 75%
        for (int i = 0; i < 3; i++) {
            falsos.frequencias.add(new Frequencia("au" + i, "s1", Frequencia.PRESENTE));
        }
        falsos.frequencias.add(new Frequencia("au9", "s1", Frequencia.FALTA));

        ActivityScenario.launch(da(TelaInicialAluno.class));

        Espera.ate(() -> onView(withId(R.id.tvSaudacaoAluno)).check(matches(withText("Olá, Ana!"))));
        Espera.ate(() -> onView(withId(R.id.tvPontuacaoTotalAluno)).check(matches(withText("42"))));
        onView(withId(R.id.tvPosicaoRankingAluno)).check(matches(withText("3º")));
        onView(withId(R.id.tvFrequenciaPercentual)).check(matches(withText("75% | 1 faltas")));
        onView(withId(R.id.tvTituloBimestre)).check(matches(withText("3º Bimestre")));

        // Só as 2 mais recentes das disciplinas ativas
        onView(withText("Terceira")).check(matches(isDisplayed()));
        onView(withText("Segunda")).check(matches(isDisplayed()));
        onView(withText("Primeira")).check(doesNotExist());
        onView(withText("Fantasma")).check(doesNotExist());
    }

    @Test
    public void inicialDoAlunoSemPosicaoNemTrilhasNaoQuebra() {
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana Souza", "aluno", false, "t1");

        ActivityScenario.launch(da(TelaInicialAluno.class));

        Espera.ate(() -> onView(withId(R.id.tvPosicaoRankingAluno)).check(matches(withText("—"))));
        onView(withId(R.id.tvFrequenciaPercentual)).check(matches(withText("100% | 0 faltas")));
        onView(withId(R.id.tvSemAtividadesAluno)).check(matches(isDisplayed()));
        onView(withId(R.id.tvPeriodoBimestre)).check(matches(withText("Bimestre não definido")));
    }

    // ===== Tela inicial do professor =====

    @Test
    public void inicialDoProfessorMostraNomeEQuantidadeDeDisciplinasAtivasDele() {
        bimestreAtual();
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula Lima", "professor", false, null);
        falsos.disciplinas.add(disciplina("d1", "Matemática", "p1", true));
        falsos.disciplinas.add(disciplina("d2", "Física", "p1", true));
        falsos.disciplinas.add(disciplina("d3", "Excluída", "p1", false));
        falsos.disciplinas.add(disciplina("d4", "De outro", "outro", true));

        ActivityScenario.launch(da(TelaInicialProfessor.class));

        Espera.ate(() -> onView(withId(R.id.tvNomeProfessor)).check(matches(withText("Paula Lima"))));
        onView(withId(R.id.tvNumeroDisciplinas)).check(matches(withText("2")));
        onView(withId(R.id.tvTituloBimestre)).check(matches(withText("3º Bimestre")));
    }

    @Test
    public void semDisciplinaOBotaoCriarTrilhaSoAvisa() {
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula Lima", "professor", false, null);

        ActivityScenario.launch(da(TelaInicialProfessor.class));
        Espera.ate(() -> onView(withId(R.id.tvNumeroDisciplinas)).check(matches(withText("0"))));

        onView(withId(R.id.btnCriarTarefa)).perform(click());

        // Continua na tela inicial: o aviso é só um Toast
        onView(withId(R.id.tvNomeProfessor)).check(matches(isDisplayed()));
    }

    // ===== Disciplinas do aluno =====

    @Test
    public void alunoVeSoDisciplinasAtivasEPodeBuscarPeloNome() {
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana", "aluno", false, "t1");
        falsos.disciplinas.add(disciplina("d1", "Matemática", "p1", true));
        falsos.disciplinas.add(disciplina("d2", "História", "p1", true));
        falsos.disciplinas.add(disciplina("d3", "Excluída", "p1", false));

        ActivityScenario.launch(da(TelaDisciplinaAluno.class));

        Espera.ate(() -> onView(withText("Matemática")).check(matches(isDisplayed())));
        onView(withText("História")).check(matches(isDisplayed()));
        onView(withText("Excluída")).check(doesNotExist());

        onView(withId(R.id.edtBuscar)).perform(replaceText("hist"), closeSoftKeyboard());

        onView(withText("História")).check(matches(isDisplayed()));
        onView(withText("Matemática")).check(doesNotExist());
    }

    @Test
    public void alunoSemDisciplinasVeOAviso() {
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana", "aluno", false, "t1");

        ActivityScenario.launch(da(TelaDisciplinaAluno.class));

        Espera.ate(() -> onView(withId(R.id.tvSemDisciplinasAluno)).check(matches(isDisplayed())));
    }

    // ===== Disciplinas do professor =====

    private void professorComDuasDisciplinas() {
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula", "professor", false, null);
        falsos.disciplinas.add(disciplina("d1", "Matemática", "p1", true));
        falsos.disciplinas.add(disciplina("d2", "Física", "outro", true));
        falsos.participantes.add(new Participante("p1", "Paula", "professor", null, null, null));
        falsos.participantes.add(new Participante("s1", "Ana", "aluno", null, null, null));
        falsos.participantes.add(new Participante("s2", "Bruno", "aluno", null, null, null));
    }

    @Test
    public void professorVeSoAsPropriasEQuantidadeDeAlunos() {
        professorComDuasDisciplinas();

        ActivityScenario.launch(da(TelaDisciplinaProfessor.class));

        Espera.ate(() -> onView(withText("Matemática (1º ano A)")).check(matches(isDisplayed())));
        // O professor não conta como aluno
        onView(withText("2 alunos")).check(matches(isDisplayed()));
        onView(withText("Física (1º ano A)")).check(doesNotExist());
    }

    @Test
    public void renomearDisciplinaChamaOServidorEAtualizaALista() {
        professorComDuasDisciplinas();

        ActivityScenario.launch(da(TelaDisciplinaProfessor.class));
        Espera.ate(() -> onView(withText("Matemática (1º ano A)")).check(matches(isDisplayed())));

        onView(withId(R.id.btnMenuDisciplina)).perform(click());
        onView(withText("Editar")).inRoot(isPlatformPopup()).perform(click());
        onView(instanceOf(EditText.class)).inRoot(androidx.test.espresso.matcher.RootMatchers.isDialog())
                .perform(replaceText("Álgebra"), closeSoftKeyboard());
        onView(withText("Salvar")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("disciplinas.renomear d1 Álgebra")));
        Espera.ate(() -> onView(withText("Álgebra (1º ano A)")).check(matches(isDisplayed())));
    }

    @Test
    public void excluirDisciplinaDesativaEEscondeDaLista() {
        professorComDuasDisciplinas();

        ActivityScenario.launch(da(TelaDisciplinaProfessor.class));
        Espera.ate(() -> onView(withText("Matemática (1º ano A)")).check(matches(isDisplayed())));

        onView(withId(R.id.btnMenuDisciplina)).perform(click());
        onView(withText("Excluir")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Excluir disciplina")).check(matches(isDisplayed()));
        onView(withText("Cancelar")).perform(click());
        assertEquals(0, falsos.chamadas.size());

        onView(withId(R.id.btnMenuDisciplina)).perform(click());
        onView(withText("Excluir")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Excluir")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("disciplinas.desativar d1")));
        Espera.ate(() -> onView(withText("Matemática (1º ano A)")).check(doesNotExist()));
    }

    // ===== Diálogo (i) da disciplina =====

    @Test
    public void infoDaDisciplinaMostraProfessorTurmaEParticipantes() {
        falsos.eu = RepositoriosFalsos.perfil("s1", "Ana", "aluno", false, "t1");
        falsos.disciplinas.add(disciplina("d1", "Matemática", "p1", true));
        falsos.participantes.add(new Participante("p1", "Paula Lima", "professor", "paula@escola.test",
                null, null));
        falsos.participantes.add(new Participante("s2", "Bruno", "aluno", null, null, null));
        falsos.participantes.add(new Participante("s1", "Ana", "aluno", null, null, null));

        ActivityScenario.launch(da(TelaDetalhesDisciplinaAluno.class)
                .putExtra("disciplinaId", "d1").putExtra("disciplinaNome", "Matemática"));

        onView(withId(R.id.btnInfoProfessor)).perform(click());

        Espera.ate(() -> onView(withText("Paula Lima")).check(matches(isDisplayed())));
        onView(withText("paula@escola.test")).check(matches(isDisplayed()));
        onView(withText("1º ano A")).check(matches(isDisplayed()));
        onView(withText("Participantes (2)")).check(matches(isDisplayed()));
        onView(withText("Ana")).check(matches(isDisplayed()));
        onView(withText("Bruno")).check(matches(isDisplayed()));
    }
}
