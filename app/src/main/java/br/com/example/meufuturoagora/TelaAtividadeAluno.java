package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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

import java.io.IOException;

import br.com.example.meufuturoagora.dados.Arquivos;
import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class TelaAtividadeAluno extends AppCompatActivity {

    private String atividadeId;
    private Perfil aluno;

    private String arquivoProfessorPath;
    private Uri arquivoAlunoUri;
    private String nomeArquivoAluno;
    private String tipoArquivoAluno;
    private String linkProfessor;

    private ActivityResultLauncher<String[]> selecionarArquivoLauncher;

    private TextView tvStatusEntregaAluno;
    private TextView tvNomeAnexoProfessor;
    private LinearLayout layoutEnvioAluno;
    private LinearLayout layoutAvaliacaoAluno;
    private MaterialButton btnSelecionarArquivoAluno;
    private EditText edtComentarioAluno;
    private MaterialButton btnFazerEntrega;

    // Depois de entregue, o aluno não pode mais alterar a entrega
    private boolean entregaFeita = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_atividade_aluno);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

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

        findViewById(R.id.itemAnexoProfessor).setOnClickListener(v ->
                ArquivoUtil.abrir(this, Arquivos.BUCKET_ATIVIDADES, arquivoProfessorPath));

        findViewById(R.id.itemLinkProfessor).setOnClickListener(v -> {

            if (linkProfessor != null && !linkProfessor.isEmpty()) {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(linkProfessor)));
            }
        });

        selecionarArquivoLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {

                    if (uri == null) {
                        return;
                    }

                    String problema = ArquivoUtil.validar(getContentResolver(), uri);

                    if (problema != null) {

                        Toast.makeText(this, problema, Toast.LENGTH_LONG).show();
                        return;
                    }

                    arquivoAlunoUri = uri;
                    tipoArquivoAluno = getContentResolver().getType(uri);
                    nomeArquivoAluno = ArquivoUtil.obterNome(getContentResolver(), uri);
                    btnSelecionarArquivoAluno.setText(nomeArquivoAluno);
                }
        );

        btnSelecionarArquivoAluno.setOnClickListener(v ->
                selecionarArquivoLauncher.launch(ArquivoUtil.TIPOS_DO_SELETOR));

        btnFazerEntrega = findViewById(R.id.btnFazerEntrega);
        btnFazerEntrega.setOnClickListener(v -> enviarEntrega());

        carregarAtividadeEEntrega();
    }

    private static final class Carga {

        final Perfil aluno;
        final Atividade atividade;
        final Entrega entrega;

        Carga(Perfil aluno, Atividade atividade, Entrega entrega) {
            this.aluno = aluno;
            this.atividade = atividade;
            this.entrega = entrega;
        }
    }

    private void carregarAtividadeEEntrega() {

        if (atividadeId == null) {
            return;
        }

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this,
                () -> new Carga(servicos.perfis.meu(), servicos.atividades.buscar(atividadeId),
                        servicos.atividades.minhaEntrega(atividadeId)),
                new Tarefas.Retorno<Carga>() {
                    @Override
                    public void sucesso(Carga carga) {

                        aluno = carga.aluno;

                        if (carga.atividade != null) {
                            preencherAtividade(carga.atividade);
                        }

                        preencherEntrega(carga.entrega);
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Toast.makeText(TelaAtividadeAluno.this,
                                "Erro ao carregar a atividade: " + erro.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void preencherAtividade(Atividade atividade) {

        arquivoProfessorPath = atividade.arquivoPath;
        linkProfessor = atividade.link;

        if (atividade.nome != null) {
            ((TextView) findViewById(R.id.tvTituloAtividadeAluno)).setText(atividade.nome);
        }

        ((TextView) findViewById(R.id.tvDescricaoAtividadeAluno)).setText(
                atividade.descricao != null && !atividade.descricao.isEmpty()
                        ? atividade.descricao
                        : "Sem descrição."
        );

        tvNomeAnexoProfessor.setText(
                atividade.arquivoNome != null && !atividade.arquivoNome.isEmpty()
                        ? atividade.arquivoNome
                        : "Nenhum anexo"
        );

        View itemLinkProfessor = findViewById(R.id.itemLinkProfessor);

        if (linkProfessor != null && !linkProfessor.isEmpty()) {

            itemLinkProfessor.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvLinkProfessor)).setText(linkProfessor);

        } else {

            itemLinkProfessor.setVisibility(View.GONE);
        }

        ((TextView) findViewById(R.id.tvPontuacaoMaximaAluno)).setText(atividade.pontos + " pts");

        ((TextView) findViewById(R.id.tvPrazoFinalAluno)).setText(
                atividade.prazo != null ? Datas.formatar(atividade.prazo) : ""
        );
    }

    private void preencherEntrega(Entrega entrega) {

        if (entrega == null) {

            tvStatusEntregaAluno.setText("Não entregue");
            tvStatusEntregaAluno.setBackgroundResource(R.drawable.bg_pill_vermelho);
            tvStatusEntregaAluno.setTextColor(getColor(R.color.vermelho_erro));

            layoutEnvioAluno.setVisibility(View.VISIBLE);
            layoutAvaliacaoAluno.setVisibility(View.GONE);
            return;
        }

        bloquearEdicaoEntrega();

        if (entrega.comentarioAluno != null) {
            edtComentarioAluno.setText(entrega.comentarioAluno);
        }

        if (entrega.arquivoNome != null && !entrega.arquivoNome.isEmpty()) {
            btnSelecionarArquivoAluno.setText(entrega.arquivoNome);
        }

        // Decisão D4: a entrega atrasada é aceita e mostra os dias de atraso
        String atraso = entrega.atrasada()
                ? " (" + entrega.diasAtraso + (entrega.diasAtraso == 1 ? " dia" : " dias") + " de atraso)"
                : "";

        if (entrega.avaliada()) {

            tvStatusEntregaAluno.setText("Avaliada" + atraso);
            tvStatusEntregaAluno.setBackgroundResource(R.drawable.bg_pill_verde);
            tvStatusEntregaAluno.setTextColor(getColor(R.color.verde_sucesso));

            layoutEnvioAluno.setVisibility(View.GONE);
            layoutAvaliacaoAluno.setVisibility(View.VISIBLE);

            ((TextView) findViewById(R.id.tvNotaAluno)).setText(
                    entrega.nota != null ? String.valueOf(entrega.nota) : "—"
            );

            ((TextView) findViewById(R.id.tvComentarioProfessorAluno)).setText(
                    entrega.comentarioProfessor != null && !entrega.comentarioProfessor.isEmpty()
                            ? entrega.comentarioProfessor
                            : "—"
            );

        } else {

            tvStatusEntregaAluno.setText("Entregue — aguardando avaliação" + atraso);
            tvStatusEntregaAluno.setBackgroundResource(R.drawable.bg_card_branco);
            tvStatusEntregaAluno.setTextColor(getColor(R.color.roxo_primario));

            layoutEnvioAluno.setVisibility(View.VISIBLE);
            layoutAvaliacaoAluno.setVisibility(View.GONE);
        }
    }

    // Mantém a entrega visível, mas sem permitir trocar o arquivo, o comentário ou reenviar
    private void bloquearEdicaoEntrega() {

        entregaFeita = true;

        btnSelecionarArquivoAluno.setEnabled(false);
        edtComentarioAluno.setEnabled(false);
        edtComentarioAluno.setFocusable(false);
        edtComentarioAluno.setHint("Sem comentário");
        btnFazerEntrega.setVisibility(View.GONE);
    }

    private void enviarEntrega() {

        if (entregaFeita) {

            Toast.makeText(this, "Esta atividade já foi entregue.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (aluno == null) {

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
                        + "\n\nAtenção: depois de enviada, a entrega não poderá ser modificada."
                        + "\n\nDeseja enviar?")
                .setNegativeButton("Revisar", null)
                .setPositiveButton("Enviar", (dialog, which) -> confirmarEnvio(comentario))
                .show();
    }

    private void confirmarEnvio(String comentario) {

        // Evita um segundo envio enquanto o primeiro ainda está em andamento
        btnFazerEntrega.setEnabled(false);

        Servicos servicos = Servicos.get(this);
        Uri uri = arquivoAlunoUri;
        String nome = nomeArquivoAluno;
        String tipo = tipoArquivoAluno;

        if (uri != null) {
            Toast.makeText(this, "Enviando arquivo...", Toast.LENGTH_SHORT).show();
        }

        Tarefas.executar(this, () -> {

            String caminho = null;

            if (uri != null) {

                byte[] conteudo;

                try {
                    conteudo = ArquivoUtil.ler(getContentResolver(), uri);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }

                caminho = Arquivos.caminhoEntrega(atividadeId, aluno.id, tipo);
                servicos.arquivos.enviar(Arquivos.BUCKET_ENTREGAS, caminho, conteudo, tipo);
            }

            // Se a gravação falhar, o arquivo enviado fica sem dono: o aluno não tem
            // permissão de apagar no Storage, e a exclusão da trilha limpa a pasta depois
            servicos.atividades.entregar(atividadeId, comentario, caminho, uri != null ? nome : null);

            return null;

        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {

                Toast.makeText(TelaAtividadeAluno.this, "Entrega enviada!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void erro(ErroApi erro) {

                btnFazerEntrega.setEnabled(true);

                Toast.makeText(TelaAtividadeAluno.this,
                        "Erro ao enviar entrega: " + erro.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
