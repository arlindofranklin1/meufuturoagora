package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
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
                        this, "Erro ao carregar atividade.", Toast.LENGTH_SHORT
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

                                for (QueryDocumentSnapshot documento : entregas) {

                                    String alunoId = documento.getString("alunoId");

                                    if (alunoId != null) {
                                        enviouMapa.put(alunoId, true);
                                    }
                                }

                                listaEntregas.clear();

                                for (String[] aluno : alunos) {

                                    listaEntregas.add(new Entrega(
                                            aluno[0],
                                            aluno[1],
                                            Boolean.TRUE.equals(enviouMapa.get(aluno[0]))
                                    ));
                                }

                                adapter.notifyDataSetChanged();

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

    public static class Entrega {

        String alunoId;
        String alunoNome;
        boolean entregue;

        public Entrega(String alunoId, String alunoNome, boolean entregue) {
            this.alunoId = alunoId;
            this.alunoNome = alunoNome;
            this.entregue = entregue;
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

            if (entrega.entregue) {

                holder.tvStatusEntrega.setText("Entregue");
                holder.tvStatusEntrega.setBackgroundResource(R.drawable.bg_pill_verde);
                holder.tvStatusEntrega.setTextColor(getColor(R.color.verde_sucesso));

            } else {

                holder.tvStatusEntrega.setText("Não entregue");
                holder.tvStatusEntrega.setBackgroundResource(R.drawable.bg_pill_vermelho);
                holder.tvStatusEntrega.setTextColor(getColor(R.color.vermelho_erro));
            }

            holder.itemView.setOnClickListener(v -> {

                if (!entrega.entregue) {

                    Toast.makeText(
                            QuestoesDoLivroActivity.this,
                            "Este aluno ainda não entregou a atividade.",
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

            public ViewHolder(View itemView) {
                super(itemView);

                tvNomeAlunoEntrega = itemView.findViewById(R.id.tvNomeAlunoEntrega);
                tvStatusEntrega = itemView.findViewById(R.id.tvStatusEntrega);
            }
        }
    }
}
