package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

public class TelaPerfilAluno extends AppCompatActivity {

    private FirebaseFirestore db;
    private String alunoId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_perfil_aluno);

        db = FirebaseFirestore.getInstance();

        ImageView imgFotoPerfilAluno = findViewById(R.id.imgFotoPerfilAluno);
        TextView tvNomePerfilAluno = findViewById(R.id.tvNomePerfilAluno);
        TextView tvTurmaPerfilAluno = findViewById(R.id.tvTurmaPerfilAluno);
        TextView tvEmailPerfilAluno = findViewById(R.id.tvEmailPerfilAluno);
        TextView tvTurmaContaPerfilAluno = findViewById(R.id.tvTurmaContaPerfilAluno);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario != null) {

            String nome = usuario.getDisplayName();
            tvNomePerfilAluno.setText(nome != null && !nome.isEmpty() ? nome : "Aluno");

            Uri foto = usuario.getPhotoUrl();

            if (foto != null) {

                Glide.with(this)
                        .load(foto)
                        .placeholder(R.drawable.ic_perfil)
                        .error(R.drawable.ic_perfil)
                        .circleCrop()
                        .into(imgFotoPerfilAluno);
            }

            alunoId = usuario.getUid();

            db.collection("alunos")
                    .document(alunoId)
                    .get()
                    .addOnSuccessListener(alunoDoc -> {

                        if (!alunoDoc.exists()) {
                            return;
                        }

                        String email = alunoDoc.getString("email");
                        String turma = alunoDoc.getString("turma");
                        Long pontuacao = alunoDoc.getLong("pontuacao");

                        tvEmailPerfilAluno.setText(email != null ? email : usuario.getEmail());
                        tvTurmaPerfilAluno.setText(turma != null ? turma : "");
                        tvTurmaContaPerfilAluno.setText(turma != null ? turma : "—");

                        ((TextView) findViewById(R.id.tvPontuacaoTotalPerfil)).setText(
                                String.valueOf(pontuacao != null ? pontuacao : 0)
                        );

                        carregarPosicaoRanking();
                        carregarEstatisticas();
                    });
        }

        // =========================
        // CONFIGURAÇÕES
        // =========================

        LinearLayout layoutOutrosAluno = findViewById(R.id.layoutOutrosAluno);

        ImageView btnConfiguracoesAluno = findViewById(R.id.btnConfiguracoesAluno);

        btnConfiguracoesAluno.setOnClickListener(v -> layoutOutrosAluno.setVisibility(
                layoutOutrosAluno.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE
        ));

        findViewById(R.id.itemSobreAluno).setOnClickListener(v ->
                startActivity(new Intent(this, SobreActivity.class))
        );

        findViewById(R.id.itemSairAluno).setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Sair da conta")
                        .setMessage("Tem certeza que deseja sair?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Sair", (dialog, which) -> sairDaConta())
                        .show()
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        bottomNavigation.setItemIconTintList(null);
        bottomNavigation.setSelectedItemId(R.id.nav_perfil);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                startActivity(new Intent(this, TelaInicialAluno.class));
                return true;

            } else if (id == R.id.nav_disciplinas) {

                startActivity(new Intent(this, TelaDisciplinaAluno.class));
                return true;

            } else if (id == R.id.nav_ranking) {

                startActivity(new Intent(this, RankingActivity.class));
                return true;

            } else if (id == R.id.nav_perfil) {

                return true;
            }

            return false;
        });
    }

    private void carregarPosicaoRanking() {

        db.collection("alunos")
                .orderBy("pontuacao", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int posicao = 1;

                    for (QueryDocumentSnapshot documento : querySnapshot) {

                        if (documento.getId().equals(alunoId)) {

                            ((TextView) findViewById(R.id.tvPosicaoRankingPerfil))
                                    .setText(posicao + "º");
                            return;
                        }

                        posicao++;
                    }
                });
    }

    private void carregarEstatisticas() {

        db.collection("matriculas")
                .whereEqualTo("alunoId", alunoId)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        ((TextView) findViewById(R.id.tvQtdDisciplinasPerfil))
                                .setText(String.valueOf(querySnapshot.size()))
                );

        db.collection("entregas")
                .whereEqualTo("alunoId", alunoId)
                .whereEqualTo("avaliado", true)
                .get()
                .addOnSuccessListener(querySnapshot ->
                        ((TextView) findViewById(R.id.tvQtdConcluidasPerfil))
                                .setText(String.valueOf(querySnapshot.size()))
                );

        db.collection("frequencias")
                .whereEqualTo("alunoId", alunoId)
                .whereEqualTo("status", "Falta")
                .get()
                .addOnSuccessListener(querySnapshot ->
                        ((TextView) findViewById(R.id.tvQtdFaltasPerfil))
                                .setText(String.valueOf(querySnapshot.size()))
                );
    }

    private void sairDaConta() {

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, TelaEntrar.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
