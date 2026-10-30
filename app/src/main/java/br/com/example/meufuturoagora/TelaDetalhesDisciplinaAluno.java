package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class TelaDetalhesDisciplinaAluno extends AppCompatActivity {

    private enum Aba { TODOS, ENTREGUES, AVALIADAS }


    private String disciplinaId;

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

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);


        disciplinaId = getIntent().getStringExtra("disciplinaId");
        String disciplinaNome = getIntent().getStringExtra("disciplinaNome");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        TextView tvNomeDisciplinaAluno = findViewById(R.id.tvNomeDisciplinaAluno);

        if (disciplinaNome != null) {
            tvNomeDisciplinaAluno.setText(disciplinaNome);
        }

        // (i): professor e participantes da disciplina
        findViewById(R.id.btnInfoProfessor).setOnClickListener(v ->
                InfoDisciplinaDialog.mostrar(this, disciplinaId)
        );

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

        int corAtiva = getColor(R.color.roxo_primario);
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

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            Map<String, Entrega> entregas = new HashMap<>();

            for (Entrega e : servicos.atividades.minhasEntregas()) {
                entregas.put(e.atividadeId, e);
            }

            List<AtividadeAluno> lista = new ArrayList<>();

            for (Atividade a : servicos.atividades.daDisciplina(disciplinaId)) {

                Entrega entrega = entregas.get(a.id);
                Status status = entrega == null
                        ? Status.PENDENTE
                        : (entrega.avaliada() ? Status.AVALIADA : Status.ENTREGUE);

                lista.add(new AtividadeAluno(
                        a.id,
                        a.nome != null ? a.nome : "Trilha",
                        a.prazo != null ? Datas.formatar(a.prazo) : "",
                        status,
                        entrega != null ? entrega.nota : null
                ));
            }

            return lista;

        }, new Tarefas.Retorno<List<AtividadeAluno>>() {
            @Override
            public void sucesso(List<AtividadeAluno> atividades) {

                listaCompleta.clear();
                listaCompleta.addAll(atividades);
                aplicarFiltro();
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(TelaDetalhesDisciplinaAluno.this,
                        "Erro ao carregar trilhas: " + erro.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
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
