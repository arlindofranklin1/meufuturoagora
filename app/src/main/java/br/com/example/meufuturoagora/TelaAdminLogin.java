package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class TelaAdminLogin extends AppCompatActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private EditText edtSenhaAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_admin_login);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ImageView btnVoltarAdmin = findViewById(R.id.btnVoltarAdmin);
        btnVoltarAdmin.setOnClickListener(v -> finish());

        edtSenhaAdmin = findViewById(R.id.edtSenhaAdmin);

        MaterialButton btnAcessarAdmin = findViewById(R.id.btnAcessarAdmin);
        btnAcessarAdmin.setOnClickListener(v -> verificarSenha());
    }

    private void verificarSenha() {

        String senha = edtSenhaAdmin.getText().toString().trim();

        if (senha.isEmpty()) {

            edtSenhaAdmin.setError("Digite a senha");
            return;
        }

        // As regras do Firestore exigem um usuário autenticado (request.auth
        // != null) pra qualquer leitura. Quem acessa o admin sem antes ter
        // feito login com Google ainda não tem essa autenticação, então
        // entramos de forma anônima só pra satisfazer a regra.
        if (auth.getCurrentUser() != null) {

            consultarSenha(senha);

        } else {

            auth.signInAnonymously()
                    .addOnSuccessListener(result -> consultarSenha(senha))
                    .addOnFailureListener(e -> Toast.makeText(
                            this,
                            "Erro ao autenticar: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show());
        }
    }

    private void consultarSenha(String senha) {

        db.collection("administrador")
                .whereEqualTo("senha", senha)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    if (querySnapshot.isEmpty()) {

                        Toast.makeText(this, "Senha incorreta.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    startActivity(new Intent(this, TelaAdminHome.class));
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao verificar a senha: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
