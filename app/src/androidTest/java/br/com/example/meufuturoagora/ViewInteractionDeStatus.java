package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.hasDescendant;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withParent;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.hamcrest.Matchers.allOf;

import androidx.test.espresso.ViewInteraction;

// O seletor de status de um aluno na tela de frequência, achado pelo nome dele
// (o nome e o seletor ficam no mesmo contêiner da linha)
final class ViewInteractionDeStatus {

    private ViewInteractionDeStatus() {
    }

    static ViewInteraction de(String nomeDoAluno) {
        return onView(allOf(withId(R.id.spinnerStatusFrequencia), withParent(hasDescendant(withText(nomeDoAluno)))));
    }
}
