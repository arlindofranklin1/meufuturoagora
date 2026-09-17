package br.com.example.meufuturoagora;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class TelaDetalhesDisciplinaProfessor
        extends AppCompatActivity {

    // =====================================================
    // FIRESTORE
    // =====================================================

    private FirebaseFirestore db;

    // =====================================================
    // DISCIPLINA
    // =====================================================

    private String disciplinaId;
    private String disciplinaCor;

    private TextView tvNomeDisciplina;

    // =====================================================
    // ATIVIDADES
    // =====================================================

    private RecyclerView recyclerAtividades;

    private AtividadeAdapter adapter;

    private final List<Atividade> listaAtividades =
            new ArrayList<>();

    // =====================================================
    // ABAS
    // =====================================================

    private TextView tabAtividades, tabAlunos, tabFrequencia;
    private View linhaAbaAtividades, linhaAbaAlunos, linhaAbaFrequencia;

    private ImageButton btnAdicionarAtividade;

    // =====================================================
    // ALUNOS
    // =====================================================

    private RecyclerView recyclerAlunos;
    private TextView tvSemAlunosDisciplina;

    private AlunoDisciplinaAdapter adapterAlunos;
    private final List<String> listaNomesAlunos = new ArrayList<>();

    // =====================================================
    // FREQUÊNCIA
    // =====================================================

    private LinearLayout layoutFrequenciaAcoes;

    // =====================================================
    // ON CREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(
                R.layout.activity_tela_detalhes_disciplina_professor
        );

        // =================================================
        // INSETS
        // =================================================

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars =
                            insets.getInsets(
                                    WindowInsetsCompat.Type.systemBars()
                            );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // =================================================
        // FIRESTORE
        // =================================================

        db = FirebaseFirestore.getInstance();

        // =================================================
        // RECEBER DADOS DA DISCIPLINA
        // =================================================

        disciplinaId =
                getIntent().getStringExtra(
                        "disciplinaId"
                );

        disciplinaCor =
                getIntent().getStringExtra(
                        "disciplinaCor"
                );

        // =================================================
        // BOTÃO ADICIONAR ATIVIDADE
        // =================================================

        btnAdicionarAtividade =
                findViewById(
                        R.id.btnAdicionarAtividade
                );

        btnAdicionarAtividade.setOnClickListener(v -> {

            Intent intent = new Intent(
                    TelaDetalhesDisciplinaProfessor.this,
                    CriarAtividade.class
            );

            intent.putExtra(
                    "disciplinaId",
                    disciplinaId
            );

            intent.putExtra(
                    "disciplinaCor",
                    disciplinaCor
            );

            startActivity(intent);
        });

        // =================================================
        // COR DO BOTÃO ADICIONAR
        // =================================================

        if (disciplinaCor != null &&
                !disciplinaCor.isEmpty()) {

            GradientDrawable fundoBotao =
                    new GradientDrawable();

            fundoBotao.setShape(
                    GradientDrawable.OVAL
            );

            fundoBotao.setColor(
                    Color.parseColor(disciplinaCor)
            );

            btnAdicionarAtividade.setBackground(
                    fundoBotao
            );
        }

        // =================================================
        // COMPONENTES
        // =================================================

        tvNomeDisciplina =
                findViewById(
                        R.id.tvNomeDisciplina
                );

        recyclerAtividades =
                findViewById(
                        R.id.recyclerAtividades
                );

        // =================================================
        // RECYCLERVIEW
        // =================================================

        recyclerAtividades.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter =
                new AtividadeAdapter(
                        listaAtividades
                );

        recyclerAtividades.setAdapter(
                adapter
        );

        // =================================================
        // ALUNOS
        // =================================================

        recyclerAlunos = findViewById(R.id.recyclerAlunos);
        tvSemAlunosDisciplina = findViewById(R.id.tvSemAlunosDisciplina);

        recyclerAlunos.setLayoutManager(new LinearLayoutManager(this));
        adapterAlunos = new AlunoDisciplinaAdapter(listaNomesAlunos);
        recyclerAlunos.setAdapter(adapterAlunos);

        // =================================================
        // FREQUÊNCIA
        // =================================================

        layoutFrequenciaAcoes = findViewById(R.id.layoutFrequenciaAcoes);

        findViewById(R.id.itemCadastrarAula).setOnClickListener(v -> {

            Intent intent = new Intent(this, CadastrarAulaActivity.class);
            intent.putExtra("disciplinaId", disciplinaId);
            startActivity(intent);
        });

        findViewById(R.id.itemRegistrarFrequencia).setOnClickListener(v -> {

            Intent intent = new Intent(this, RegistrarFrequenciaActivity.class);
            intent.putExtra("disciplinaId", disciplinaId);
            startActivity(intent);
        });

        // =================================================
        // ABAS
        // =================================================

        tabAtividades = findViewById(R.id.tabAtividades);
        tabAlunos = findViewById(R.id.tabAlunos);
        tabFrequencia = findViewById(R.id.tabFrequencia);

        linhaAbaAtividades = findViewById(R.id.linhaAbaAtividades);
        linhaAbaAlunos = findViewById(R.id.linhaAbaAlunos);
        linhaAbaFrequencia = findViewById(R.id.linhaAbaFrequencia);

        tabAtividades.setOnClickListener(v -> mostrarAba(Aba.ATIVIDADES));
        tabAlunos.setOnClickListener(v -> mostrarAba(Aba.ALUNOS));
        tabFrequencia.setOnClickListener(v -> mostrarAba(Aba.FREQUENCIA));

        // =================================================
        // BOTÃO VOLTAR
        // =================================================

        ImageView btnVoltar =
                findViewById(
                        R.id.btnVoltar
                );

        btnVoltar.setOnClickListener(v ->
                finish()
        );

        // =================================================
        // CARREGAR DISCIPLINA
        // =================================================

        carregarDisciplina();

        // =================================================
        // CARREGAR ATIVIDADES
        // =================================================

        carregarAtividades();

        // =================================================
        // CARREGAR ALUNOS
        // =================================================

        carregarAlunos();
    }

    // =====================================================
    // ABAS
    // =====================================================

    private enum Aba { ATIVIDADES, ALUNOS, FREQUENCIA }

    private Aba abaAtual = Aba.ATIVIDADES;

    private void mostrarAba(Aba aba) {

        abaAtual = aba;

        recyclerAtividades.setVisibility(aba == Aba.ATIVIDADES ? View.VISIBLE : View.GONE);
        btnAdicionarAtividade.setVisibility(aba == Aba.ATIVIDADES ? View.VISIBLE : View.GONE);

        boolean mostrarAlunos = aba == Aba.ALUNOS;
        boolean semAlunos = listaNomesAlunos.isEmpty();

        recyclerAlunos.setVisibility(mostrarAlunos && !semAlunos ? View.VISIBLE : View.GONE);
        tvSemAlunosDisciplina.setVisibility(mostrarAlunos && semAlunos ? View.VISIBLE : View.GONE);

        layoutFrequenciaAcoes.setVisibility(aba == Aba.FREQUENCIA ? View.VISIBLE : View.GONE);

        int corAtiva = getColor(R.color.roxo_primario);
        int corInativa = Color.parseColor("#171717");

        tabAtividades.setTextColor(aba == Aba.ATIVIDADES ? corAtiva : corInativa);
        tabAlunos.setTextColor(aba == Aba.ALUNOS ? corAtiva : corInativa);
        tabFrequencia.setTextColor(aba == Aba.FREQUENCIA ? corAtiva : corInativa);

        linhaAbaAtividades.setVisibility(aba == Aba.ATIVIDADES ? View.VISIBLE : View.GONE);
        linhaAbaAlunos.setVisibility(aba == Aba.ALUNOS ? View.VISIBLE : View.GONE);
        linhaAbaFrequencia.setVisibility(aba == Aba.FREQUENCIA ? View.VISIBLE : View.GONE);
    }

    // =====================================================
    // CARREGAR ALUNOS MATRICULADOS
    // =====================================================

    private void carregarAlunos() {

        if (disciplinaId == null) {
            return;
        }

        db.collection("matriculas")
                .whereEqualTo("disciplinaId", disciplinaId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    listaNomesAlunos.clear();

                    for (QueryDocumentSnapshot documento : querySnapshot) {

                        String alunoNome = documento.getString("alunoNome");

                        if (alunoNome != null) {
                            listaNomesAlunos.add(alunoNome);
                        }
                    }

                    adapterAlunos.notifyDataSetChanged();

                    if (abaAtual == Aba.ALUNOS) {

                        recyclerAlunos.setVisibility(
                                listaNomesAlunos.isEmpty() ? View.GONE : View.VISIBLE
                        );

                        tvSemAlunosDisciplina.setVisibility(
                                listaNomesAlunos.isEmpty() ? View.VISIBLE : View.GONE
                        );
                    }
                });
    }

    // =====================================================
    // CARREGAR DISCIPLINA
    // =====================================================

    private void carregarDisciplina() {

        if (disciplinaId == null) {
            return;
        }

        db.collection("disciplinas")
                .document(disciplinaId)
                .get()
                .addOnSuccessListener(documento -> {

                    if (documento.exists()) {

                        String nome =
                                documento.getString("nome");

                        if (nome != null) {

                            tvNomeDisciplina.setText(
                                    nome
                            );
                        }
                    }
                });
    }

    // =====================================================
    // CARREGAR ATIVIDADES
    // =====================================================

    private void carregarAtividades() {

        if (disciplinaId == null) {
            return;
        }

        db.collection("atividades")
                .whereEqualTo(
                        "disciplinaId",
                        disciplinaId
                )
                .whereEqualTo(
                        "ativo",
                        true
                )
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    listaAtividades.clear();

                    for (
                            QueryDocumentSnapshot documento :
                            querySnapshot
                    ) {

                        String id =
                                documento.getId();

                        String nome =
                                documento.getString(
                                        "nome"
                                );

                        Long pontos =
                                documento.getLong(
                                        "pontos"
                                );

                        String prazo =
                                documento.getString(
                                        "prazo"
                                );

                        if (nome != null) {

                            listaAtividades.add(
                                    new Atividade(
                                            id,
                                            nome,
                                            pontos != null
                                                    ? pontos
                                                    : 0,
                                            prazo != null
                                                    ? prazo
                                                    : ""
                                    )
                            );
                        }
                    }

                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao carregar atividades.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =====================================================
    // EXCLUIR ATIVIDADE
    // =====================================================

    private void confirmarExclusao(String atividadeId) {

        new AlertDialog.Builder(this)
                .setTitle("Excluir atividade")
                .setMessage(
                        "Tem certeza que deseja excluir esta atividade?"
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Excluir",
                        (dialog, which) ->
                                excluirAtividade(atividadeId)
                )
                .show();
    }

    private void excluirAtividade(String atividadeId) {

        db.collection("atividades")
                .document(atividadeId)
                .delete()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Atividade excluída!",
                            Toast.LENGTH_SHORT
                    ).show();

                    carregarAtividades();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao excluir atividade.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =====================================================
    // VOLTOU PARA A TELA
    // =====================================================

    @Override
    protected void onResume() {

        super.onResume();

        if (adapter != null) {
            carregarAtividades();
        }

        if (adapterAlunos != null) {
            carregarAlunos();
        }
    }

    // =====================================================
    // MODELO DA ATIVIDADE
    // =====================================================

    public static class Atividade {

        String id;
        String nome;
        long pontos;
        String prazo;

        public Atividade(
                String id,
                String nome,
                long pontos,
                String prazo
        ) {

            this.id = id;
            this.nome = nome;
            this.pontos = pontos;
            this.prazo = prazo;
        }
    }

    // =====================================================
    // ADAPTER
    // =====================================================

    private class AtividadeAdapter
            extends RecyclerView.Adapter<
            AtividadeAdapter.ViewHolder> {

        private final List<Atividade> atividades;

        public AtividadeAdapter(
                List<Atividade> atividades
        ) {

            this.atividades = atividades;
        }

        // =================================================
        // CRIAR ITEM
        // =================================================

        @Override
        public ViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {

            View view =
                    getLayoutInflater().inflate(
                            R.layout.item_atividade_professor,
                            parent,
                            false
                    );

            return new ViewHolder(view);
        }

        // =================================================
        // PREENCHER ITEM
        // =================================================

        @Override
        public void onBindViewHolder(
                ViewHolder holder,
                int position
        ) {

            Atividade atividade =
                    atividades.get(position);

            // =============================================
            // TEXTOS
            // =============================================

            holder.tvNomeAtividade.setText(
                    atividade.nome
            );

            holder.tvPontuacao.setText(
                    atividade.pontos + " pts"
            );

            holder.tvPrazo.setText(
                    "Prazo: " + atividade.prazo
            );

            // =============================================
            // ABRIR QUESTÕES / ENTREGAS
            // =============================================

            holder.itemView.setOnClickListener(v -> {

                Intent intent = new Intent(
                        TelaDetalhesDisciplinaProfessor.this,
                        QuestoesDoLivroActivity.class
                );

                intent.putExtra("atividadeId", atividade.id);
                intent.putExtra("atividadeNome", atividade.nome);
                intent.putExtra("disciplinaId", disciplinaId);

                startActivity(intent);
            });

            // =============================================
            // MENU DE OPÇÕES
            // =============================================

            holder.btnMenuAtividade.setOnClickListener(v -> {

                PopupMenu popupMenu =
                        new PopupMenu(
                                TelaDetalhesDisciplinaProfessor.this,
                                holder.btnMenuAtividade
                        );

                popupMenu.getMenu().add(
                        "Editar"
                );

                popupMenu.getMenu().add(
                        "Excluir"
                );

                popupMenu.setOnMenuItemClickListener(
                        item -> {

                            String opcao =
                                    item.getTitle()
                                            .toString();

                            // =============================
                            // EDITAR
                            // =============================

                            if (opcao.equals("Editar")) {

                                Intent intent =
                                        new Intent(
                                                TelaDetalhesDisciplinaProfessor.this,
                                                CriarAtividade.class
                                        );

                                intent.putExtra(
                                        "atividadeId",
                                        atividade.id
                                );

                                intent.putExtra(
                                        "disciplinaId",
                                        disciplinaId
                                );

                                intent.putExtra(
                                        "disciplinaCor",
                                        disciplinaCor
                                );

                                startActivity(intent);

                                return true;
                            }

                            // =============================
                            // EXCLUIR
                            // =============================

                            if (opcao.equals("Excluir")) {

                                confirmarExclusao(
                                        atividade.id
                                );

                                return true;
                            }

                            return false;
                        }
                );

                popupMenu.show();
            });

            // =============================================
            // COR DA DISCIPLINA
            // =============================================

            if (disciplinaCor != null &&
                    !disciplinaCor.isEmpty()) {

                int cor =
                        Color.parseColor(
                                disciplinaCor
                        );

                GradientDrawable fundo =
                        new GradientDrawable();

                fundo.setColor(cor);

                fundo.setCornerRadius(
                        14
                );

                holder.fundoAtividade.setBackground(
                        fundo
                );

                holder.imgAtividade.setColorFilter(
                        Color.WHITE,
                        PorterDuff.Mode.SRC_IN
                );
            }
        }

        // =================================================
        // QUANTIDADE
        // =================================================

        @Override
        public int getItemCount() {

            return atividades.size();
        }

        // =================================================
        // VIEW HOLDER
        // =================================================

        class ViewHolder
                extends RecyclerView.ViewHolder {

            FrameLayout fundoAtividade;

            ImageView imgAtividade;

            // CORRIGIDO:
            // XML usa btnMenuAtividade
            ImageView btnMenuAtividade;

            TextView tvNomeAtividade;
            TextView tvPontuacao;
            TextView tvPrazo;

            public ViewHolder(
                    View itemView
            ) {

                super(itemView);

                fundoAtividade =
                        itemView.findViewById(
                                R.id.fundoAtividade
                        );

                imgAtividade =
                        itemView.findViewById(
                                R.id.imgAtividade
                        );

                // CORRIGIDO
                btnMenuAtividade =
                        itemView.findViewById(
                                R.id.btnMenuAtividade
                        );

                tvNomeAtividade =
                        itemView.findViewById(
                                R.id.tvNomeAtividade
                        );

                tvPontuacao =
                        itemView.findViewById(
                                R.id.tvPontuacao
                        );

                tvPrazo =
                        itemView.findViewById(
                                R.id.tvPrazo
                        );
            }
        }
    }

    // =====================================================
    // ADAPTER DE ALUNOS
    // =====================================================

    private class AlunoDisciplinaAdapter
            extends RecyclerView.Adapter<AlunoDisciplinaAdapter.ViewHolder> {

        private final List<String> nomes;

        public AlunoDisciplinaAdapter(List<String> nomes) {
            this.nomes = nomes;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(
                    R.layout.item_aluno_disciplina, parent, false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            holder.tvNomeAlunoDisciplina.setText(nomes.get(position));
        }

        @Override
        public int getItemCount() {
            return nomes.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvNomeAlunoDisciplina;

            public ViewHolder(View itemView) {
                super(itemView);

                tvNomeAlunoDisciplina = itemView.findViewById(R.id.tvNomeAlunoDisciplina);
            }
        }
    }
}