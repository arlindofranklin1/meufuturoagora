package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CriarDisciplinaActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private EditText edtNomeDisciplina;
    private Spinner spinnerTurmaDisciplina;

    private final List<String> turmaIds = new ArrayList<>();
    private final List<String> turmaNomes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_criar_disciplina);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        db = FirebaseFirestore.getInstance();

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        edtNomeDisciplina = findViewById(R.id.edtNomeDisciplina);
        spinnerTurmaDisciplina = findViewById(R.id.spinnerTurmaDisciplina);

        carregarTurmas();

        MaterialButton btnSalvarDisciplina = findViewById(R.id.btnSalvarDisciplina);
        btnSalvarDisciplina.setOnClickListener(v -> salvarDisciplina());
    }

    private void carregarTurmas() {

        db.collection("turmas")
                .whereEqualTo("ativo", true)
                .get()
                .addOnSuccessListener(turmas -> {

                    turmaIds.clear();
                    turmaNomes.clear();

                    for (QueryDocumentSnapshot documento : turmas) {

                        String nome = documento.getString("nome");

                        if (nome != null) {
                            turmaIds.add(documento.getId());
                            turmaNomes.add(nome);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            R.layout.item_spinner,
                            turmaNomes
                    );

                    adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
                    spinnerTurmaDisciplina.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao carregar turmas: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void salvarDisciplina() {

        String nome = edtNomeDisciplina.getText().toString().trim();

        if (nome.isEmpty()) {
            edtNomeDisciplina.setError("Digite o nome da disciplina");
            return;
        }

        if (turmaIds.isEmpty()) {
            Toast.makeText(this, "Nenhuma turma ativa cadastrada.", Toast.LENGTH_LONG).show();
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return;
        }

        int posicao = spinnerTurmaDisciplina.getSelectedItemPosition();

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("turmaId", turmaIds.get(posicao));
        dados.put("turmaNome", turmaNomes.get(posicao));
        dados.put("professorId", usuario.getUid());
        dados.put("professorNome", usuario.getDisplayName() != null ? usuario.getDisplayName() : "Professor");
        dados.put("professorEmail", usuario.getEmail() != null ? usuario.getEmail() : "");
        dados.put("ativo", true);
        dados.put("criadoEm", com.google.firebase.firestore.FieldValue.serverTimestamp());
        AnoLetivoUtil.marcar(dados);

        db.collection("disciplinas")
                .add(dados)
                .addOnSuccessListener(referencia -> {

                    Toast.makeText(this, "Disciplina criada!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao criar disciplina: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
