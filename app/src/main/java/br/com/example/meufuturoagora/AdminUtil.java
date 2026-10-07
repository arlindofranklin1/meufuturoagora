package br.com.example.meufuturoagora;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

// A área do administrador só é aberta por contas marcadas como admin no banco:
// o cadastro do usuário (em "professores" ou "alunos", achado pelo e-mail)
// precisa ter o campo admin = true. Para essas contas, o perfil mostra o
// botão "Área do administrador".
class AdminUtil {

    static final String CAMPO_ADMIN = "admin";

    interface Callback {
        void aoVerificar(boolean ehAdmin);
    }

    private AdminUtil() {
    }

    static void verificar(FirebaseFirestore db, Callback callback) {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null || usuario.getEmail() == null) {
            callback.aoVerificar(false);
            return;
        }

        String email = usuario.getEmail();

        db.collection("professores")
                .whereEqualTo("email", email)
                .whereEqualTo(CAMPO_ADMIN, true)
                .get()
                .addOnSuccessListener(professores -> {

                    if (!professores.isEmpty()) {
                        callback.aoVerificar(true);
                        return;
                    }

                    db.collection("alunos")
                            .whereEqualTo("email", email)
                            .whereEqualTo(CAMPO_ADMIN, true)
                            .get()
                            .addOnSuccessListener(alunos -> callback.aoVerificar(!alunos.isEmpty()))
                            .addOnFailureListener(e -> callback.aoVerificar(false));
                })
                .addOnFailureListener(e -> callback.aoVerificar(false));
    }

    // Mostra o botão do perfil só para administradores
    static void configurarBotao(Activity activity, FirebaseFirestore db) {

        View botao = activity.findViewById(R.id.btnAreaAdministrador);

        botao.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, TelaAdminHome.class))
        );

        verificar(db, ehAdmin -> {

            if (!activity.isDestroyed()) {
                botao.setVisibility(ehAdmin ? View.VISIBLE : View.GONE);
            }
        });
    }
}
