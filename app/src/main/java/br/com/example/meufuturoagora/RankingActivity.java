package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private TextView tvNomePrimeiro, tvPontosPrimeiro;
    private TextView tvNomeSegundo, tvPontosSegundo;
    private TextView tvNomeTerceiro, tvPontosTerceiro;

    private RecyclerView recyclerRanking;
    private TextView tvSemDados;

    private RankingAdapter adapter;
    private final List<Aluno> listaAlunos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ranking);

        db = FirebaseFirestore.getInstance();

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        tvNomePrimeiro = findViewById(R.id.tvNomePrimeiro);
        tvPontosPrimeiro = findViewById(R.id.tvPontosPrimeiro);
        tvNomeSegundo = findViewById(R.id.tvNomeSegundo);
        tvPontosSegundo = findViewById(R.id.tvPontosSegundo);
        tvNomeTerceiro = findViewById(R.id.tvNomeTerceiro);
        tvPontosTerceiro = findViewById(R.id.tvPontosTerceiro);

        recyclerRanking = findViewById(R.id.recyclerRanking);
        tvSemDados = findViewById(R.id.tvSemDados);

        recyclerRanking.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RankingAdapter(listaAlunos);
        recyclerRanking.setAdapter(adapter);

        carregarRanking();
    }

    private void carregarRanking() {

        db.collection("alunos")
                .orderBy("pontuacao", Query.Direction.DESCENDING)
                .limit(50)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    listaAlunos.clear();

                    for (QueryDocumentSnapshot documento : querySnapshot) {

                        String nome = documento.getString("nome");
                        Long pontuacao = documento.getLong("pontuacao");

                        if (nome != null) {

                            listaAlunos.add(new Aluno(
                                    nome,
                                    pontuacao != null ? pontuacao : 0
                            ));
                        }
                    }

                    preencherPodio();

                    adapter.notifyDataSetChanged();

                    tvSemDados.setVisibility(
                            listaAlunos.isEmpty() ? View.VISIBLE : View.GONE
                    );

                    recyclerRanking.setVisibility(
                            listaAlunos.isEmpty() ? View.GONE : View.VISIBLE
                    );
                })
                .addOnFailureListener(e -> {

                    tvSemDados.setVisibility(View.VISIBLE);
                    recyclerRanking.setVisibility(View.GONE);
                });
    }

    private void preencherPodio() {

        if (listaAlunos.size() > 0) {

            tvNomePrimeiro.setText(listaAlunos.get(0).nome);
            tvPontosPrimeiro.setText(listaAlunos.get(0).pontuacao + " pts");
        }

        if (listaAlunos.size() > 1) {

            tvNomeSegundo.setText(listaAlunos.get(1).nome);
            tvPontosSegundo.setText(listaAlunos.get(1).pontuacao + " pts");
        }

        if (listaAlunos.size() > 2) {

            tvNomeTerceiro.setText(listaAlunos.get(2).nome);
            tvPontosTerceiro.setText(listaAlunos.get(2).pontuacao + " pts");
        }
    }

    public static class Aluno {

        String nome;
        long pontuacao;

        public Aluno(String nome, long pontuacao) {
            this.nome = nome;
            this.pontuacao = pontuacao;
        }
    }

    private class RankingAdapter
            extends RecyclerView.Adapter<RankingAdapter.ViewHolder> {

        private final List<Aluno> alunos;

        public RankingAdapter(List<Aluno> alunos) {
            this.alunos = alunos;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(
                    R.layout.item_ranking, parent, false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            Aluno aluno = alunos.get(position);

            holder.tvPosicao.setText(String.valueOf(position + 1));
            holder.tvNomeAluno.setText(aluno.nome);
            holder.tvPontosAluno.setText(aluno.pontuacao + " pts");
        }

        @Override
        public int getItemCount() {
            return alunos.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvPosicao;
            TextView tvNomeAluno;
            TextView tvPontosAluno;

            public ViewHolder(View itemView) {
                super(itemView);

                tvPosicao = itemView.findViewById(R.id.tvPosicao);
                tvNomeAluno = itemView.findViewById(R.id.tvNomeAluno);
                tvPontosAluno = itemView.findViewById(R.id.tvPontosAluno);
            }
        }
    }
}
