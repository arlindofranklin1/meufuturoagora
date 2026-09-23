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

public class Splash extends AppCompatActivity {

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

        // A TelaEntrar é a única responsável por decidir se o usuário
        // logado é professor, aluno ou não autorizado (consulta o
        // Firestore); aqui só encaminhamos pra ela após o splash.
        new Handler(Looper.getMainLooper()).postDelayed(
                this::abrirTelaEntrar, 2000
        );
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