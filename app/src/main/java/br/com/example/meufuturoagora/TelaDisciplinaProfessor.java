package br.com.example.meufuturoagora;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TelaDisciplinaProfessor extends AppCompatActivity {

    private FirebaseFirestore db;

    private RecyclerView recyclerDisciplinas;

    private DisciplinaAdapter adapter;

    private final List<Disciplina> listaDisciplinas =
            new ArrayList<>();

    private final List<Disciplina> listaDisciplinasOriginal =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        setContentView(
                R.layout.activity_tela_disciplina_professor);

        // =========================
        // FIRESTORE
        // =========================

        db = FirebaseFirestore.getInstance();

        // =========================
        // RECYCLERVIEW
        // =========================

        recyclerDisciplinas =
                findViewById(R.id.recyclerDisciplinas);

        recyclerDisciplinas.setLayoutManager(
                new LinearLayoutManager(this)
        );

        adapter = new DisciplinaAdapter(
                listaDisciplinas
        );

        recyclerDisciplinas.setAdapter(adapter);

        EditText edtBuscar =
                findViewById(R.id.edtBuscar);

        edtBuscar.addTextChangedListener(
                new TextWatcher() {

                    @Override
                    public void beforeTextChanged(
                            CharSequence s,
                            int start,
                            int count,
                            int after
                    ) {
                    }

                    @Override
                    public void onTextChanged(
                            CharSequence s,
                            int start,
                            int before,
                            int count
                    ) {
                        filtrarDisciplinas(s.toString());
                    }

                    @Override
                    public void afterTextChanged(
                            Editable s
                    ) {
                    }
                }
        );

        // =========================
        // CRIAR DISCIPLINA
        // =========================

        findViewById(R.id.btnCriarDisciplina).setOnClickListener(v ->
                startActivity(new Intent(this, CriarDisciplinaActivity.class))
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation =
                findViewById(R.id.bottomNavigation);

        bottomNavigation.setItemIconTintList(null);

        bottomNavigation.setSelectedItemId(
                R.id.nav_disciplinas
        );

        // =========================
        // NAVEGAÇÃO
        // =========================

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                Intent intent = new Intent(
                        TelaDisciplinaProfessor.this,
                        TelaInicialProfessor.class
                );

                startActivity(intent);

                return true;

            } else if (id == R.id.nav_disciplinas) {

                return true;

            } else if (id == R.id.nav_perfil) {

                Intent intent = new Intent(
                        TelaDisciplinaProfessor.this,
                        TelaPerfilProfessor.class
                );

                startActivity(intent);

                return true;
            }

            return false;
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        carregarDisciplinas();
    }

    // =====================================================
    // CARREGAR DISCIPLINAS DO PROFESSOR
    // =====================================================

    private void carregarDisciplinas() {

        FirebaseUser usuario =
                FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {

            android.util.Log.e(
                    "DISCIPLINAS",
                    "Nenhum usuário está logado."
            );

            return;
        }

        String uidProfessor = usuario.getUid();

        android.util.Log.d(
                "DISCIPLINAS",
                "UID LOGADO: " + uidProfessor
        );

        db.collection("disciplinas")
                .whereEqualTo(
                        "professorId",
                        uidProfessor
                )
                .whereEqualTo(
                        "ativo",
                        true
                )
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    android.util.Log.d(
                            "DISCIPLINAS",
                            "Quantidade encontrada: "
                                    + querySnapshot.size()
                    );

                    listaDisciplinas.clear();
                    listaDisciplinasOriginal.clear();

                    for (QueryDocumentSnapshot documento :
                            querySnapshot) {

                        String id =
                                documento.getId();

                        String nome =
                                documento.getString("nome");

                        android.util.Log.d(
                                "DISCIPLINAS",
                                "ID: " + id
                        );

                        android.util.Log.d(
                                "DISCIPLINAS",
                                "NOME: " + nome
                        );

                        if (nome != null) {

                            String cor = gerarCorDisciplina(id);

                            Disciplina disciplina =
                                    new Disciplina(
                                            id,
                                            nome,
                                            cor
                                    );

                            listaDisciplinas.add(disciplina);
                            listaDisciplinasOriginal.add(disciplina);
                        }
                    }

                    adapter.notifyDataSetChanged();

                })
                .addOnFailureListener(e -> {

                    android.util.Log.e(
                            "DISCIPLINAS",
                            "ERRO AO BUSCAR DISCIPLINAS",
                            e
                    );
                });
    }

    // =====================================================
    // MODELO DA DISCIPLINA
    // =====================================================

    private void filtrarDisciplinas(String texto) {

        String busca =
                texto.trim().toLowerCase();

        listaDisciplinas.clear();

        if (busca.isEmpty()) {

            listaDisciplinas.addAll(
                    listaDisciplinasOriginal
            );

        } else {

            for (Disciplina disciplina :
                    listaDisciplinasOriginal) {

                if (disciplina.nome
                        .toLowerCase()
                        .contains(busca)) {

                    listaDisciplinas.add(
                            disciplina
                    );
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    public static class Disciplina {

        String id;
        String nome;
        String cor;

        public Disciplina(
                String id,
                String nome,
                String cor
        ) {
            this.id = id;
            this.nome = nome;
            this.cor = cor;
        }
    }

    // =====================================================
    // ADAPTER
    // =====================================================

    private class DisciplinaAdapter
            extends RecyclerView.Adapter<DisciplinaAdapter.ViewHolder> {

        private final List<Disciplina> disciplinas;

        public DisciplinaAdapter(List<Disciplina> disciplinas) {
            this.disciplinas = disciplinas;
        }

        @Override
        public ViewHolder onCreateViewHolder(
                ViewGroup parent,
                int viewType
        ) {

            // IMPORTANTE:
            // Aqui estamos usando o seu item_disciplina.xml
            View view = getLayoutInflater().inflate(
                    R.layout.item_disciplina,
                    parent,
                    false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(
                ViewHolder holder,
                int position
        ) {

            Disciplina disciplina =
                    disciplinas.get(position);

            // Nome da disciplina
            holder.tvNomeDisciplina.setText(
                    disciplina.nome
            );

            // Por enquanto, quantidade de alunos
            holder.tvQuantidadeAlunos.setText(
                    "0 alunos"
            );

            int cor = Color.parseColor(disciplina.cor);

            GradientDrawable fundoCirculo = new GradientDrawable();
            fundoCirculo.setShape(GradientDrawable.OVAL);
            fundoCirculo.setColor(cor);

            holder.circuloCor.setBackground(fundoCirculo);

            // Clique no item inteiro
            holder.itemView.setOnClickListener(v -> {

                Intent intent = new Intent(
                        TelaDisciplinaProfessor.this,
                        TelaDetalhesDisciplinaProfessor.class
                );

                intent.putExtra(
                        "disciplinaId",
                        disciplina.id
                );

                intent.putExtra(
                        "disciplinaCor",
                        disciplina.cor
                );

                startActivity(intent);
            });

            // Botão de três pontinhos
            holder.btnMenuDisciplina.setOnClickListener(v -> {

                Toast.makeText(
                        TelaDisciplinaProfessor.this,
                        "Opções de " + disciplina.nome,
                        Toast.LENGTH_SHORT
                ).show();
            });
        }

        @Override
        public int getItemCount() {
            return disciplinas.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            View circuloCor;

            TextView tvNomeDisciplina;
            TextView tvQuantidadeAlunos;

            ImageView btnMenuDisciplina;

            public ViewHolder(View itemView) {

                super(itemView);

                circuloCor =
                        itemView.findViewById(
                                R.id.circuloCor
                        );

                tvNomeDisciplina =
                        itemView.findViewById(
                                R.id.tvNomeDisciplina
                        );

                tvQuantidadeAlunos =
                        itemView.findViewById(
                                R.id.tvQuantidadeAlunos
                        );

                btnMenuDisciplina =
                        itemView.findViewById(
                                R.id.btnMenuDisciplina
                        );
            }
        }
    }

    // =====================================================
    // COR ALEATÓRIA
    // =====================================================

    private String gerarCorDisciplina(String id) {

        String[] cores = {
                "#B0181D",
                "#D1CC00",
                "#22BEEA",
                "#8E44AD",
                "#27AE60",
                "#E67E22",
                "#E84393",
                "#34495E",
                "#16A085",
                "#F39C12"
        };

        int indice =
                Math.abs(id.hashCode()) % cores.length;

        return cores[indice];
    }
}