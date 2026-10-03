package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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

            // Foto escolhida pelo aluno (ou a da conta Google); tocar nela troca a foto
            PerfilFotoUtil.carregarFotoDoUsuario(this, imgFotoPerfilAluno, false);

            // Galeria -> tela de recorte quadrado -> envio da foto recortada
            ActivityResultLauncher<Intent> recortarFoto = registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    resultado -> {
                        if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                            PerfilFotoUtil.trocarFoto(this, resultado.getData().getData(), false, imgFotoPerfilAluno);
                        }
                    }
            );

            ActivityResultLauncher<String> escolherFoto = registerForActivityResult(
                    new ActivityResultContracts.GetContent(),
                    uri -> {
                        if (uri != null) {
                            recortarFoto.launch(RecortarFotoActivity.criarIntent(this, uri));
                        }
                    }
            );

            imgFotoPerfilAluno.setOnClickListener(v -> escolherFoto.launch("image/*"));

            alunoId = usuario.getUid();

            // Pontuação recalculada com as notas e faltas antes de exibir
            PontuacaoUtil.recalcular(db, alunoId, () -> db.collection("alunos")
                    .document(alunoId)
                    .get()
                    .addOnSuccessListener(alunoDoc -> {

                        if (!alunoDoc.exists()) {
                            return;
                        }

                        String email = alunoDoc.getString("email");
                        String turma = alunoDoc.getString("turmaNome");
                        Long pontuacao = alunoDoc.getLong("pontuacao");

                        tvEmailPerfilAluno.setText(email != null ? email : usuario.getEmail());
                        tvTurmaPerfilAluno.setText(turma != null ? turma : "");
                        tvTurmaContaPerfilAluno.setText(turma != null ? turma : "—");

                        ((TextView) findViewById(R.id.tvPontuacaoTotalAluno)).setText(
                                String.valueOf(pontuacao != null ? pontuacao : 0)
                        );

                        carregarPosicaoRanking();
                        carregarEstatisticas();
                    }));
        }

        // =========================
        // CONFIGURAÇÕES
        // =========================

        ImageView btnConfiguracoesAluno = findViewById(R.id.btnConfiguracoesAluno);

        btnConfiguracoesAluno.setOnClickListener(v ->
                startActivity(new Intent(this, ConfiguracoesActivity.class))
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

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

            } else if (id == R.id.nav_diario) {

                startActivity(new Intent(this, DiarioAlunoActivity.class));
                return true;

            } else if (id == R.id.nav_ranking) {

                startActivity(new Intent(this, RankingActivity.class)
                        .putExtra(RankingActivity.EXTRA_PERFIL, RankingActivity.PERFIL_ALUNO));
                return true;

            } else if (id == R.id.nav_perfil) {

                return true;
            }

            return false;
        });
    }

    private void carregarPosicaoRanking() {

        RankingActivity.carregarPosicaoNaTurma(
                db, alunoId, findViewById(R.id.tvPosicaoRankingAluno)
        );
    }

    private void carregarEstatisticas() {

        DisciplinaUtil.carregarDoAluno(db, alunoId, disciplinas ->
                ((TextView) findViewById(R.id.tvQtdDisciplinasPerfil))
                        .setText(String.valueOf(disciplinas.size()))
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

}
