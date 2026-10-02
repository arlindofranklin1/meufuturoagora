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

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RankingActivity extends AppCompatActivity {

    public static final String EXTRA_PERFIL = "perfil";
    public static final String PERFIL_PROFESSOR = "professor";
    public static final String PERFIL_ALUNO = "aluno";

    private boolean modoProfessor;

    private FirebaseFirestore db;

    private TextView tvNomePrimeiro, tvPontosPrimeiro;
    private TextView tvNomeSegundo, tvPontosSegundo;
    private TextView tvNomeTerceiro, tvPontosTerceiro;
    private ImageView imgPrimeiro, imgSegundo, imgTerceiro;

    private RecyclerView recyclerRanking;
    private TextView tvSemDados;

    private RankingAdapter adapter;

    // Lista exibida abaixo do pódio (professor: todos; aluno: só a própria posição)
    private final List<Aluno> listaAlunos = new ArrayList<>();

    // Todos os alunos da turma selecionada, ordenados por pontuação
    private final List<Aluno> rankingTurma = new ArrayList<>();

    private Spinner spinnerTurma;
    private final List<String> turmaIds = new ArrayList<>();
    private final List<String> turmaNomes = new ArrayList<>();

    private String alunoLogadoId;

    private String turmaSelecionada;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_ranking);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsTopo(this);

        db = FirebaseFirestore.getInstance();

        modoProfessor = PERFIL_PROFESSOR.equals(
                getIntent().getStringExtra(EXTRA_PERFIL)
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

        bottomNavigation.setItemIconTintList(null);

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

        db.collection("turmas")
                .whereEqualTo("ativo", true)
                .get()
                .addOnSuccessListener(turmas -> {

                    turmaIds.clear();
                    turmaNomes.clear();

                    List<String[]> lista = new ArrayList<>();

                    for (QueryDocumentSnapshot documento : turmas) {

                        String nome = documento.getString("nome");

                        if (nome != null) {
                            lista.add(new String[]{documento.getId(), nome});
                        }
                    }

                    Collections.sort(lista, (a, b) -> a[1].compareToIgnoreCase(b[1]));

                    for (String[] turma : lista) {
                        turmaIds.add(turma[0]);
                        turmaNomes.add(turma[1]);
                    }

                    ArrayAdapter<String> adapterTurmas = new ArrayAdapter<>(
                            this,
                            R.layout.item_spinner,
                            turmaNomes
                    );
                    adapterTurmas.setDropDownViewResource(
                            R.layout.item_spinner_dropdown
                    );
                    spinnerTurma.setAdapter(adapterTurmas);

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

                        if (turmaIds.isEmpty()) {
                            mostrarLista();
                        }

                    } else {

                        selecionarTurmaDoAluno();
                    }
                })
                .addOnFailureListener(e -> mostrarLista());
    }

    // O aluno só vê o ranking da própria turma
    private void selecionarTurmaDoAluno() {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return;
        }

        alunoLogadoId = usuario.getUid();

        db.collection("alunos")
                .document(alunoLogadoId)
                .get()
                .addOnSuccessListener(documento -> {

                    int indice = turmaIds.indexOf(documento.getString("turmaId"));

                    if (indice >= 0) {

                        spinnerTurma.setSelection(indice);
                        carregarRanking(turmaIds.get(indice));
                    }

                    spinnerTurma.setEnabled(false);
                });
    }

    // =========================
    // RANKING DA TURMA
    // =========================

    // Recalcula a pontuação de cada aluno da turma (notas e faltas) e depois monta o ranking
    private void carregarRanking(String turmaId) {

        turmaSelecionada = turmaId;

        db.collection("alunos")
                .whereEqualTo("turmaId", turmaId)
                .get()
                .addOnSuccessListener(alunos -> {

                    List<String> ids = new ArrayList<>();

                    for (QueryDocumentSnapshot documento : alunos) {

                        if (documento.getLong("pontuacao") != null) {
                            ids.add(documento.getId());
                        }
                    }

                    if (ids.isEmpty()) {
                        exibirRanking(turmaId);
                        return;
                    }

                    int[] restantes = {ids.size()};

                    for (String id : ids) {

                        PontuacaoUtil.recalcular(db, id, () -> {

                            restantes[0]--;

                            if (restantes[0] == 0) {
                                exibirRanking(turmaId);
                            }
                        });
                    }
                })
                .addOnFailureListener(e -> exibirRanking(turmaId));
    }

    private void exibirRanking(String turmaId) {

        // Se o professor já trocou de turma, ignora o resultado antigo
        if (!turmaId.equals(turmaSelecionada)) {
            return;
        }

        db.collection("alunos")
                .whereEqualTo("turmaId", turmaId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    rankingTurma.clear();

                    for (QueryDocumentSnapshot documento : querySnapshot) {

                        String nome = documento.getString("nome");
                        Long pontuacao = documento.getLong("pontuacao");

                        // Só contas de alunos que já entraram no app (as autorizações
                        // cadastradas pela coordenação não têm pontuação)
                        if (nome == null || pontuacao == null
                                || Boolean.FALSE.equals(documento.getBoolean("ativo"))) {
                            continue;
                        }

                        rankingTurma.add(new Aluno(
                                documento.getId(),
                                nome,
                                pontuacao,
                                documento.getString("fotoUrl")
                        ));
                    }

                    Collections.sort(rankingTurma, (a, b) -> {

                        int porPontos = Long.compare(b.pontuacao, a.pontuacao);
                        return porPontos != 0 ? porPontos : a.nome.compareToIgnoreCase(b.nome);
                    });

                    for (int i = 0; i < rankingTurma.size(); i++) {
                        rankingTurma.get(i).posicao = i + 1;
                    }

                    preencherPodio();

                    listaAlunos.clear();

                    if (modoProfessor) {

                        // Professor vê todos os alunos da turma
                        listaAlunos.addAll(rankingTurma);

                    } else {

                        // Aluno vê os 3 primeiros e, na 4ª linha, a própria posição como "Você"
                        // (mesmo que ele já esteja entre os 3 primeiros)
                        for (int i = 0; i < Math.min(3, rankingTurma.size()); i++) {
                            listaAlunos.add(rankingTurma.get(i));
                        }

                        for (Aluno aluno : rankingTurma) {

                            if (aluno.id.equals(alunoLogadoId)) {

                                Aluno voce = new Aluno(aluno.id, "Você", aluno.pontuacao, aluno.fotoUrl);
                                voce.posicao = aluno.posicao;
                                listaAlunos.add(voce);
                            }
                        }
                    }

                    mostrarLista();
                })
                .addOnFailureListener(e -> {

                    rankingTurma.clear();
                    listaAlunos.clear();
                    preencherPodio();
                    mostrarLista();
                });
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

        preencherLugar(0, tvNomePrimeiro, tvPontosPrimeiro, imgPrimeiro);
        preencherLugar(1, tvNomeSegundo, tvPontosSegundo, imgSegundo);
        preencherLugar(2, tvNomeTerceiro, tvPontosTerceiro, imgTerceiro);
    }

    // Limpa o lugar do pódio quando a turma tem menos de 3 alunos
    private void preencherLugar(int indice, TextView tvNome, TextView tvPontos, ImageView img) {

        img.setImageResource(R.drawable.ic_perfil);

        if (rankingTurma.size() > indice) {

            Aluno aluno = rankingTurma.get(indice);

            tvNome.setText(aluno.nome);
            tvPontos.setText(aluno.pontuacao + " pts");
            FotoUtil.carregar(this, aluno.fotoUrl, img);

        } else {

            tvNome.setText("—");
            tvPontos.setText("0 pts");
        }
    }

    // Posição do aluno no ranking da própria turma (usado na tela inicial e no perfil)
    static void carregarPosicaoNaTurma(FirebaseFirestore db, String alunoId, TextView destino) {

        db.collection("alunos")
                .document(alunoId)
                .get()
                .addOnSuccessListener(alunoDoc -> {

                    String turmaId = alunoDoc.getString("turmaId");
                    Long minhaPontuacao = alunoDoc.getLong("pontuacao");
                    String meuNome = alunoDoc.getString("nome");

                    if (turmaId == null || minhaPontuacao == null) {
                        return;
                    }

                    db.collection("alunos")
                            .whereEqualTo("turmaId", turmaId)
                            .get()
                            .addOnSuccessListener(turma -> {

                                int posicao = 1;

                                for (QueryDocumentSnapshot documento : turma) {

                                    Long pontuacao = documento.getLong("pontuacao");
                                    String nome = documento.getString("nome");

                                    if (documento.getId().equals(alunoId) || nome == null
                                            || pontuacao == null
                                            || Boolean.FALSE.equals(documento.getBoolean("ativo"))) {
                                        continue;
                                    }

                                    // Mesmo critério da tela de ranking: pontos e, no empate, nome
                                    boolean naFrente = pontuacao > minhaPontuacao
                                            || (pontuacao.equals(minhaPontuacao) && meuNome != null
                                            && nome.compareToIgnoreCase(meuNome) < 0);

                                    if (naFrente) {
                                        posicao++;
                                    }
                                }

                                destino.setText(posicao + "º");
                            });
                });
    }

    public static class Aluno {

        String id;
        String nome;
        long pontuacao;
        String fotoUrl;
        int posicao;

        public Aluno(String id, String nome, long pontuacao, String fotoUrl) {
            this.id = id;
            this.nome = nome;
            this.pontuacao = pontuacao;
            this.fotoUrl = fotoUrl;
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
            FotoUtil.carregar(RankingActivity.this, aluno.fotoUrl, holder.imgAvatar);
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
