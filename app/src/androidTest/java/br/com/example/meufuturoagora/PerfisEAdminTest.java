package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.OffsetDateTime;

import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.PosicaoRanking;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.SolicitacaoExclusao;
import br.com.example.meufuturoagora.dados.Turma;

// Módulo 2 da Fase 5: perfis, cadastro pelo admin, lista de usuários,
// exclusão definitiva (D7, D13) e pedidos de exclusão (D12).
@RunWith(AndroidJUnit4.class)
public class PerfisEAdminTest {

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

    // ===== Perfis =====

    @Test
    public void perfilDoAlunoMostraOsDadosDoServidor() {
        falsos.eu = RepositoriosFalsos.perfil("a1", "Ana Souza", Perfil.ALUNO, false, "t1");
        falsos.pontuacao = 14;
        falsos.posicao = new PosicaoRanking(2, 14, 5);
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "x", "a1", null, true));
        falsos.entregas.add(RepositoriosFalsos.entrega("e2", "y", "a1", null, false));

        ActivityScenario.launch(TelaPerfilAluno.class);

        Espera.ate(() -> onView(withId(R.id.tvNomePerfilAluno)).check(matches(withText("Ana Souza"))));
        onView(withId(R.id.tvTurmaContaPerfilAluno)).check(matches(withText("6º ano A")));
        onView(withId(R.id.tvPontuacaoTotalAluno)).check(matches(withText("14")));
        onView(withId(R.id.tvPosicaoRankingAluno)).check(matches(withText("2º")));
        onView(withId(R.id.tvQtdConcluidasPerfil)).check(matches(withText("1")));
        onView(withId(R.id.btnAreaAdministrador))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    @Test
    public void adminVeOBotaoDaAreaDoAdministrador() {
        falsos.eu = RepositoriosFalsos.perfil("p1", "Coordenação", Perfil.PROFESSOR, true, null);
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d1", "Matemática", "t1", "p1"));
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d2", "Português", "t1", "outro"));

        ActivityScenario.launch(TelaPerfilProfessor.class);

        Espera.ate(() -> onView(withId(R.id.btnAreaAdministrador)).check(matches(isDisplayed())));
        onView(withId(R.id.tvNomePerfil)).check(matches(withText("Coordenação")));
        onView(withId(R.id.tvQtdDisciplinasPerfilProfessor)).check(matches(withText("1")));
    }

    @Test
    public void professorComumNaoVeAAreaDoAdministrador() {
        falsos.eu = RepositoriosFalsos.perfil("p2", "Paula", Perfil.PROFESSOR, false, null);

        ActivityScenario.launch(TelaPerfilProfessor.class);

        Espera.ate(() -> onView(withId(R.id.tvNomePerfil)).check(matches(withText("Paula"))));
        onView(withId(R.id.btnAreaAdministrador))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }

    // ===== Cadastro =====

    @Test
    public void adminCadastraAlunoNaTurma() {
        ActivityScenario.launch(CadastrarUsuarioActivity.class);

        onView(withId(R.id.radioAluno)).perform(click());
        onView(withId(R.id.edtNomeUsuario)).perform(replaceText("Bruno Lima"));
        onView(withId(R.id.edtEmailUsuario)).perform(replaceText("bruno@escola.test"), closeSoftKeyboard());
        // As turmas chegam em segundo plano; sem elas o aluno não tem turma para escolher
        Espera.ate(() -> onView(withId(R.id.spinnerTurma)).check(matches(ViewMatchers.withSpinnerText("6º ano A"))));
        onView(withId(R.id.btnSalvarUsuario)).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(),
                falsos.chamou("perfis.cadastrar bruno@escola.test Bruno Lima aluno t1")));
    }

    @Test
    public void emailInvalidoNaoChamaOServidor() {
        ActivityScenario.launch(CadastrarUsuarioActivity.class);

        onView(withId(R.id.edtNomeUsuario)).perform(replaceText("Pedro"));
        onView(withId(R.id.edtEmailUsuario)).perform(replaceText("pedro-sem-arroba"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarUsuario)).perform(click());

        assertEquals(0, falsos.chamadas.size());
    }

    @Test
    public void emailRepetidoOfereceAtualizarOCadastro() {
        falsos.perfis.add(RepositoriosFalsos.perfil("p5", "Pedro Antigo", Perfil.PROFESSOR, false, null));
        falsos.erroNoCadastro = RepositoriosFalsos.erro(409, "23505", "duplicate key");

        ActivityScenario.launch(CadastrarUsuarioActivity.class);

        onView(withId(R.id.edtNomeUsuario)).perform(replaceText("Pedro Novo"));
        onView(withId(R.id.edtEmailUsuario)).perform(replaceText("p5@escola.test"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarUsuario)).perform(click());

        Espera.ate(() -> onView(withText("E-mail já cadastrado")).check(matches(isDisplayed())));
        onView(withText("Atualizar")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(),
                falsos.chamou("perfis.atualizar p5 Pedro Novo null")));
    }

    @Test
    public void adminCadastraTurma() {
        ActivityScenario.launch(CadastrarUsuarioActivity.class);

        onView(withId(R.id.radioTurma)).perform(click());
        onView(withId(R.id.edtNomeUsuario)).perform(replaceText("7º ano B"));
        onView(withId(R.id.edtAnoTurma)).perform(replaceText("7"), closeSoftKeyboard());
        onView(withId(R.id.btnSalvarUsuario)).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamou("escola.criarTurma 7º ano B 7")));
    }

    // ===== Lista de usuários e exclusão =====

    @Test
    public void excluirAlunoApagaOsArquivosEDepoisOCadastro() {
        falsos.perfis.add(RepositoriosFalsos.perfil("a1", "Ana", Perfil.ALUNO, false, "t1"));
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "t9", "a1", "t9/a1/resposta.pdf", true));

        ActivityScenario.launch(ListaUsuariosActivity.class);

        Espera.ate(() -> onView(withText("Turma 6º ano A (1 aluno)")).perform(click()));
        onView(withId(R.id.btnMenuPessoa)).perform(click());
        onView(withText("Excluir")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Excluir")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("perfis.excluir a1")));
        int arquivos = falsos.chamadas.indexOf("arquivos.apagar entregas t9/a1/resposta.pdf");
        assertTrue("os arquivos saem antes do cadastro",
                arquivos >= 0 && arquivos < falsos.chamadas.indexOf("perfis.excluir a1"));
    }

    @Test
    public void excluirProfessorComDisciplinasPedeQuemRecebe() {
        falsos.perfis.add(RepositoriosFalsos.perfil("p1", "Paula", Perfil.PROFESSOR, false, null));
        falsos.perfis.add(RepositoriosFalsos.perfil("p2", "Pedro", Perfil.PROFESSOR, false, null));
        falsos.disciplinas.add(RepositoriosFalsos.disciplina("d1", "Matemática", "t1", "p1"));

        ActivityScenario.launch(ListaUsuariosActivity.class);

        Espera.ate(() -> onView(withText("Professores (2)")).perform(click()));
        onView(Matchers.allOf(withId(R.id.btnMenuPessoa), ViewMatchers.hasSibling(
                ViewMatchers.hasDescendant(withText("Paula"))))).perform(click());
        onView(withText("Excluir")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Excluir")).perform(click());

        Espera.ate(() -> onView(withText("Pedro")).inRoot(isDialog()).check(matches(isDisplayed())));
        onView(withText("Pedro")).inRoot(isDialog()).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("perfis.excluir p1")));
        assertTrue("a transferência vem antes da exclusão",
                falsos.chamadas.indexOf("perfis.transferir p1 p2") < falsos.chamadas.indexOf("perfis.excluir p1"));
    }

    @Test
    public void adminAprovaPedidoDeExclusao() {
        falsos.perfis.add(RepositoriosFalsos.perfil("a1", "Ana", Perfil.ALUNO, false, "t1"));
        falsos.pedidos.add(new SolicitacaoExclusao("s1", "a1", "Vou mudar de escola",
                SolicitacaoExclusao.PENDENTE, OffsetDateTime.now(), null));

        ActivityScenario.launch(ListaUsuariosActivity.class);

        Espera.ate(() -> onView(withText("Pedidos de exclusão (1)")).perform(click()));
        onView(withId(R.id.btnMenuPessoa)).perform(click());
        onView(withText("Aprovar exclusão")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Excluir")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamadas.toString(), falsos.chamou("perfis.decidir s1 true")));
    }

    @Test
    public void adminRecusaPedidoDeExclusao() {
        falsos.perfis.add(RepositoriosFalsos.perfil("a1", "Ana", Perfil.ALUNO, false, "t1"));
        falsos.pedidos.add(new SolicitacaoExclusao("s1", "a1", null,
                SolicitacaoExclusao.PENDENTE, OffsetDateTime.now(), null));

        ActivityScenario.launch(ListaUsuariosActivity.class);

        Espera.ate(() -> onView(withText("Pedidos de exclusão (1)")).perform(click()));
        onView(withId(R.id.btnMenuPessoa)).perform(click());
        onView(withText("Recusar pedido")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Recusar")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamou("perfis.decidir s1 false")));
    }

    // ===== Pedido de exclusão pelo próprio usuário =====

    @Test
    public void usuarioPedeAExclusaoDosDados() {
        ActivityScenario.launch(ConfiguracoesActivity.class);

        onView(withId(R.id.itemExcluirDados)).perform(click());
        onView(Matchers.instanceOf(EditText.class)).perform(replaceText("Vou mudar de escola"), closeSoftKeyboard());
        onView(withText("Enviar pedido")).perform(click());

        Espera.ate(() -> assertTrue(falsos.chamou("perfis.solicitarExclusao Vou mudar de escola")));
    }
}
