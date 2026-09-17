package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class Splash extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        new Handler(Looper.getMainLooper()).postDelayed(() -> {

            verificarUsuario();

        }, 2000);
    }

    private void verificarUsuario() {

        FirebaseUser user = auth.getCurrentUser();

        // Não existe usuário logado
        if (user == null) {

            abrirTelaEntrar();
            return;
        }

        // Usuário está logado
        String email = user.getEmail();

        // Procura o e-mail na coleção professores
        db.collection("professores")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (!querySnapshot.isEmpty()) {

                        // É professor
                        Intent intent = new Intent(
                                Splash.this,
                                TelaInicialProfessor.class
                        );

                        startActivity(intent);
                        finish();

                    } else {

                        // Está logado, mas não é professor
                        abrirTelaEntrar();
                    }

                })
                .addOnFailureListener(e -> {

                    // Se der erro no Firestore,
                    // manda para a tela de login
                    abrirTelaEntrar();
                });
    }

    private void abrirTelaEntrar() {

        Intent intent = new Intent(
                Splash.this,
                TelaEntrar.class
        );

        startActivity(intent);
        finish();
    }
}