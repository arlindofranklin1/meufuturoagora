package br.com.example.meufuturoagora;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class CadastrarAulaActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private EditText edtQuantidadeAulas;
    private EditText edtDescricaoAula;
    private EditText edtDataAula;

    private String disciplinaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cadastrar_aula);

        db = FirebaseFirestore.getInstance();

        disciplinaId = getIntent().getStringExtra("disciplinaId");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        edtQuantidadeAulas = findViewById(R.id.edtQuantidadeAulas);
        edtDescricaoAula = findViewById(R.id.edtDescricaoAula);
        edtDataAula = findViewById(R.id.edtDataAula);

        edtDataAula.setOnClickListener(v -> abrirCalendario());

        MaterialButton btnSalvarAula = findViewById(R.id.btnSalvarAula);
        btnSalvarAula.setOnClickListener(v -> salvarAula());
    }

    private void abrirCalendario() {

        Calendar calendario = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, ano, mes, dia) -> edtDataAula.setText(
                        String.format("%02d/%02d/%04d", dia, mes + 1, ano)
                ),
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void salvarAula() {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {

            Toast.makeText(this, "Usuário não está logado.", Toast.LENGTH_SHORT).show();
            return;
        }

        String quantidadeTexto = edtQuantidadeAulas.getText().toString().trim();
        String descricao = edtDescricaoAula.getText().toString().trim();
        String data = edtDataAula.getText().toString().trim();

        if (quantidadeTexto.isEmpty()) {

            edtQuantidadeAulas.setError("Digite a quantidade de aulas");
            return;
        }

        if (data.isEmpty()) {

            edtDataAula.setError("Selecione a data");
            return;
        }

        if (disciplinaId == null) {

            Toast.makeText(this, "Disciplina não identificada.", Toast.LENGTH_SHORT).show();
            return;
        }

        long quantidade;

        try {

            quantidade = Long.parseLong(quantidadeTexto);

        } catch (NumberFormatException e) {

            edtQuantidadeAulas.setError("Digite um número válido");
            return;
        }

        Map<String, Object> aula = new HashMap<>();

        aula.put("quantidade", quantidade);
        aula.put("descricao", descricao);
        aula.put("data", data);
        aula.put("disciplinaId", disciplinaId);
        aula.put("professorId", usuario.getUid());
        aula.put("ativo", true);

        db.collection("aulas")
                .add(aula)
                .addOnSuccessListener(documentReference -> {

                    Toast.makeText(this, "Aula cadastrada!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao cadastrar aula: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
