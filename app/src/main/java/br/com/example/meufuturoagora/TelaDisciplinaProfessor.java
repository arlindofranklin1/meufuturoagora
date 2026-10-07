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
import android.widget.FrameLayout;
import android.widget.ImageView;
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
import android.text.Editable;
import android.text.TextWatcher;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

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

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsTopo(this);

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

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

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

            } else if (id == R.id.nav_ranking) {

                Intent intent = new Intent(
                        TelaDisciplinaProfessor.this,
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

                            disciplina.turmaNome =
                                    documento.getString("turmaNome");

                            listaDisciplinas.add(disciplina);
                            listaDisciplinasOriginal.add(disciplina);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    carregarQuantidadeAlunos();
                })
                .addOnFailureListener(e -> {

                    android.util.Log.e(
                            "DISCIPLINAS",
                            "ERRO AO BUSCAR DISCIPLINAS",
                            e
                    );
                });
    }

    // Conta os alunos matriculados em cada disciplina. Só entram alunos que
    // ainda existem na coleção "alunos" (aluno excluído não é contado).
    private void carregarQuantidadeAlunos() {

        List<Task<QuerySnapshot>> consultas = new ArrayList<>();

        for (int i = 0; i < listaDisciplinasOriginal.size(); i += 10) {

            List<String> lote = new ArrayList<>();

            for (Disciplina disciplina : listaDisciplinasOriginal.subList(
                    i, Math.min(i + 10, listaDisciplinasOriginal.size()))) {
                lote.add(disciplina.id);
            }

            consultas.add(db.collection("matriculas").whereIn("disciplinaId", lote).get());
        }

        Tasks.whenAllSuccess(consultas).addOnSuccessListener(this, resultados -> {

            Map<String, Set<String>> alunosPorDisciplina = new HashMap<>();
            Set<String> idsAlunos = new HashSet<>();

            for (Object resultado : resultados) {
                for (DocumentSnapshot matricula : ((QuerySnapshot) resultado).getDocuments()) {

                    String disciplinaId = matricula.getString("disciplinaId");
                    String alunoId = matricula.getString("alunoId");

                    if (disciplinaId != null && alunoId != null) {
                        alunosPorDisciplina
                                .computeIfAbsent(disciplinaId, k -> new HashSet<>())
                                .add(alunoId);
                        idsAlunos.add(alunoId);
                    }
                }
            }

            List<String> listaIds = new ArrayList<>(idsAlunos);
            List<Task<QuerySnapshot>> consultasAlunos = new ArrayList<>();

            for (int i = 0; i < listaIds.size(); i += 10) {
                consultasAlunos.add(db.collection("alunos")
                        .whereIn(FieldPath.documentId(),
                                new ArrayList<>(listaIds.subList(i, Math.min(i + 10, listaIds.size()))))
                        .get());
            }

            Tasks.whenAllSuccess(consultasAlunos).addOnSuccessListener(this, alunos -> {

                Set<String> existentes = new HashSet<>();

                for (Object resultado : alunos) {
                    for (DocumentSnapshot aluno : ((QuerySnapshot) resultado).getDocuments()) {
                        existentes.add(aluno.getId());
                    }
                }

                for (Disciplina disciplina : listaDisciplinasOriginal) {

                    int quantidade = 0;
                    Set<String> matriculados = alunosPorDisciplina.get(disciplina.id);

                    if (matriculados != null) {
                        for (String alunoId : matriculados) {
                            if (existentes.contains(alunoId)) {
                                quantidade++;
                            }
                        }
                    }

                    disciplina.qtdAlunos = quantidade;
                }

                adapter.notifyDataSetChanged();
            });
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
        String turmaNome;
        int qtdAlunos = -1; // -1 = ainda carregando

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

            // Nome da disciplina com a turma entre parênteses
            holder.tvNomeDisciplina.setText(
                    disciplina.turmaNome != null && !disciplina.turmaNome.isEmpty()
                            ? disciplina.nome + " (" + disciplina.turmaNome + ")"
                            : disciplina.nome
            );

            // Quantidade de alunos matriculados
            holder.tvQuantidadeAlunos.setText(
                    disciplina.qtdAlunos < 0
                            ? "..."
                            : disciplina.qtdAlunos
                                    + (disciplina.qtdAlunos == 1 ? " aluno" : " alunos")
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

                PopupMenu popupMenu =
                        new PopupMenu(
                                TelaDisciplinaProfessor.this,
                                holder.btnMenuDisciplina
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

                            if (opcao.equals("Editar")) {

                                mostrarDialogEditar(disciplina);

                            } else if (opcao.equals("Excluir")) {

                                confirmarExclusao(disciplina);
                            }

                            return true;
                        }
                );

                popupMenu.show();
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
    // EDITAR DISCIPLINA
    // =====================================================

    private void mostrarDialogEditar(Disciplina disciplina) {

        EditText edtNome = new EditText(this);

        edtNome.setText(disciplina.nome);
        edtNome.setSelection(disciplina.nome.length());
        edtNome.setSingleLine(true);
        edtNome.setHint("Nome da disciplina");

        int padding = (int) (20 * getResources()
                .getDisplayMetrics().density);

        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(edtNome);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Editar disciplina")
                .setView(container)
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Salvar",
                        null
                )
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setOnClickListener(v -> {

                            String novoNome =
                                    edtNome.getText()
                                            .toString()
                                            .trim();

                            if (novoNome.isEmpty()) {

                                edtNome.setError(
                                        "Informe o nome da disciplina"
                                );

                                return;
                            }

                            if (!novoNome.equals(disciplina.nome)) {
                                editarDisciplina(disciplina, novoNome);
                            }

                            dialog.dismiss();
                        })
        );

        dialog.show();
    }

    private void editarDisciplina(
            Disciplina disciplina,
            String novoNome
    ) {

        db.collection("disciplinas")
                .document(disciplina.id)
                .update("nome", novoNome)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Disciplina atualizada!",
                            Toast.LENGTH_SHORT
                    ).show();

                    carregarDisciplinas();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao atualizar disciplina.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =====================================================
    // EXCLUIR DISCIPLINA
    // =====================================================

    private void confirmarExclusao(Disciplina disciplina) {

        new AlertDialog.Builder(this)
                .setTitle("Excluir disciplina")
                .setMessage(
                        "Tem certeza que deseja excluir a disciplina \""
                                + disciplina.nome + "\"?"
                )
                .setNegativeButton(
                        "Cancelar",
                        null
                )
                .setPositiveButton(
                        "Excluir",
                        (dialog, which) ->
                                excluirDisciplina(disciplina)
                )
                .show();
    }

    private void excluirDisciplina(Disciplina disciplina) {

        // Exclusão lógica: a disciplina deixa de aparecer em
        // todas as telas (que filtram por "ativo"), mas as
        // atividades e aulas vinculadas continuam preservadas.
        db.collection("disciplinas")
                .document(disciplina.id)
                .update("ativo", false)
                .addOnSuccessListener(unused -> {

                    Toast.makeText(
                            this,
                            "Disciplina excluída!",
                            Toast.LENGTH_SHORT
                    ).show();

                    carregarDisciplinas();
                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao excluir disciplina.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
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
