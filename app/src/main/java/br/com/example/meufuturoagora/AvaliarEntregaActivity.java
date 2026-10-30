package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import br.com.example.meufuturoagora.dados.Arquivos;
import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class AvaliarEntregaActivity extends AppCompatActivity {

    private String atividadeId;
    private String alunoId;
    // Vem da notificação de nova entrega, que sabe o id da entrega e não o do aluno
    private String entregaId;

    // A entrega do aluno nesta trilha; null enquanto carrega
    private Entrega entrega;

    private TextView tvNomeAnexoAluno;
    private TextView tvComentarioAluno;
    private EditText edtNota;
    private EditText edtComentarioProfessor;
    private MaterialButton btnAvaliar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_avaliar_entrega);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        atividadeId = getIntent().getStringExtra("atividadeId");
        alunoId = getIntent().getStringExtra("alunoId");
        entregaId = getIntent().getStringExtra("entregaId");
        String alunoNome = getIntent().getStringExtra("alunoNome");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        TextView tvNomeAlunoAvaliar = findViewById(R.id.tvNomeAlunoAvaliar);
        tvNomeAlunoAvaliar.setText(alunoNome != null ? alunoNome : "Aluno");

        tvNomeAnexoAluno = findViewById(R.id.tvNomeAnexoAluno);
        tvComentarioAluno = findViewById(R.id.tvComentarioAluno);
        edtNota = findViewById(R.id.edtNota);
        edtComentarioProfessor = findViewById(R.id.edtComentarioProfessor);

        findViewById(R.id.itemAnexoAluno).setOnClickListener(v -> abrirAnexo());

        btnAvaliar = findViewById(R.id.btnAvaliar);
        btnAvaliar.setOnClickListener(v -> salvarAvaliacao());

        carregarEntrega();
    }

    private void carregarEntrega() {

        if (atividadeId == null || (alunoId == null && entregaId == null)) {
            return;
        }

        Tarefas.executar(this, () -> {

            for (Entrega e : Servicos.get(this).atividades.entregasDaAtividade(atividadeId)) {

                if (alunoId != null ? alunoId.equals(e.alunoId) : entregaId.equals(e.id)) {
                    return e;
                }
            }

            return null;

        }, new Tarefas.Retorno<Entrega>() {
            @Override
            public void sucesso(Entrega encontrada) {

                if (encontrada == null) {
                    return;
                }

                entrega = encontrada;

                tvNomeAnexoAluno.setText(
                        entrega.arquivoNome != null && !entrega.arquivoNome.isEmpty()
                                ? entrega.arquivoNome
                                : "Nenhum anexo"
                );

                tvComentarioAluno.setText(
                        entrega.comentarioAluno != null && !entrega.comentarioAluno.isEmpty()
                                ? entrega.comentarioAluno
                                : "—"
                );

                if (entrega.nota != null) {
                    edtNota.setText(String.valueOf(entrega.nota));
                }

                if (entrega.comentarioProfessor != null) {
                    edtComentarioProfessor.setText(entrega.comentarioProfessor);
                }
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(AvaliarEntregaActivity.this, "Erro ao carregar entrega.", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void abrirAnexo() {

        if (entrega == null || entrega.arquivoPath == null || entrega.arquivoPath.isEmpty()) {

            Toast.makeText(this, "Nenhum anexo disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArquivoUtil.abrir(this, Arquivos.BUCKET_ENTREGAS, entrega.arquivoPath);
    }

    private void salvarAvaliacao() {

        if (entrega == null) {
            return;
        }

        String notaTexto = edtNota.getText().toString().trim().replace(',', '.');

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

        String comentario = edtComentarioProfessor.getText().toString().trim();
        String entregaId = entrega.id;

        btnAvaliar.setEnabled(false);

        // O servidor confere se a nota cabe na trilha e atualiza o ranking sozinho
        Tarefas.executar(this, () -> {
            Servicos.get(this).atividades.avaliar(entregaId, nota, comentario);
            return null;
        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {

                Toast.makeText(AvaliarEntregaActivity.this, "Avaliação salva!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void erro(ErroApi erro) {

                btnAvaliar.setEnabled(true);

                Toast.makeText(AvaliarEntregaActivity.this,
                        "Erro ao salvar avaliação: " + erro.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
