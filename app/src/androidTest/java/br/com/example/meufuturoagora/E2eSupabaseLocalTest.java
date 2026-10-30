package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.SupabaseLocal;

// Ponta a ponta: telas de verdade, camada de dados de verdade e o banco LOCAL
// de verdade (RLS, gatilhos e funções), com as contas fictícias do seed.sql.
// Precisa do Supabase local ligado e do banco recém-resetado
// (supabase db reset), porque alguns testes cadastram e excluem usuários.
// Sem a chave local nos argumentos, os testes são pulados.
@RunWith(AndroidJUnit4.class)
public class E2eSupabaseLocalTest {

    private static final String ADMIN = "coordenacao@exemplo.test";
    private static final String MARTA = "professora.marta@exemplo.test";
    private static final String JOAO = "professor.joao@exemplo.test";
    private static final String LIA = "aluna.lia@exemplo.test";
    private static final String RUI = "aluno.rui@exemplo.test";
    private static final String NOVO = "aluno.novo@exemplo.test";

    private SupabaseLocal local;

    @Before
    public void preparar() {
        Assume.assumeTrue("Supabase local não informado", SupabaseLocal.disponivel());
        local = SupabaseLocal.instalar();
    }

    @After
    public void encerrar() {
        if (local != null) {
            local.sair();
        }
        Servicos.substituir(null);
    }

    @Test
    public void professoraVeOProprioPerfilComAsDisciplinasDela() throws Exception {
        Perfil marta = local.entrar(MARTA);
        assertTrue(marta.ehProfessor());

        ActivityScenario.launch(TelaPerfilProfessor.class);

        Espera.ate(() -> onView(withId(R.id.tvNomePerfil)).check(matches(withText("Marta Fictícia"))));
        // Seed: Marta dá Lógica de Programação (1º ano) e Banco de Dados (2º ano)
        Espera.ate(() -> onView(withId(R.id.tvQtdDisciplinasPerfilProfessor)).check(matches(withText("2"))));
        onView(withId(R.id.btnAreaAdministrador))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        Captura.salvar("01_perfil_professora");
    }

    @Test
    public void alunaVeOProprioPerfilComATurma() throws Exception {
        local.entrar(LIA);

        ActivityScenario.launch(TelaPerfilAluno.class);

        Espera.ate(() -> onView(withId(R.id.tvNomePerfilAluno)).check(matches(withText("Lia Fictícia"))));
        Espera.ate(() -> onView(withId(R.id.tvTurmaContaPerfilAluno)).check(matches(withText("1º ano Informática"))));
        // Matrícula automática (D9): as duas disciplinas do 1º ano
        onView(withId(R.id.tvQtdDisciplinasPerfil)).check(matches(withText("2")));
        onView(withId(R.id.tvPontuacaoTotalAluno)).check(matches(withText("0")));
        Captura.salvar("02_perfil_aluna");
    }

    @Test
    public void adminVeAAreaDoAdministradorComOAnoLetivo() throws Exception {
        local.entrar(ADMIN);

        ActivityScenario.launch(TelaPerfilProfessor.class);
        Espera.ate(() -> onView(withId(R.id.btnAreaAdministrador)).check(matches(isDisplayed())));
        Captura.salvar("03_perfil_admin");

        ActivityScenario.launch(TelaAdminHome.class);
        Espera.ate(() -> onView(withId(R.id.tvAnoLetivoAdmin)).check(matches(withText("Ano letivo 2026"))));
        Captura.salvar("04_area_admin");
    }

    @Test
    public void rlsAlunaNaoEnxergaOutrosAlunos() throws Exception {
        Perfil lia = local.entrar(LIA);

        List<Perfil> alunos = local.servicos.perfis.listar(Perfil.ALUNO);

        assertEquals("a aluna só vê o próprio perfil", 1, alunos.size());
        assertEquals(lia.id, alunos.get(0).id);

        try {
            local.servicos.escola.criarTurma("Turma da Lia", 9);
            fail("Aluna não pode criar turma");
        } catch (ErroApi esperado) {
            assertTrue(esperado.semPermissao());
        }
    }

    @Test
    public void adminCadastraAlunoQueEntraJaMatriculado() throws Exception {
        local.entrar(ADMIN);

        ActivityScenario.launch(CadastrarUsuarioActivity.class);

        onView(withId(R.id.radioAluno)).perform(click());
        Espera.ate(() -> onView(withId(R.id.spinnerTurma)).check(matches(withSpinnerText("1º ano Informática"))));
        onView(withId(R.id.edtNomeUsuario)).perform(replaceText("Aluno Novo"));
        onView(withId(R.id.edtEmailUsuario)).perform(replaceText(NOVO), closeSoftKeyboard());
        Captura.salvar("05_cadastro_aluno");
        onView(withId(R.id.btnSalvarUsuario)).perform(click());

        Espera.ate(() -> {
            try {
                assertNotNull(local.servicos.perfis.buscarPorEmail(NOVO));
            } catch (ErroApi e) {
                throw new AssertionError(e);
            }
        });

        Perfil novo = local.entrar(NOVO);

        assertTrue(novo.ehAluno());
        assertEquals("matriculado nas 2 disciplinas da turma (D9)", 2, local.servicos.disciplinas.listar().size());
    }

    @Test
    public void alunoPedeExclusaoEAdminAprova() throws Exception {
        local.entrar(RUI);

        ActivityScenario.launch(ConfiguracoesActivity.class);
        onView(withId(R.id.itemExcluirDados)).perform(click());
        onView(Matchers.instanceOf(EditText.class)).perform(replaceText("Mudei de escola"), closeSoftKeyboard());
        Captura.salvar("06_pedido_exclusao");
        onView(withText("Enviar pedido")).perform(click());

        Espera.ate(() -> {
            try {
                assertEquals(1, local.servicos.perfis.solicitacoes().size());
            } catch (ErroApi e) {
                throw new AssertionError(e);
            }
        });

        local.entrar(ADMIN);

        ActivityScenario.launch(ListaUsuariosActivity.class);
        Espera.ate(() -> onView(withText("Pedidos de exclusão (1)")).perform(click()));
        Captura.salvar("07_lista_com_pedido");
        onView(withId(R.id.btnMenuPessoa)).perform(click());
        onView(withText("Aprovar exclusão")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Excluir")).perform(click());

        Espera.ate(() -> {
            try {
                assertEquals(null, local.servicos.perfis.buscarPorEmail(RUI));
            } catch (ErroApi e) {
                throw new AssertionError(e);
            }
        });
        Captura.salvar("08_lista_depois_da_exclusao");

        // A conta de login também saiu: o Rui não entra mais
        try {
            local.entrar(RUI);
            fail("A conta excluída não pode entrar");
        } catch (ErroApi esperado) {
            // ok
        }
    }

    @Test
    public void professorComumNaoTemAreaDoAdministrador() throws Exception {
        local.entrar(JOAO);

        ActivityScenario.launch(TelaPerfilProfessor.class);

        Espera.ate(() -> onView(withId(R.id.tvNomePerfil)).check(matches(withText("João Fictício"))));
        onView(withId(R.id.btnAreaAdministrador))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }
}
