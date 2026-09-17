package br.com.example.meufuturoagora;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.HashMap;
import java.util.Map;

public class TelaAtividadeAluno extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private String atividadeId;
    private String alunoId;
    private String documentoEntregaId;

    private String arquivoProfessorUrl;
    private Uri arquivoAlunoUri;
    private String nomeArquivoAluno;

    private ActivityResultLauncher<String> selecionarArquivoLauncher;

    private TextView tvStatusEntregaAluno;
    private TextView tvNomeAnexoProfessor;
    private LinearLayout layoutEnvioAluno;
    private LinearLayout layoutAvaliacaoAluno;
    private MaterialButton btnSelecionarArquivoAluno;
    private EditText edtComentarioAluno;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_atividade_aluno);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        atividadeId = getIntent().getStringExtra("atividadeId");
        String atividadeNome = getIntent().getStringExtra("atividadeNome");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        TextView tvTituloAtividadeAluno = findViewById(R.id.tvTituloAtividadeAluno);

        if (atividadeNome != null && !atividadeNome.isEmpty()) {
            tvTituloAtividadeAluno.setText(atividadeNome);
        }

        tvStatusEntregaAluno = findViewById(R.id.tvStatusEntregaAluno);
        tvNomeAnexoProfessor = findViewById(R.id.tvNomeAnexoProfessor);
        layoutEnvioAluno = findViewById(R.id.layoutEnvioAluno);
        layoutAvaliacaoAluno = findViewById(R.id.layoutAvaliacaoAluno);
        btnSelecionarArquivoAluno = findViewById(R.id.btnSelecionarArquivoAluno);
        edtComentarioAluno = findViewById(R.id.edtComentarioAluno);

        findViewById(R.id.itemAnexoProfessor).setOnClickListener(v -> {

            if (arquivoProfessorUrl != null && !arquivoProfessorUrl.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(arquivoProfessorUrl)));
            }
        });

        selecionarArquivoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {

                    if (uri != null) {

                        arquivoAlunoUri = uri;
                        nomeArquivoAluno = obterNomeArquivo(uri);
                        btnSelecionarArquivoAluno.setText(nomeArquivoAluno);
                    }
                }
        );

        btnSelecionarArquivoAluno.setOnClickListener(v -> selecionarArquivoLauncher.launch("*/*"));

        MaterialButton btnFazerEntrega = findViewById(R.id.btnFazerEntrega);
        btnFazerEntrega.setOnClickListener(v -> enviarEntrega());

        carregarAtividadeEEntrega(atividadeNome);
    }

    private void carregarAtividadeEEntrega(String atividadeNomeInicial) {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null || atividadeId == null) {
            return;
        }

        db.collection("atividades")
                .document(atividadeId)
                .get()
                .addOnSuccessListener(this::preencherAtividade);

        db.collection("alunos")
                .whereEqualTo("email", usuario.getEmail())
                .get()
                .addOnSuccessListener(alunos -> {

                    if (alunos.isEmpty()) {
                        return;
                    }

                    alunoId = alunos.getDocuments().get(0).getId();
                    documentoEntregaId = atividadeId + "_" + alunoId;

                    db.collection("entregas")
                            .document(documentoEntregaId)
                            .get()
                            .addOnSuccessListener(this::preencherEntrega);
                });
    }

    private void preencherAtividade(DocumentSnapshot documento) {

        if (!documento.exists()) {
            return;
        }

        String nome = documento.getString("nome");
        String descricao = documento.getString("descricao");
        Long pontos = documento.getLong("pontos");
        String prazo = documento.getString("prazo");
        String arquivoNome = documento.getString("arquivoNome");

        arquivoProfessorUrl = documento.getString("arquivoUrl");

        if (nome != null) {
            ((TextView) findViewById(R.id.tvTituloAtividadeAluno)).setText(nome);
        }

        ((TextView) findViewById(R.id.tvDescricaoAtividadeAluno)).setText(
                descricao != null && !descricao.isEmpty() ? descricao : "Sem descrição."
        );

        tvNomeAnexoProfessor.setText(
                arquivoNome != null && !arquivoNome.isEmpty() ? arquivoNome : "Nenhum anexo"
        );

        ((TextView) findViewById(R.id.tvPontuacaoMaximaAluno)).setText(
                (pontos != null ? pontos : 0) + " pts"
        );

        ((TextView) findViewById(R.id.tvPrazoFinalAluno)).setText(prazo != null ? prazo : "");
    }

    private void preencherEntrega(DocumentSnapshot documento) {

        if (!documento.exists()) {

            tvStatusEntregaAluno.setText("Não entregue");
            tvStatusEntregaAluno.setBackgroundResource(R.drawable.bg_pill_vermelho);
            tvStatusEntregaAluno.setTextColor(getColor(R.color.vermelho_erro));

            layoutEnvioAluno.setVisibility(View.VISIBLE);
            layoutAvaliacaoAluno.setVisibility(View.GONE);
            return;
        }

        Boolean avaliado = documento.getBoolean("avaliado");
        String comentarioAluno = documento.getString("comentarioAluno");

        if (comentarioAluno != null) {
            edtComentarioAluno.setText(comentarioAluno);
        }

        String arquivoNomeEnviado = documento.getString("arquivoNome");

        if (arquivoNomeEnviado != null && !arquivoNomeEnviado.isEmpty()) {
            btnSelecionarArquivoAluno.setText(arquivoNomeEnviado);
        }

        if (Boolean.TRUE.equals(avaliado)) {

            tvStatusEntregaAluno.setText("Avaliada");
            tvStatusEntregaAluno.setBackgroundResource(R.drawable.bg_pill_verde);
            tvStatusEntregaAluno.setTextColor(getColor(R.color.verde_sucesso));

            layoutEnvioAluno.setVisibility(View.GONE);
            layoutAvaliacaoAluno.setVisibility(View.VISIBLE);

            Double nota = documento.getDouble("nota");
            String comentarioProfessor = documento.getString("comentarioProfessor");

            ((TextView) findViewById(R.id.tvNotaAluno)).setText(
                    nota != null ? String.valueOf(nota) : "—"
            );

            ((TextView) findViewById(R.id.tvComentarioProfessorAluno)).setText(
                    comentarioProfessor != null && !comentarioProfessor.isEmpty()
                            ? comentarioProfessor
                            : "—"
            );

        } else {

            tvStatusEntregaAluno.setText("Entregue — aguardando avaliação");
            tvStatusEntregaAluno.setBackgroundResource(R.drawable.bg_card_branco);
            tvStatusEntregaAluno.setTextColor(getColor(R.color.roxo_primario));

            layoutEnvioAluno.setVisibility(View.VISIBLE);
            layoutAvaliacaoAluno.setVisibility(View.GONE);
        }
    }

    private void enviarEntrega() {

        if (alunoId == null || documentoEntregaId == null) {

            Toast.makeText(this, "Não foi possível identificar o aluno.", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> entrega = new HashMap<>();

        entrega.put("atividadeId", atividadeId);
        entrega.put("alunoId", alunoId);
        entrega.put("comentarioAluno", edtComentarioAluno.getText().toString().trim());
        entrega.put("avaliado", false);

        if (arquivoAlunoUri != null) {

            enviarArquivo(entrega);

        } else {

            salvarEntrega(entrega);
        }
    }

    private void enviarArquivo(Map<String, Object> entrega) {

        StorageReference referencia = storage.getReference()
                .child("entregas")
                .child(atividadeId)
                .child(System.currentTimeMillis() + "_" + nomeArquivoAluno);

        Toast.makeText(this, "Enviando arquivo...", Toast.LENGTH_SHORT).show();

        referencia.putFile(arquivoAlunoUri)
                .addOnSuccessListener(taskSnapshot ->
                        referencia.getDownloadUrl().addOnSuccessListener(uri -> {

                            entrega.put("arquivoNome", nomeArquivoAluno);
                            entrega.put("arquivoUrl", uri.toString());

                            salvarEntrega(entrega);
                        })
                )
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao enviar arquivo: " + e.getMessage(), Toast.LENGTH_LONG
                ).show());
    }

    private void salvarEntrega(Map<String, Object> entrega) {

        db.collection("entregas")
                .document(documentoEntregaId)
                .set(entrega, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Entrega enviada!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao enviar entrega: " + e.getMessage(), Toast.LENGTH_LONG
                ).show());
    }

    private String obterNomeArquivo(Uri uri) {

        String resultado = null;

        if ("content".equals(uri.getScheme())) {

            Cursor cursor = getContentResolver().query(uri, null, null, null, null);

            try {

                if (cursor != null && cursor.moveToFirst()) {

                    int indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (indice >= 0) {
                        resultado = cursor.getString(indice);
                    }
                }

            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (resultado == null) {
            resultado = uri.getLastPathSegment();
        }

        return resultado;
    }
}
