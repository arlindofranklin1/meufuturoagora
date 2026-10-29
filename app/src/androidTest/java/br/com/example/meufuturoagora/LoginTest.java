package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.intent.Intents.intended;
import static androidx.test.espresso.intent.Intents.intending;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasComponent;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra;
import static androidx.test.espresso.intent.matcher.IntentMatchers.hasExtraWithKey;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertTrue;

import android.app.Activity;
import android.app.Instrumentation;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.intent.Intents;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.ErroApiDeTeste;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;

// Módulo 1 da Fase 5: para onde a abertura do app leva cada perfil, e o logout.
// O AuthRepositorio é falso (sem rede) e as telas de destino não chegam a
// abrir: o Espresso Intents só confere qual tela seria aberta.
@RunWith(AndroidJUnit4.class)
public class LoginTest {

    // A Splash espera pelo menos 2 s; dá uma folga
    private static final long ESPERA_SPLASH_MS = 3500;

    private AuthFalso auth;

    @Before
    public void preparar() {
        auth = new AuthFalso();
        Servicos.substituir(new Servicos(auth, null, null, null, null, null, null, null, null, null));

        Intents.init();
        // As telas de destino não abrem de verdade (a de abertura do teste, sim)
        intending(anyOf(
                hasComponent(TelaInicialAluno.class.getName()),
                hasComponent(TelaInicialProfessor.class.getName()),
                hasComponent(TelaEntrar.class.getName())
        )).respondWith(new Instrumentation.ActivityResult(Activity.RESULT_OK, null));
    }

    @After
    public void encerrar() {
        Intents.release();
        Servicos.substituir(null);
    }

    private void abrirSplashEEsperar() throws InterruptedException {
        ActivityScenario.launch(Splash.class);
        Thread.sleep(ESPERA_SPLASH_MS);
    }

    @Test
    public void alunoComSessaoVaiParaATelaDoAluno() throws Exception {
        auth.perfil = AuthFalso.perfil(Perfil.ALUNO, false);

        abrirSplashEEsperar();

        intended(hasComponent(TelaInicialAluno.class.getName()));
    }

    @Test
    public void professorComSessaoVaiParaATelaDoProfessor() throws Exception {
        auth.perfil = AuthFalso.perfil(Perfil.PROFESSOR, false);

        abrirSplashEEsperar();

        intended(hasComponent(TelaInicialProfessor.class.getName()));
    }

    @Test
    public void adminEntraPelaTelaDoProfessor() throws Exception {
        auth.perfil = AuthFalso.perfil(Perfil.PROFESSOR, true);

        abrirSplashEEsperar();

        intended(hasComponent(TelaInicialProfessor.class.getName()));
    }

    @Test
    public void semSessaoVaiParaOLogin() throws Exception {
        auth.perfil = null;

        abrirSplashEEsperar();

        intended(allOf(hasComponent(TelaEntrar.class.getName()), not(hasExtraWithKey(TelaEntrar.EXTRA_MENSAGEM))));
    }

    @Test
    public void cadastroRecusadoVaiParaOLoginComAMensagemDoServidor() throws Exception {
        String recusa = "Seu e-mail não está autorizado a acessar o aplicativo. Fale com a coordenação da escola.";
        auth.erro = erro(403, "42501", recusa);

        abrirSplashEEsperar();

        intended(allOf(hasComponent(TelaEntrar.class.getName()), hasExtra(TelaEntrar.EXTRA_MENSAGEM, recusa)));
    }

    @Test
    public void sairEncerraASessaoEVoltaParaOLogin() throws Exception {
        ActivityScenario.launch(ConfiguracoesActivity.class);

        onView(withId(R.id.itemSair)).perform(click());
        onView(withText("Sair")).perform(click());

        intended(hasComponent(TelaEntrar.class.getName()));

        // O logout roda em segundo plano: espera um pouco
        for (int i = 0; i < 20 && !auth.saiu; i++) {
            Thread.sleep(100);
        }
        assertTrue("a sessão do Supabase foi encerrada", auth.saiu);
    }

    // ErroApi não tem construtor público: o caminho real é pela resposta do servidor
    private static ErroApi erro(int http, String codigo, String mensagem) {
        return ErroApiDeTeste.criar(http, "{\"code\":\"" + codigo + "\",\"message\":\"" + mensagem + "\"}");
    }
}
