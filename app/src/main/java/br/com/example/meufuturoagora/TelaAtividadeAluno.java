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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class TelaAtividadeAluno extends AppCompatActivity {

    private FirebaseFirestore db;

    private String atividadeId;
    private String alunoId;
    private String documentoEntregaId;

    private String arquivoProfessorUrl;
    private Uri arquivoAlunoUri;
    private String nomeArquivoAluno;
    private String linkProfessor;

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

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        db = FirebaseFirestore.getInstance();

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

        findViewById(R.id.itemLinkProfessor).setOnClickListener(v -> {

            if (linkProfessor != null && !linkProfessor.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(linkProfessor)));
            }
        });

        selecionarArquivoLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {

                    if (uri != null) {

                        long tamanho = ArquivoUtil.obterTamanho(
                                getContentResolver(), uri
                        );

                        if (tamanho > ArquivoUtil.LIMITE_TAMANHO_BYTES) {

                            Toast.makeText(
                                    this,
                                    "O arquivo deve ter até 3 MB.",
                                    Toast.LENGTH_LONG
                            ).show();

                            return;
                        }

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

        alunoId = usuario.getUid();
        documentoEntregaId = atividadeId + "_" + alunoId;

        db.collection("entregas")
                .document(documentoEntregaId)
                .get()
                .addOnSuccessListener(this::preencherEntrega);
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
        linkProfessor = documento.getString("link");

        if (nome != null) {
            ((TextView) findViewById(R.id.tvTituloAtividadeAluno)).setText(nome);
        }

        ((TextView) findViewById(R.id.tvDescricaoAtividadeAluno)).setText(
                descricao != null && !descricao.isEmpty() ? descricao : "Sem descrição."
        );

        tvNomeAnexoProfessor.setText(
                arquivoNome != null && !arquivoNome.isEmpty() ? arquivoNome : "Nenhum anexo"
        );

        View itemLinkProfessor = findViewById(R.id.itemLinkProfessor);

        if (linkProfessor != null && !linkProfessor.isEmpty()) {

            itemLinkProfessor.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvLinkProfessor)).setText(linkProfessor);

        } else {

            itemLinkProfessor.setVisibility(View.GONE);
        }

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

        String comentario = edtComentarioAluno.getText().toString().trim();

        // Não permite entrega vazia: precisa de comentário ou de arquivo
        if (comentario.isEmpty() && arquivoAlunoUri == null) {

            Toast.makeText(
                    this,
                    "Escreva um comentário ou anexe um arquivo para fazer a entrega.",
                    Toast.LENGTH_LONG
            ).show();
            return;
        }

        // Confirma antes de enviar, caso o aluno tenha anexado ou escrito algo errado
        String resumo = "Comentário: " + (comentario.isEmpty() ? "nenhum" : comentario)
                + "\nArquivo: " + (arquivoAlunoUri != null ? nomeArquivoAluno : "nenhum");

        new AlertDialog.Builder(this)
                .setTitle("Confirmar entrega")
                .setMessage("Confira sua entrega antes de enviar:\n\n" + resumo
                        + "\n\nDeseja enviar?")
                .setNegativeButton("Revisar", null)
                .setPositiveButton("Enviar", (dialog, which) -> confirmarEnvio(comentario))
                .show();
    }

    private void confirmarEnvio(String comentario) {

        Map<String, Object> entrega = new HashMap<>();

        entrega.put("atividadeId", atividadeId);
        entrega.put("alunoId", alunoId);
        entrega.put("comentarioAluno", comentario);
        entrega.put("avaliado", false);
        // Usado para notificar o professor sobre a nova entrega
        entrega.put("enviadoEm", com.google.firebase.firestore.FieldValue.serverTimestamp());
        AnoLetivoUtil.marcar(entrega);

        if (arquivoAlunoUri != null) {

            enviarArquivo(entrega);

        } else {

            salvarEntrega(entrega);
        }
    }

    private void enviarArquivo(Map<String, Object> entrega) {

        Toast.makeText(this, "Enviando arquivo...", Toast.LENGTH_SHORT).show();

        CloudinaryUtil.enviar(
                getContentResolver(),
                arquivoAlunoUri,
                nomeArquivoAluno,
                "entregas/" + atividadeId,
                new CloudinaryUtil.Callback() {

                    @Override
                    public void onSucesso(String url) {

                        entrega.put("arquivoNome", nomeArquivoAluno);
                        entrega.put("arquivoUrl", url);

                        salvarEntrega(entrega);
                    }

                    @Override
                    public void onErro(String mensagem) {

                        Toast.makeText(
                                TelaAtividadeAluno.this,
                                "Erro ao enviar arquivo: " + mensagem,
                                Toast.LENGTH_LONG
                        ).show();
                    }
                }
        );
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
