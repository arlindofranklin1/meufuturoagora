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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.example.meufuturoagora.dados.Arquivos;
import br.com.example.meufuturoagora.dados.AtividadeRepositorio;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class TelaDetalhesDisciplinaProfessor
        extends AppCompatActivity {

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
    private final List<AlunoItem> listaNomesAlunos = new ArrayList<>();

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
        // (o carregamento inicial acontece em onResume, que
        // sempre roda logo após onCreate)

        // =================================================
        // CARREGAR ALUNOS
        // =================================================
        // (o carregamento inicial acontece em onResume, que
        // sempre roda logo após onCreate)
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

        Tarefas.executar(this, () -> {

            List<AlunoItem> alunos = new ArrayList<>();

            for (Participante p : Servicos.get(this).disciplinas.participantes(disciplinaId)) {

                if ("aluno".equals(p.tipo)) {
                    alunos.add(new AlunoItem(p.perfilId, p.nome, p.fotoPath, p.fotoGoogleUrl));
                }
            }

            return alunos;

        }, new Tarefas.Retorno<List<AlunoItem>>() {
            @Override
            public void sucesso(List<AlunoItem> alunos) {

                listaNomesAlunos.clear();
                listaNomesAlunos.addAll(alunos);
                mostrarAlunos();
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(TelaDetalhesDisciplinaProfessor.this,
                        "Erro ao carregar alunos: " + erro.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarAlunos() {

        adapterAlunos.notifyDataSetChanged();

        if (abaAtual == Aba.ALUNOS) {

            recyclerAlunos.setVisibility(
                    listaNomesAlunos.isEmpty() ? View.GONE : View.VISIBLE
            );

            tvSemAlunosDisciplina.setVisibility(
                    listaNomesAlunos.isEmpty() ? View.VISIBLE : View.GONE
            );
        }
    }

    // =====================================================
    // CARREGAR DISCIPLINA
    // =====================================================

    private void carregarDisciplina() {

        if (disciplinaId == null) {
            return;
        }

        Tarefas.executar(this, () -> Servicos.get(this).disciplinas.buscar(disciplinaId),
                new Tarefas.Retorno<Disciplina>() {
                    @Override
                    public void sucesso(Disciplina disciplina) {

                        if (disciplina == null || disciplina.nome == null) {
                            return;
                        }

                        // Nome da disciplina com a turma entre parênteses
                        tvNomeDisciplina.setText(
                                disciplina.turmaNome != null && !disciplina.turmaNome.isEmpty()
                                        ? disciplina.nome + " (" + disciplina.turmaNome + ")"
                                        : disciplina.nome
                        );
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        // O nome fica como está no layout
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

        Tarefas.executar(this, () -> Servicos.get(this).atividades.daDisciplina(disciplinaId),
                new Tarefas.Retorno<List<br.com.example.meufuturoagora.dados.Atividade>>() {
                    @Override
                    public void sucesso(List<br.com.example.meufuturoagora.dados.Atividade> atividades) {

                        listaAtividades.clear();

                        for (br.com.example.meufuturoagora.dados.Atividade a : atividades) {

                            listaAtividades.add(new Atividade(
                                    a.id,
                                    a.nome,
                                    a.pontos,
                                    a.prazo != null ? Datas.formatar(a.prazo) : ""
                            ));
                        }

                        adapter.notifyDataSetChanged();
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Toast.makeText(TelaDetalhesDisciplinaProfessor.this,
                                "Erro ao carregar trilhas.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =====================================================
    // EXCLUIR ATIVIDADE
    // =====================================================

    // Decisão D6: o aviso diz quantas entregas serão apagadas junto
    private void confirmarExclusao(String atividadeId) {

        Tarefas.executar(this, () -> Servicos.get(this).atividades.entregasDaAtividade(atividadeId).size(),
                new Tarefas.Retorno<Integer>() {
                    @Override
                    public void sucesso(Integer entregas) {

                        String aviso = entregas == 0
                                ? "Nenhum aluno entregou esta trilha."
                                : (entregas == 1
                                ? "A entrega de 1 aluno e o arquivo dela serão apagados junto."
                                : "As entregas de " + entregas + " alunos e os arquivos delas serão apagados junto.");

                        new AlertDialog.Builder(TelaDetalhesDisciplinaProfessor.this)
                                .setTitle("Excluir trilha")
                                .setMessage("Tem certeza que deseja excluir esta trilha?\n\n" + aviso
                                        + "\n\nEsta ação não pode ser desfeita.")
                                .setNegativeButton("Cancelar", null)
                                .setPositiveButton("Excluir", (dialog, which) -> excluirAtividade(atividadeId))
                                .show();
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Toast.makeText(TelaDetalhesDisciplinaProfessor.this,
                                "Erro ao excluir trilha: " + erro.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // O Storage não apaga por SQL: o app tira os arquivos de lá e depois apaga a linha
    private void excluirAtividade(String atividadeId) {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            AtividadeRepositorio.ArquivosDaAtividade arquivos = servicos.atividades.arquivosDaAtividade(atividadeId);

            if (arquivos.anexo != null) {
                servicos.arquivos.apagar(Arquivos.BUCKET_ATIVIDADES, Collections.singletonList(arquivos.anexo));
            }

            if (!arquivos.entregas.isEmpty()) {
                servicos.arquivos.apagar(Arquivos.BUCKET_ENTREGAS, arquivos.entregas);
            }

            servicos.atividades.excluir(atividadeId);
            return null;

        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {

                Toast.makeText(TelaDetalhesDisciplinaProfessor.this, "Trilha excluída!", Toast.LENGTH_SHORT).show();
                carregarAtividades();
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(TelaDetalhesDisciplinaProfessor.this,
                        "Erro ao excluir trilha: " + erro.getMessage(), Toast.LENGTH_SHORT).show();
            }
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

    private static class AlunoItem {

        String id;
        String nome;
        String fotoPath;
        String fotoGoogleUrl;

        AlunoItem(String id, String nome, String fotoPath, String fotoGoogleUrl) {
            this.id = id;
            this.nome = nome;
            this.fotoPath = fotoPath;
            this.fotoGoogleUrl = fotoGoogleUrl;
        }
    }

    private class AlunoDisciplinaAdapter
            extends RecyclerView.Adapter<AlunoDisciplinaAdapter.ViewHolder> {

        private final List<AlunoItem> alunos;

        public AlunoDisciplinaAdapter(List<AlunoItem> alunos) {
            this.alunos = alunos;
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

            AlunoItem item = alunos.get(position);

            holder.tvNomeAlunoDisciplina.setText(item.nome);
            holder.imgAlunoDisciplina.setImageResource(R.drawable.ic_perfil);
            PerfilFotoUtil.mostrar(
                    TelaDetalhesDisciplinaProfessor.this,
                    item.fotoPath,
                    item.fotoGoogleUrl,
                    holder.imgAlunoDisciplina
            );
        }

        @Override
        public int getItemCount() {
            return alunos.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvNomeAlunoDisciplina;
            ImageView imgAlunoDisciplina;

            public ViewHolder(View itemView) {
                super(itemView);

                tvNomeAlunoDisciplina = itemView.findViewById(R.id.tvNomeAlunoDisciplina);
                imgAlunoDisciplina = itemView.findViewById(R.id.imgAlunoDisciplina);
            }
        }
    }
}