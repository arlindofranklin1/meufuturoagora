package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class DefinirAnoLetivoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText edtAnoLetivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_definir_ano_letivo);

        db = FirebaseFirestore.getInstance();

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        edtAnoLetivo = findViewById(R.id.edtAnoLetivo);

        MaterialButton btnSalvarAnoLetivo = findViewById(R.id.btnSalvarAnoLetivo);
        btnSalvarAnoLetivo.setOnClickListener(v -> salvarAnoLetivo());

        carregarAnoLetivo();
    }

    private void carregarAnoLetivo() {

        db.collection("configuracoes")
                .document("geral")
                .get()
                .addOnSuccessListener(documento -> {

                    String anoLetivo = documento.getString("anoLetivo");

                    if (anoLetivo != null) {
                        edtAnoLetivo.setText(anoLetivo);
                    }
                });
    }

    private void salvarAnoLetivo() {

        String anoLetivo = edtAnoLetivo.getText().toString().trim();

        if (anoLetivo.isEmpty()) {

            edtAnoLetivo.setError("Digite o ano letivo");
            return;
        }

        Map<String, Object> dados = new HashMap<>();
        dados.put("anoLetivo", anoLetivo);

        db.collection("configuracoes")
                .document("geral")
                .set(dados, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Ano letivo salvo!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao salvar: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
