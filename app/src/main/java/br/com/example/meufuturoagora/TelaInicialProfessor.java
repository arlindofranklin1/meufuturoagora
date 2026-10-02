package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import com.google.firebase.firestore.DocumentSnapshot;

public class TelaInicialProfessor extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private ImageView imgProfessor;

    private TextView tvNomeProfessor;
    private TextView tvNumeroDisciplinas;
    private CardView cardDisciplinas;
    private CardView btnCriarTarefa;

    // -1 enquanto ainda não carregou
    private int quantidadeDisciplinas = -1;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_inicial_professor);

        // =========================
        // FIRESTORE
        // =========================

        db = FirebaseFirestore.getInstance();

        // =========================
        // COMPONENTES
        // =========================

        imgProfessor = findViewById(R.id.imgProfessor);

        tvNomeProfessor = findViewById(R.id.tvNomeProfessor);

        tvNumeroDisciplinas = findViewById(
                R.id.tvNumeroDisciplinas
        );


        // =========================
        // USUÁRIO LOGADO
        // =========================

        FirebaseUser usuario = FirebaseAuth
                .getInstance()
                .getCurrentUser();

        if (usuario != null) {

            String uidProfessor = usuario.getUid();

            android.util.Log.d(
                    "PROFESSOR_UID",
                    "UID LOGADO: " + uidProfessor
            );

            // restante...
        }

        if (usuario != null) {

            String uidProfessor = usuario.getUid();

            // =========================
            // NOME DO PROFESSOR
            // =========================

            String nome = usuario.getDisplayName();

            if (nome != null && !nome.isEmpty()) {

                tvNomeProfessor.setText(nome);

            } else {

                // Caso o Google não forneça o nome
                tvNomeProfessor.setText("Professor");
            }

            // Tocar na foto abre o perfil
            imgProfessor.setOnClickListener(v ->
                    startActivity(new Intent(this, TelaPerfilProfessor.class))
            );
        }

        AnoLetivoUtil.carregar(db);

        // =========================
        // BIMESTRE ATUAL
        // =========================

        BimestreUtil.carregarPeriodoAtual(
                db,
                findViewById(R.id.tvTituloBimestre),
                findViewById(R.id.tvPeriodoBimestre)
        );

        // Notificações de novidades (respeita o switch das configurações)
        NotificacaoUtil.iniciar(this, NotificacaoUtil.PERFIL_PROFESSOR);

        // Sino: abre a lista de notificações; bolinha laranja = tem notificação não vista
        View bolinhaNotificacao = findViewById(R.id.bolinhaNotificacao);
        NotificacaoHistorico.vincularBolinha(this, bolinhaNotificacao);
        findViewById(R.id.btnNotificacoes).setOnClickListener(v ->
                NotificacoesDialog.mostrar(this, bolinhaNotificacao)
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        bottomNavigation = findViewById(
                R.id.bottomNavigation
        );

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

        bottomNavigation.setItemIconTintList(null);

        bottomNavigation.setSelectedItemId(
                R.id.nav_inicio
        );

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                return true;

            } else if (id == R.id.nav_disciplinas) {

                Intent intent = new Intent(
                        TelaInicialProfessor.this,
                        TelaDisciplinaProfessor.class
                );

                startActivity(intent);

                return true;

            } else if (id == R.id.nav_ranking) {

                Intent intent = new Intent(
                        TelaInicialProfessor.this,
                        RankingActivity.class
                );

                intent.putExtra(
                        RankingActivity.EXTRA_PERFIL,
                        RankingActivity.PERFIL_PROFESSOR
                );

                startActivity(intent);

                return true;

            } else if (id == R.id.nav_perfil) {

                Intent intent = new Intent(
                        TelaInicialProfessor.this,
                        TelaPerfilProfessor.class
                );

                startActivity(intent);

                return true;
            }

            return false;
        });

        cardDisciplinas = findViewById(R.id.cardDisciplinas);

        cardDisciplinas.setOnClickListener(v -> {

            Intent intent = new Intent(
                    TelaInicialProfessor.this,
                    TelaDisciplinaProfessor.class
            );

            startActivity(intent);
        });

        // =========================
        // AÇÕES RÁPIDAS
        // =========================

        btnCriarTarefa = findViewById(R.id.btnCriarTarefa);

        // Toda trilha pertence a uma disciplina: sem disciplina criada, só avisa
        btnCriarTarefa.setOnClickListener(v -> {

            if (quantidadeDisciplinas == 0) {

                Toast.makeText(
                        this,
                        "Crie uma disciplina primeiro para depois criar uma trilha.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            startActivity(new Intent(TelaInicialProfessor.this, CriarAtividade.class));
        });

        CardView btnVerRanking = findViewById(R.id.btnVerRanking);

        btnVerRanking.setOnClickListener(v -> startActivity(
                new Intent(TelaInicialProfessor.this, RankingActivity.class)
                        .putExtra(
                                RankingActivity.EXTRA_PERFIL,
                                RankingActivity.PERFIL_PROFESSOR
                        )
        ));

        CardView btnDesempenho = findViewById(R.id.btnDesempenho);

        btnDesempenho.setOnClickListener(v -> startActivity(
                new Intent(TelaInicialProfessor.this, DesempenhoActivity.class)
                        .putExtra(
                                DesempenhoActivity.EXTRA_PERFIL,
                                DesempenhoActivity.PERFIL_PROFESSOR
                        )
        ));
    }


    @Override
    protected void onResume() {
        super.onResume();

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return;
        }

        // Foto (a escolhida no perfil ou a do Google) e disciplinas, atualizadas ao voltar
        PerfilFotoUtil.carregarFotoDoUsuario(this, imgProfessor, true);
        carregarQuantidadeDisciplinas(usuario.getUid());
    }

    // Sem disciplina, o botão fica apagado (o toque só mostra o aviso)
    private void btnCriarTarefaHabilitado(boolean habilitado) {
        btnCriarTarefa.setAlpha(habilitado ? 1f : 0.5f);
    }

    // =====================================================
    // QUANTIDADE DE DISCIPLINAS DO PROFESSOR
    // =====================================================

    private void carregarQuantidadeDisciplinas(String uidProfessor) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("disciplinas")
                .whereEqualTo("professorId", uidProfessor)
                .whereEqualTo("ativo", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int quantidade = querySnapshot.size();

                    quantidadeDisciplinas = quantidade;
                    btnCriarTarefaHabilitado(quantidade > 0);

                    tvNumeroDisciplinas.setText(String.valueOf(quantidade));

                    android.util.Log.d(
                            "FIRESTORE",
                            "Disciplinas encontradas: " + quantidade
                    );

                    for (DocumentSnapshot documento : querySnapshot.getDocuments()) {
                        android.util.Log.d(
                                "FIRESTORE",
                                "Disciplina: " + documento.getId()
                        );
                        android.util.Log.d(
                                "FIRESTORE",
                                "Dados: " + documento.getData()
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    android.util.Log.e(
                            "FIRESTORE",
                            "ERRO AO BUSCAR DISCIPLINAS",
                            e
                    );

                    tvNumeroDisciplinas.setText("ERRO");
                });
    }
}
