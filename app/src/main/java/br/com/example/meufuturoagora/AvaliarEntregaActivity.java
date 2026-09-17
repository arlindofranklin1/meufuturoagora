package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class AvaliarEntregaActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private String documentoId;
    private String arquivoUrl;

    private TextView tvNomeAnexoAluno;
    private TextView tvComentarioAluno;
    private EditText edtNota;
    private EditText edtComentarioProfessor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_avaliar_entrega);

        db = FirebaseFirestore.getInstance();

        String atividadeId = getIntent().getStringExtra("atividadeId");
        String alunoId = getIntent().getStringExtra("alunoId");
        String alunoNome = getIntent().getStringExtra("alunoNome");

        documentoId = atividadeId + "_" + alunoId;

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        TextView tvNomeAlunoAvaliar = findViewById(R.id.tvNomeAlunoAvaliar);
        tvNomeAlunoAvaliar.setText(alunoNome != null ? alunoNome : "Aluno");

        tvNomeAnexoAluno = findViewById(R.id.tvNomeAnexoAluno);
        tvComentarioAluno = findViewById(R.id.tvComentarioAluno);
        edtNota = findViewById(R.id.edtNota);
        edtComentarioProfessor = findViewById(R.id.edtComentarioProfessor);

        findViewById(R.id.itemAnexoAluno).setOnClickListener(v -> abrirAnexo());

        MaterialButton btnAvaliar = findViewById(R.id.btnAvaliar);
        btnAvaliar.setOnClickListener(v -> salvarAvaliacao());

        carregarEntrega();
    }

    private void carregarEntrega() {

        db.collection("entregas")
                .document(documentoId)
                .get()
                .addOnSuccessListener(documento -> {

                    if (!documento.exists()) {
                        return;
                    }

                    String arquivoNome = documento.getString("arquivoNome");
                    arquivoUrl = documento.getString("arquivoUrl");

                    tvNomeAnexoAluno.setText(
                            arquivoNome != null && !arquivoNome.isEmpty()
                                    ? arquivoNome
                                    : "Nenhum anexo"
                    );

                    String comentarioAluno = documento.getString("comentarioAluno");
                    tvComentarioAluno.setText(
                            comentarioAluno != null && !comentarioAluno.isEmpty()
                                    ? comentarioAluno
                                    : "—"
                    );

                    Double nota = documento.getDouble("nota");
                    if (nota != null) {
                        edtNota.setText(String.valueOf(nota));
                    }

                    String comentarioProfessor = documento.getString("comentarioProfessor");
                    if (comentarioProfessor != null) {
                        edtComentarioProfessor.setText(comentarioProfessor);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao carregar entrega.", Toast.LENGTH_SHORT
                ).show());
    }

    private void abrirAnexo() {

        if (arquivoUrl == null || arquivoUrl.isEmpty()) {

            Toast.makeText(this, "Nenhum anexo disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(arquivoUrl)));
    }

    private void salvarAvaliacao() {

        String notaTexto = edtNota.getText().toString().trim();

        if (notaTexto.isEmpty()) {

            edtNota.setError("Digite a nota");
            return;
        }

        double nota;

        try {

            nota = Double.parseDouble(notaTexto);

        } catch (NumberFormatException e) {

            edtNota.setError("Digite uma nota válida");
            return;
        }

        Map<String, Object> avaliacao = new HashMap<>();

        avaliacao.put("nota", nota);
        avaliacao.put(
                "comentarioProfessor",
                edtComentarioProfessor.getText().toString().trim()
        );
        avaliacao.put("avaliado", true);

        db.collection("entregas")
                .document(documentoId)
                .set(avaliacao, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Avaliação salva!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao salvar avaliação: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
