package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TelaDetalhesDisciplinaAluno extends AppCompatActivity {

    private enum Aba { TODOS, ENTREGUES, AVALIADAS }

    private FirebaseFirestore db;

    private String disciplinaId;
    private String alunoId;

    private TextView tabTodos, tabEntregues, tabAvaliadas;
    private View linhaAbaTodos, linhaAbaEntregues, linhaAbaAvaliadas;

    private RecyclerView recyclerAtividadesAluno;
    private TextView tvSemAtividadesDisciplinaAluno;

    private Aba abaAtual = Aba.TODOS;

    private AtividadeAlunoAdapter adapter;
    private final List<AtividadeAluno> listaCompleta = new ArrayList<>();
    private final List<AtividadeAluno> listaFiltrada = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_detalhes_disciplina_aluno);

        db = FirebaseFirestore.getInstance();

        disciplinaId = getIntent().getStringExtra("disciplinaId");
        String disciplinaNome = getIntent().getStringExtra("disciplinaNome");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        TextView tvNomeDisciplinaAluno = findViewById(R.id.tvNomeDisciplinaAluno);

        if (disciplinaNome != null) {
            tvNomeDisciplinaAluno.setText(disciplinaNome);
        }

        tabTodos = findViewById(R.id.tabTodos);
        tabEntregues = findViewById(R.id.tabEntregues);
        tabAvaliadas = findViewById(R.id.tabAvaliadas);

        linhaAbaTodos = findViewById(R.id.linhaAbaTodos);
        linhaAbaEntregues = findViewById(R.id.linhaAbaEntregues);
        linhaAbaAvaliadas = findViewById(R.id.linhaAbaAvaliadas);

        tabTodos.setOnClickListener(v -> mostrarAba(Aba.TODOS));
        tabEntregues.setOnClickListener(v -> mostrarAba(Aba.ENTREGUES));
        tabAvaliadas.setOnClickListener(v -> mostrarAba(Aba.AVALIADAS));

        recyclerAtividadesAluno = findViewById(R.id.recyclerAtividadesAluno);
        tvSemAtividadesDisciplinaAluno = findViewById(R.id.tvSemAtividadesDisciplinaAluno);

        recyclerAtividadesAluno.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AtividadeAlunoAdapter(listaFiltrada);
        recyclerAtividadesAluno.setAdapter(adapter);

        carregarAtividades();
    }

    @Override
    protected void onResume() {
        super.onResume();

        if (adapter != null) {
            carregarAtividades();
        }
    }

    private void mostrarAba(Aba aba) {

        abaAtual = aba;

        int corAtiva = getColor(R.color.roxo_acao);
        int corInativa = getColor(R.color.texto_escuro);

        tabTodos.setTextColor(aba == Aba.TODOS ? corAtiva : corInativa);
        tabEntregues.setTextColor(aba == Aba.ENTREGUES ? corAtiva : corInativa);
        tabAvaliadas.setTextColor(aba == Aba.AVALIADAS ? corAtiva : corInativa);

        linhaAbaTodos.setVisibility(aba == Aba.TODOS ? View.VISIBLE : View.GONE);
        linhaAbaEntregues.setVisibility(aba == Aba.ENTREGUES ? View.VISIBLE : View.GONE);
        linhaAbaAvaliadas.setVisibility(aba == Aba.AVALIADAS ? View.VISIBLE : View.GONE);

        aplicarFiltro();
    }

    private void aplicarFiltro() {

        listaFiltrada.clear();

        for (AtividadeAluno atividade : listaCompleta) {

            boolean incluir = abaAtual == Aba.TODOS
                    || (abaAtual == Aba.ENTREGUES && (atividade.status == Status.ENTREGUE || atividade.status == Status.AVALIADA))
                    || (abaAtual == Aba.AVALIADAS && atividade.status == Status.AVALIADA);

            if (incluir) {
                listaFiltrada.add(atividade);
            }
        }

        adapter.notifyDataSetChanged();

        tvSemAtividadesDisciplinaAluno.setVisibility(
                listaFiltrada.isEmpty() ? View.VISIBLE : View.GONE
        );

        recyclerAtividadesAluno.setVisibility(
                listaFiltrada.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    private void carregarAtividades() {

        if (disciplinaId == null) {
            return;
        }

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return;
        }

        alunoId = usuario.getUid();

        db.collection("atividades")
                .whereEqualTo("disciplinaId", disciplinaId)
                .whereEqualTo("ativo", true)
                .get()
                .addOnSuccessListener(this::processarAtividades);
    }

    private void processarAtividades(com.google.firebase.firestore.QuerySnapshot atividadesSnapshot) {

        listaCompleta.clear();

        List<QueryDocumentSnapshot> documentos = new ArrayList<>();

        for (QueryDocumentSnapshot documento : atividadesSnapshot) {
            documentos.add(documento);
        }

        if (documentos.isEmpty()) {
            aplicarFiltro();
            return;
        }

        int[] restantes = {documentos.size()};

        for (QueryDocumentSnapshot documento : documentos) {

            String atividadeId = documento.getId();
            String nome = documento.getString("nome");
            String prazo = documento.getString("prazo");

            db.collection("entregas")
                    .document(atividadeId + "_" + alunoId)
                    .get()
                    .addOnSuccessListener(entregaDoc -> {

                        Status status = Status.PENDENTE;
                        Double nota = null;

                        if (entregaDoc.exists()) {

                            Boolean avaliado = entregaDoc.getBoolean("avaliado");
                            status = Boolean.TRUE.equals(avaliado) ? Status.AVALIADA : Status.ENTREGUE;
                            nota = entregaDoc.getDouble("nota");
                        }

                        listaCompleta.add(new AtividadeAluno(
                                atividadeId,
                                nome != null ? nome : "Trilha",
                                prazo != null ? prazo : "",
                                status,
                                nota
                        ));

                        restantes[0]--;

                        if (restantes[0] == 0) {
                            aplicarFiltro();
                        }
                    });
        }
    }

    private enum Status { PENDENTE, ENTREGUE, AVALIADA }

    public static class AtividadeAluno {

        String id;
        String nome;
        String prazo;
        Status status;
        Double nota;

        public AtividadeAluno(String id, String nome, String prazo, Status status, Double nota) {
            this.id = id;
            this.nome = nome;
            this.prazo = prazo;
            this.status = status;
            this.nota = nota;
        }
    }

    private class AtividadeAlunoAdapter
            extends RecyclerView.Adapter<AtividadeAlunoAdapter.ViewHolder> {

        private final List<AtividadeAluno> atividades;

        public AtividadeAlunoAdapter(List<AtividadeAluno> atividades) {
            this.atividades = atividades;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(
                    R.layout.item_atividade_aluno, parent, false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            AtividadeAluno atividade = atividades.get(position);

            holder.tvNomeAtividadeAluno.setText(atividade.nome);
            holder.tvPrazoAtividadeAluno.setText("Prazo: " + atividade.prazo);

            switch (atividade.status) {

                case AVALIADA:
                    holder.tvStatusAtividadeAluno.setText(
                            atividade.nota != null ? "Nota: " + atividade.nota : "Avaliada"
                    );
                    holder.tvStatusAtividadeAluno.setBackgroundResource(R.drawable.bg_pill_verde);
                    holder.tvStatusAtividadeAluno.setTextColor(getColor(R.color.verde_sucesso));
                    break;

                case ENTREGUE:
                    holder.tvStatusAtividadeAluno.setText("Entregue");
                    holder.tvStatusAtividadeAluno.setBackgroundResource(R.drawable.bg_card_branco);
                    holder.tvStatusAtividadeAluno.setTextColor(getColor(R.color.roxo_primario));
                    break;

                default:
                    holder.tvStatusAtividadeAluno.setText("Pendente");
                    holder.tvStatusAtividadeAluno.setBackgroundResource(R.drawable.bg_pill_vermelho);
                    holder.tvStatusAtividadeAluno.setTextColor(getColor(R.color.vermelho_erro));
                    break;
            }

            holder.itemView.setOnClickListener(v -> {

                Intent intent = new Intent(
                        TelaDetalhesDisciplinaAluno.this,
                        TelaAtividadeAluno.class
                );

                intent.putExtra("atividadeId", atividade.id);
                intent.putExtra("atividadeNome", atividade.nome);

                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return atividades.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvNomeAtividadeAluno;
            TextView tvPrazoAtividadeAluno;
            TextView tvStatusAtividadeAluno;

            public ViewHolder(View itemView) {
                super(itemView);

                tvNomeAtividadeAluno = itemView.findViewById(R.id.tvNomeAtividadeAluno);
                tvPrazoAtividadeAluno = itemView.findViewById(R.id.tvPrazoAtividadeAluno);
                tvStatusAtividadeAluno = itemView.findViewById(R.id.tvStatusAtividadeAluno);
            }
        }
    }
}
