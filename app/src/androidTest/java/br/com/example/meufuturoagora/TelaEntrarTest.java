package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isEnabled;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.Test;
import org.junit.runner.RunWith;

// A TelaEntrar abre sem consultar nada: só mostra o botão (a Splash é quem
// confere a sessão). O login com Google em si precisa de uma conta real e é
// coberto no QA manual.
@RunWith(AndroidJUnit4.class)
public class TelaEntrarTest {

    @Test
    public void mostraOBotaoDoGoogleHabilitado() {
        ActivityScenario.launch(new Intent(InstrumentationRegistry.getInstrumentation().getTargetContext(),
                TelaEntrar.class).putExtra(TelaEntrar.EXTRA_MENSAGEM, "Sua sessão expirou. Entre de novo."));

        onView(withId(R.id.googleButton)).check(matches(isDisplayed()));
        onView(withId(R.id.googleButton)).check(matches(isEnabled()));
        onView(withText("Entrar com o Google")).check(matches(isDisplayed()));
    }
}
