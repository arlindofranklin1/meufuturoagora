package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.LinhaRanking;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;
import br.com.example.meufuturoagora.dados.Turma;

// A pontuação e a ordem do ranking são calculadas no servidor (notas e faltas do
// ano letivo atual). Professor: turma inteira, só as turmas em que dá aula (D2).
// Aluno: o pódio e a própria linha.
public class RankingActivity extends AppCompatActivity {

    public static final String EXTRA_PERFIL = "perfil";
    public static final String PERFIL_PROFESSOR = "professor";
    public static final String PERFIL_ALUNO = "aluno";

    private boolean modoProfessor;

    private TextView tvNomePrimeiro, tvPontosPrimeiro;
    private TextView tvNomeSegundo, tvPontosSegundo;
    private TextView tvNomeTerceiro, tvPontosTerceiro;
    private ImageView imgPrimeiro, imgSegundo, imgTerceiro;

    private RecyclerView recyclerRanking;
    private TextView tvSemDados;

    private RankingAdapter adapter;

    // Lista exibida abaixo do pódio (professor: todos; aluno: só a própria posição)
    private final List<Aluno> listaAlunos = new ArrayList<>();

    // Os alunos da turma selecionada na ordem do ranking (aluno: só o pódio e ele)
    private final List<Aluno> rankingTurma = new ArrayList<>();

    private Spinner spinnerTurma;
    private final List<String> turmaIds = new ArrayList<>();
    private final List<String> turmaNomes = new ArrayList<>();

