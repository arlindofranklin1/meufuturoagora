package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

public class TelaAdminLogin extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText edtSenhaAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_admin_login);

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
