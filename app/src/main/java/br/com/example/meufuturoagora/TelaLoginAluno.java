package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

public class TelaLoginAluno extends AppCompatActivity {

    private static final String[] TURMAS = {
            "6º ano", "7º ano", "8º ano", "9º ano"
    };

    private FirebaseFirestore db;

    private String alunoId;

    private Spinner spinnerTurma;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_login_aluno);

        db = FirebaseFirestore.getInstance();

        alunoId = getIntent().getStringExtra("alunoId");

        ImageButton btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        spinnerTurma = findViewById(R.id.spinnerTurma);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, TURMAS
        );

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerTurma.setAdapter(adapter);

        MaterialButton btnCadastrar = findViewById(R.id.btnCadastrar);
        btnCadastrar.setOnClickListener(v -> solicitarEntrada());
    }

    private void solicitarEntrada() {

        if (alunoId == null) {

            Toast.makeText(this, "Não foi possível identificar o aluno.", Toast.LENGTH_SHORT).show();
            return;
        }

        String turma = (String) spinnerTurma.getSelectedItem();

        if (turma == null) {

            Toast.makeText(this, "Selecione uma turma.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> atualizacao = new HashMap<>();
        atualizacao.put("turma", turma);

        db.collection("alunos")
                .document(alunoId)
                .set(atualizacao, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> matricularNasDisciplinasDaTurma(turma))
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao salvar turma: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void matricularNasDisciplinasDaTurma(String turma) {

        db.collection("alunos")
                .document(alunoId)
                .get()
                .addOnSuccessListener(alunoDoc -> {

                    String alunoNome = alunoDoc.getString("nome");

                    db.collection("disciplinas")
                            .whereEqualTo("turma", turma)
                            .whereEqualTo("ativo", true)
                            .get()
                            .addOnSuccessListener(disciplinas -> {

                                for (QueryDocumentSnapshot disciplina : disciplinas) {

                                    Map<String, Object> matricula = new HashMap<>();

                                    matricula.put("disciplinaId", disciplina.getId());
                                    matricula.put("disciplinaNome", disciplina.getString("nome"));
                                    matricula.put("alunoId", alunoId);
                                    matricula.put("alunoNome", alunoNome != null ? alunoNome : "Aluno");
                                    matricula.put("turma", turma);

                                    db.collection("matriculas")
                                            .document(disciplina.getId() + "_" + alunoId)
                                            .set(matricula);
                                }

                                irParaHomeDoAluno();
                            })
                            .addOnFailureListener(e -> irParaHomeDoAluno());
                })
                .addOnFailureListener(e -> irParaHomeDoAluno());
    }

    private void irParaHomeDoAluno() {

        Intent intent = new Intent(this, TelaInicialAluno.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