    private String turmaSelecionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ranking);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsTopo(this);

        modoProfessor = PERFIL_PROFESSOR.equals(
                getIntent().getStringExtra(EXTRA_PERFIL)
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

        bottomNavigation.setItemIconTintList(null);

        // O diário é só do aluno: o professor usa esta tela com a mesma barra, sem esse item
        if (modoProfessor) {
            bottomNavigation.getMenu().removeItem(R.id.nav_diario);
        }

        bottomNavigation.setSelectedItemId(R.id.nav_ranking);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                startActivity(new Intent(
                        this,
                        modoProfessor ? TelaInicialProfessor.class : TelaInicialAluno.class
                ));

                return true;

            } else if (id == R.id.nav_disciplinas) {

                startActivity(new Intent(
                        this,
                        modoProfessor ? TelaDisciplinaProfessor.class : TelaDisciplinaAluno.class
                ));

                return true;

            } else if (id == R.id.nav_diario) {

                startActivity(new Intent(this, DiarioAlunoActivity.class));
                return true;

            } else if (id == R.id.nav_ranking) {

                return true;

            } else if (id == R.id.nav_perfil) {

                startActivity(new Intent(
                        this,
                        modoProfessor ? TelaPerfilProfessor.class : TelaPerfilAluno.class
                ));

                return true;
            }

            return false;
        });

        tvNomePrimeiro = findViewById(R.id.tvNomePrimeiro);
        tvPontosPrimeiro = findViewById(R.id.tvPontosPrimeiro);
        tvNomeSegundo = findViewById(R.id.tvNomeSegundo);
        tvPontosSegundo = findViewById(R.id.tvPontosSegundo);
        tvNomeTerceiro = findViewById(R.id.tvNomeTerceiro);
        tvPontosTerceiro = findViewById(R.id.tvPontosTerceiro);

        imgPrimeiro = findViewById(R.id.imgPrimeiro);
        imgSegundo = findViewById(R.id.imgSegundo);
        imgTerceiro = findViewById(R.id.imgTerceiro);

        recyclerRanking = findViewById(R.id.recyclerRanking);
        tvSemDados = findViewById(R.id.tvSemDados);

        recyclerRanking.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RankingAdapter(listaAlunos);
        recyclerRanking.setAdapter(adapter);

        spinnerTurma = findViewById(R.id.spinnerTurmaRanking);

        carregarTurmas();
    }

    // =========================
    // TURMAS
    // =========================

    private void carregarTurmas() {

        Tarefas.executar(this, () -> Servicos.get(this).ranking.turmasDoRanking(),
                new Tarefas.Retorno<List<Turma>>() {
                    @Override
                    public void sucesso(List<Turma> turmas) {

                        turmaIds.clear();
                        turmaNomes.clear();

                        // O servidor já devolve só as turmas que o perfil pode ver, por nome
                        for (Turma turma : turmas) {
                            turmaIds.add(turma.id);
                            turmaNomes.add(turma.nome);
                        }

                        ArrayAdapter<String> adapterTurmas = new ArrayAdapter<>(
                                RankingActivity.this,
                                R.layout.item_spinner,
                                turmaNomes
                        );
                        adapterTurmas.setDropDownViewResource(
                                R.layout.item_spinner_dropdown
                        );
                        spinnerTurma.setAdapter(adapterTurmas);

                        if (turmaIds.isEmpty()) {
                            mostrarLista();
                            return;
                        }

                        if (modoProfessor) {

                            // Professor escolhe a turma; o ranking recarrega a cada troca
                            spinnerTurma.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

                                @Override
                                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                                    carregarRanking(turmaIds.get(position));
                                }

                                @Override
                                public void onNothingSelected(AdapterView<?> parent) {
                                }
                            });

                        } else {

                            // O aluno só vê o ranking da própria turma
                            spinnerTurma.setEnabled(false);
                            carregarRanking(turmaIds.get(0));
                        }
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        mostrarLista();
                    }
                });
    }

    // =========================
    // RANKING DA TURMA
    // =========================

    private void carregarRanking(String turmaId) {

        turmaSelecionada = turmaId;

        Tarefas.executar(this, () -> Servicos.get(this).ranking.ranking(turmaId),
                new Tarefas.Retorno<List<LinhaRanking>>() {
                    @Override
                    public void sucesso(List<LinhaRanking> linhas) {

                        // Se o professor já trocou de turma, ignora o resultado antigo
                        if (!turmaId.equals(turmaSelecionada)) {
                            return;
                        }

                        exibirRanking(linhas);
                    }

                    @Override
                    public void erro(ErroApi erro) {

                        if (!turmaId.equals(turmaSelecionada)) {
                            return;
                        }

                        Toast.makeText(RankingActivity.this,
                                "Erro ao carregar o ranking.", Toast.LENGTH_SHORT).show();

                        exibirRanking(new ArrayList<>());
                    }
                });
    }

    private void exibirRanking(List<LinhaRanking> linhas) {

        rankingTurma.clear();

        for (LinhaRanking linha : linhas) {
            rankingTurma.add(new Aluno(linha));
        }

        preencherPodio();

        listaAlunos.clear();

        if (modoProfessor) {

            // Professor vê todos os alunos da turma
            listaAlunos.addAll(rankingTurma);

        } else {

            // Aluno vê os 3 primeiros e, na 4ª linha, a própria posição como "Você"
            // (mesmo que ele já esteja entre os 3 primeiros)
            for (Aluno aluno : rankingTurma) {

                if (aluno.posicao <= 3) {
                    listaAlunos.add(aluno);
                }
            }

            for (Aluno aluno : rankingTurma) {

                if (aluno.ehVoce) {
                    listaAlunos.add(aluno.comoVoce());
                }
            }
        }

        mostrarLista();
    }

    private void mostrarLista() {

        adapter.notifyDataSetChanged();

        tvSemDados.setVisibility(
                rankingTurma.isEmpty() ? View.VISIBLE : View.GONE
        );

        recyclerRanking.setVisibility(
                rankingTurma.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    private void preencherPodio() {

        preencherLugar(1, tvNomePrimeiro, tvPontosPrimeiro, imgPrimeiro);
        preencherLugar(2, tvNomeSegundo, tvPontosSegundo, imgSegundo);
        preencherLugar(3, tvNomeTerceiro, tvPontosTerceiro, imgTerceiro);
    }

    // Limpa o lugar do pódio quando a turma tem menos de 3 alunos
    private void preencherLugar(int posicao, TextView tvNome, TextView tvPontos, ImageView img) {

        img.setImageResource(R.drawable.ic_perfil);

        for (Aluno aluno : rankingTurma) {

            if (aluno.posicao == posicao) {

                tvNome.setText(aluno.nome);
                tvPontos.setText(aluno.pontuacao + " pts");
                PerfilFotoUtil.mostrar(this, aluno.fotoPath, aluno.fotoGoogleUrl, img);
                return;
            }
        }

        tvNome.setText("—");
        tvPontos.setText("0 pts");
    }

    public static class Aluno {

        final String id;
        final String nome;
        final int pontuacao;
        final String fotoPath;
        final String fotoGoogleUrl;
        final int posicao;
        final boolean ehVoce;

        Aluno(LinhaRanking linha) {
            this(linha.perfilId, linha.nome, linha.pontos, linha.fotoPath, linha.fotoGoogleUrl,
                    linha.posicao, linha.ehVoce);
        }

        private Aluno(String id, String nome, int pontuacao, String fotoPath, String fotoGoogleUrl,
                      int posicao, boolean ehVoce) {
            this.id = id;
            this.nome = nome;
            this.pontuacao = pontuacao;
            this.fotoPath = fotoPath;
            this.fotoGoogleUrl = fotoGoogleUrl;
            this.posicao = posicao;
            this.ehVoce = ehVoce;
        }

        Aluno comoVoce() {
            return new Aluno(id, "Você", pontuacao, fotoPath, fotoGoogleUrl, posicao, true);
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

            holder.tvPosicao.setText(String.valueOf(aluno.posicao));
            holder.tvNomeAluno.setText(aluno.nome);
            holder.tvPontosAluno.setText(aluno.pontuacao + " pts");
            holder.imgAvatar.setImageResource(R.drawable.ic_perfil);
            PerfilFotoUtil.mostrar(RankingActivity.this, aluno.fotoPath, aluno.fotoGoogleUrl, holder.imgAvatar);
        }

        @Override
        public int getItemCount() {
            return alunos.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvPosicao;
            TextView tvNomeAluno;
            TextView tvPontosAluno;
            ImageView imgAvatar;

            public ViewHolder(View itemView) {
                super(itemView);

                tvPosicao = itemView.findViewById(R.id.tvPosicao);
                tvNomeAluno = itemView.findViewById(R.id.tvNomeAluno);
                tvPontosAluno = itemView.findViewById(R.id.tvPontosAluno);
                imgAvatar = itemView.findViewById(R.id.imgAvatar);
            }
        }
    }
}
