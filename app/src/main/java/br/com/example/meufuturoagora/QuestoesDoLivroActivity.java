package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuestoesDoLivroActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private String atividadeId;
    private String disciplinaId;

    private String arquivoUrl;

    private TextView tabDescricao, tabEntregas;
    private View linhaAbaDescricao, linhaAbaEntregas;

    private View scrollDescricao;
    private RecyclerView recyclerEntregas;
    private TextView tvSemEntregas;

    private EntregaAdapter adapter;
    private final List<Entrega> listaEntregas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_questoes_do_livro);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        db = FirebaseFirestore.getInstance();

        atividadeId = getIntent().getStringExtra("atividadeId");
        disciplinaId = getIntent().getStringExtra("disciplinaId");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        TextView tvTituloAtividade = findViewById(R.id.tvTituloAtividade);

        String nomeAtividade = getIntent().getStringExtra("atividadeNome");

        if (nomeAtividade != null && !nomeAtividade.isEmpty()) {
            tvTituloAtividade.setText(nomeAtividade);
        }

        tabDescricao = findViewById(R.id.tabDescricao);
        tabEntregas = findViewById(R.id.tabEntregas);
        linhaAbaDescricao = findViewById(R.id.linhaAbaDescricao);
        linhaAbaEntregas = findViewById(R.id.linhaAbaEntregas);

        scrollDescricao = findViewById(R.id.scrollDescricao);
        recyclerEntregas = findViewById(R.id.recyclerEntregas);
        tvSemEntregas = findViewById(R.id.tvSemEntregas);

        recyclerEntregas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntregaAdapter(listaEntregas);
        recyclerEntregas.setAdapter(adapter);

        tabDescricao.setOnClickListener(v -> mostrarAba(true));
        tabEntregas.setOnClickListener(v -> mostrarAba(false));

        findViewById(R.id.itemAnexo).setOnClickListener(v -> abrirAnexo());

        carregarAtividade();
        carregarEntregas();
    }

    private void mostrarAba(boolean descricao) {

        scrollDescricao.setVisibility(descricao ? View.VISIBLE : View.GONE);
        recyclerEntregas.setVisibility(
                !descricao && !listaEntregas.isEmpty() ? View.VISIBLE : View.GONE
        );
        tvSemEntregas.setVisibility(
                !descricao && listaEntregas.isEmpty() ? View.VISIBLE : View.GONE
        );

        tabDescricao.setTextColor(getColor(
                descricao ? R.color.roxo_primario : R.color.texto_escuro
        ));

        tabEntregas.setTextColor(getColor(
                !descricao ? R.color.roxo_primario : R.color.texto_escuro
        ));

        linhaAbaDescricao.setVisibility(descricao ? View.VISIBLE : View.GONE);
        linhaAbaEntregas.setVisibility(!descricao ? View.VISIBLE : View.GONE);
    }

    private void abrirAnexo() {

        if (arquivoUrl == null || arquivoUrl.isEmpty()) {

            Toast.makeText(this, "Nenhum anexo disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(arquivoUrl)));
    }

    private void carregarAtividade() {

        if (atividadeId == null) {
            return;
        }

        db.collection("atividades")
                .document(atividadeId)
                .get()
                .addOnSuccessListener(this::preencherAtividade)
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao carregar trilha.", Toast.LENGTH_SHORT
                ).show());
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

        arquivoUrl = documento.getString("arquivoUrl");

        TextView tvTituloAtividade = findViewById(R.id.tvTituloAtividade);
        if (nome != null) {
            tvTituloAtividade.setText(nome);
        }

        TextView tvDescricaoAtividade = findViewById(R.id.tvDescricaoAtividade);
        tvDescricaoAtividade.setText(
                descricao != null && !descricao.isEmpty() ? descricao : "Sem descrição."
        );

        TextView tvNomeAnexo = findViewById(R.id.tvNomeAnexo);
        tvNomeAnexo.setText(
                arquivoNome != null && !arquivoNome.isEmpty() ? arquivoNome : "Nenhum anexo"
        );

        TextView tvPontuacaoMaxima = findViewById(R.id.tvPontuacaoMaxima);
        tvPontuacaoMaxima.setText((pontos != null ? pontos : 0) + " pts");

        TextView tvPrazoFinal = findViewById(R.id.tvPrazoFinal);
        tvPrazoFinal.setText(prazo != null ? prazo : "");
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adapter != null) {
            carregarEntregas();
        }
    }

    private void carregarEntregas() {

        if (disciplinaId == null || atividadeId == null) {
            return;
        }

        db.collection("matriculas")
                .whereEqualTo("disciplinaId", disciplinaId)
                .get()
                .addOnSuccessListener(matriculas -> {

                    List<String[]> alunos = new ArrayList<>();

                    for (QueryDocumentSnapshot documento : matriculas) {

                        String alunoId = documento.getString("alunoId");
                        String alunoNome = documento.getString("alunoNome");

                        if (alunoId != null && alunoNome != null) {
                            alunos.add(new String[]{alunoId, alunoNome});
                        }
                    }

                    db.collection("entregas")
                            .whereEqualTo("atividadeId", atividadeId)
                            .get()
                            .addOnSuccessListener(entregas -> {

                                Map<String, Boolean> enviouMapa = new HashMap<>();
                                Map<String, Boolean> avaliadoMapa = new HashMap<>();

                                for (QueryDocumentSnapshot documento : entregas) {

                                    String alunoId = documento.getString("alunoId");

                                    if (alunoId != null) {
                                        enviouMapa.put(alunoId, true);
                                        avaliadoMapa.put(
                                                alunoId,
                                                Boolean.TRUE.equals(documento.getBoolean("avaliado"))
                                        );
                                    }
                                }

                                listaEntregas.clear();

                                List<String> idsAlunos = new ArrayList<>();

                                for (String[] aluno : alunos) {

                                    listaEntregas.add(new Entrega(
                                            aluno[0],
                                            aluno[1],
                                            Boolean.TRUE.equals(enviouMapa.get(aluno[0])),
                                            Boolean.TRUE.equals(avaliadoMapa.get(aluno[0]))
                                    ));

                                    idsAlunos.add(aluno[0]);
                                }

                                adapter.notifyDataSetChanged();
                                carregarFotosAlunos(idsAlunos);

                                boolean vazio = listaEntregas.isEmpty();
                                boolean abaEntregasAtiva =
                                        recyclerEntregas.getVisibility() == View.VISIBLE
                                                || tvSemEntregas.getVisibility() == View.VISIBLE;

                                if (abaEntregasAtiva) {

                                    recyclerEntregas.setVisibility(vazio ? View.GONE : View.VISIBLE);
                                    tvSemEntregas.setVisibility(vazio ? View.VISIBLE : View.GONE);
                                }
                            });
                });
    }

    // Busca a foto de cada aluno na coleção "alunos" (em lotes de até 10,
    // limite do whereIn do Firestore) e preenche na lista já exibida.
    private void carregarFotosAlunos(List<String> idsAlunos) {

        for (int i = 0; i < idsAlunos.size(); i += 10) {

            List<String> lote = idsAlunos.subList(i, Math.min(i + 10, idsAlunos.size()));

            db.collection("alunos")
                    .whereIn(com.google.firebase.firestore.FieldPath.documentId(), lote)
                    .get()
                    .addOnSuccessListener(alunosSnapshot -> {

                        Map<String, String> fotosPorId = new HashMap<>();

                        for (QueryDocumentSnapshot documento : alunosSnapshot) {
                            fotosPorId.put(documento.getId(), documento.getString("fotoUrl"));
                        }

                        for (Entrega entrega : listaEntregas) {

                            if (fotosPorId.containsKey(entrega.alunoId)) {
                                entrega.fotoUrl = fotosPorId.get(entrega.alunoId);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    });
        }
    }

    // =====================================================
    // REFAZER AVALIAÇÃO
    // =====================================================

    private void confirmarRefazerAvaliacao(Entrega entrega) {

        new AlertDialog.Builder(this)
                .setTitle("Refazer avaliação")
                .setMessage("A nota e o comentário dados para " + entrega.alunoNome
                        + " serão apagados e a entrega volta a ficar aguardando avaliação. "
                        + "Deseja refazer a avaliação?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Refazer", (dialog, which) -> refazerAvaliacao(entrega))
                .show();
    }

    private void refazerAvaliacao(Entrega entrega) {

        Map<String, Object> dados = new HashMap<>();
        dados.put("avaliado", false);
        dados.put("nota", FieldValue.delete());
        dados.put("comentarioProfessor", FieldValue.delete());
        dados.put("avaliadoEm", FieldValue.delete());

        db.collection("entregas")
                .document(atividadeId + "_" + entrega.alunoId)
                .update(dados)
                .addOnSuccessListener(unused -> {

                    // A nota antiga sai da pontuação do aluno
                    PontuacaoUtil.recalcular(db, entrega.alunoId);

                    // Abre direto a tela de avaliação para dar a nova nota
                    startActivity(new Intent(this, AvaliarEntregaActivity.class)
                            .putExtra("atividadeId", atividadeId)
                            .putExtra("alunoId", entrega.alunoId)
                            .putExtra("alunoNome", entrega.alunoNome));
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao refazer avaliação: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    public static class Entrega {

        String alunoId;
        String alunoNome;
        String fotoUrl;
        boolean entregue;
        boolean avaliado;

        public Entrega(String alunoId, String alunoNome, boolean entregue, boolean avaliado) {
            this.alunoId = alunoId;
            this.alunoNome = alunoNome;
            this.entregue = entregue;
            this.avaliado = avaliado;
        }
    }

    private class EntregaAdapter extends RecyclerView.Adapter<EntregaAdapter.ViewHolder> {

        private final List<Entrega> entregas;

        public EntregaAdapter(List<Entrega> entregas) {
            this.entregas = entregas;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(R.layout.item_entrega, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            Entrega entrega = entregas.get(position);

            holder.tvNomeAlunoEntrega.setText(entrega.alunoNome);
            holder.imgAlunoEntrega.setImageResource(R.drawable.ic_perfil);
            FotoUtil.carregar(QuestoesDoLivroActivity.this, entrega.fotoUrl, holder.imgAlunoEntrega);

            if (entrega.entregue) {

                holder.tvStatusEntrega.setText(entrega.avaliado ? "Avaliado" : "Entregue");
                holder.tvStatusEntrega.setBackgroundResource(R.drawable.bg_pill_verde);
                holder.tvStatusEntrega.setTextColor(getColor(R.color.verde_sucesso));

            } else {

                holder.tvStatusEntrega.setText("Não entregue");
                holder.tvStatusEntrega.setBackgroundResource(R.drawable.bg_pill_vermelho);
                holder.tvStatusEntrega.setTextColor(getColor(R.color.vermelho_erro));
            }

            // Entrega avaliada: três pontinhos com a opção de refazer a avaliação
            holder.btnMenuEntrega.setVisibility(
                    entrega.entregue && entrega.avaliado ? View.VISIBLE : View.GONE
            );

            holder.btnMenuEntrega.setOnClickListener(v -> {

                PopupMenu menu = new PopupMenu(QuestoesDoLivroActivity.this, holder.btnMenuEntrega);
                menu.getMenu().add("Refazer avaliação");

                menu.setOnMenuItemClickListener(item -> {
                    confirmarRefazerAvaliacao(entrega);
                    return true;
                });

                menu.show();
            });

            holder.itemView.setOnClickListener(v -> {

                if (!entrega.entregue) {

                    Toast.makeText(
                            QuestoesDoLivroActivity.this,
                            "Este aluno ainda não entregou a trilha.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                Intent intent = new Intent(
                        QuestoesDoLivroActivity.this,
                        AvaliarEntregaActivity.class
                );

                intent.putExtra("atividadeId", atividadeId);
                intent.putExtra("alunoId", entrega.alunoId);
                intent.putExtra("alunoNome", entrega.alunoNome);

                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return entregas.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvNomeAlunoEntrega;
            TextView tvStatusEntrega;
            ImageView imgAlunoEntrega;
            ImageView btnMenuEntrega;

            public ViewHolder(View itemView) {
                super(itemView);

                btnMenuEntrega = itemView.findViewById(R.id.btnMenuEntrega);

                tvNomeAlunoEntrega = itemView.findViewById(R.id.tvNomeAlunoEntrega);
                tvStatusEntrega = itemView.findViewById(R.id.tvStatusEntrega);
                imgAlunoEntrega = itemView.findViewById(R.id.imgAlunoEntrega);
            }
        }
    }
}
