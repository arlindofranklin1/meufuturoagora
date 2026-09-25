package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

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

            // =========================
            // FOTO DO PROFESSOR
            // =========================

            Uri foto = usuario.getPhotoUrl();

            if (foto != null) {

                Glide.with(this)
                        .load(foto)
                        .placeholder(R.drawable.ic_perfil)
                        .error(R.drawable.ic_perfil)
                        .override(200, 200)
                        .circleCrop()
                        .into(imgProfessor);

            } else {

                imgProfessor.setImageResource(
                        R.drawable.ic_perfil
                );
            }

            // =========================
            // CARREGAR DISCIPLINAS
            // =========================

            carregarQuantidadeDisciplinas(uidProfessor);
        }

        // =========================
        // BIMESTRE ATUAL
        // =========================

        BimestreUtil.carregarPeriodoAtual(db, findViewById(R.id.tvPeriodoBimestreAluno));

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        bottomNavigation = findViewById(
                R.id.bottomNavigation
        );

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

        CardView btnCriarTarefa = findViewById(R.id.btnCriarTarefa);

        btnCriarTarefa.setOnClickListener(v -> startActivity(
                new Intent(TelaInicialProfessor.this, CriarAtividade.class)
        ));

        CardView btnVerRanking = findViewById(R.id.btnVerRanking);

        btnVerRanking.setOnClickListener(v -> startActivity(
                new Intent(TelaInicialProfessor.this, RankingActivity.class)
        ));
    }


    // =====================================================
    // QUANTIDADE DE DISCIPLINAS DO PROFESSOR
    // =====================================================

    private void carregarQuantidadeDisciplinas(String uidProfessor) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("disciplinas")
                .whereEqualTo("professorId", uidProfessor)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int quantidade = querySnapshot.size();

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