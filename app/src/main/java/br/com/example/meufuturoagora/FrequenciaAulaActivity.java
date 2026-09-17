package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrequenciaAulaActivity extends AppCompatActivity {

    private static final String[] STATUS_OPCOES = {"Presente", "Falta"};

    private FirebaseFirestore db;

    private String aulaId;
    private String disciplinaId;

    private RecyclerView recyclerAlunosFrequencia;
    private TextView tvSemAlunos;

    private AlunoFrequenciaAdapter adapter;
    private final List<AlunoFrequencia> listaAlunos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_frequencia_aula);

        db = FirebaseFirestore.getInstance();

        aulaId = getIntent().getStringExtra("aulaId");
        disciplinaId = getIntent().getStringExtra("disciplinaId");

        String aulaDescricao = getIntent().getStringExtra("aulaDescricao");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        TextView tvTituloAula = findViewById(R.id.tvTituloAula);

        if (aulaDescricao != null && !aulaDescricao.isEmpty()) {
            tvTituloAula.setText(aulaDescricao);
        }

        recyclerAlunosFrequencia = findViewById(R.id.recyclerAlunosFrequencia);
        tvSemAlunos = findViewById(R.id.tvSemAlunos);

        recyclerAlunosFrequencia.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlunoFrequenciaAdapter(listaAlunos);
        recyclerAlunosFrequencia.setAdapter(adapter);

        MaterialButton btnSalvarFrequencia = findViewById(R.id.btnSalvarFrequencia);
        btnSalvarFrequencia.setOnClickListener(v -> salvarFrequencia());

        carregarAlunosMatriculados();
    }

    private void carregarAlunosMatriculados() {

        if (disciplinaId == null) {
            return;
        }

        db.collection("matriculas")
                .whereEqualTo("disciplinaId", disciplinaId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    listaAlunos.clear();

                    for (QueryDocumentSnapshot documento : querySnapshot) {

                        String alunoId = documento.getString("alunoId");
                        String alunoNome = documento.getString("alunoNome");

                        if (alunoId != null && alunoNome != null) {

                            listaAlunos.add(new AlunoFrequencia(alunoId, alunoNome));
                        }
                    }

                    adapter.notifyDataSetChanged();

                    tvSemAlunos.setVisibility(listaAlunos.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerAlunosFrequencia.setVisibility(
                            listaAlunos.isEmpty() ? View.GONE : View.VISIBLE
                    );
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao carregar alunos.", Toast.LENGTH_SHORT
                ).show());
    }

    private void salvarFrequencia() {

        if (aulaId == null) {

            Toast.makeText(this, "Aula não identificada.", Toast.LENGTH_SHORT).show();
            return;
        }

        for (AlunoFrequencia aluno : listaAlunos) {

            Map<String, Object> registro = new HashMap<>();

            registro.put("aulaId", aulaId);
            registro.put("disciplinaId", disciplinaId);
            registro.put("alunoId", aluno.alunoId);
            registro.put("alunoNome", aluno.alunoNome);
            registro.put("status", aluno.status);

            db.collection("frequencias")
                    .document(aulaId + "_" + aluno.alunoId)
                    .set(registro);
        }

        Toast.makeText(this, "Frequência salva!", Toast.LENGTH_SHORT).show();
        finish();
    }

    public static class AlunoFrequencia {

        String alunoId;
        String alunoNome;
        String status = "Presente";

        public AlunoFrequencia(String alunoId, String alunoNome) {
            this.alunoId = alunoId;
            this.alunoNome = alunoNome;
        }
    }

    private class AlunoFrequenciaAdapter
            extends RecyclerView.Adapter<AlunoFrequenciaAdapter.ViewHolder> {

        private final List<AlunoFrequencia> alunos;

        public AlunoFrequenciaAdapter(List<AlunoFrequencia> alunos) {
            this.alunos = alunos;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(
                    R.layout.item_aluno_frequencia, parent, false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            AlunoFrequencia aluno = alunos.get(position);

            holder.tvNomeAlunoFrequencia.setText(aluno.alunoNome);

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    FrequenciaAulaActivity.this,
                    android.R.layout.simple_spinner_item,
                    STATUS_OPCOES
            );

            spinnerAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
            );

            holder.spinnerStatusFrequencia.setAdapter(spinnerAdapter);

            holder.spinnerStatusFrequencia.setSelection(
                    aluno.status.equals("Falta") ? 1 : 0
            );

            holder.spinnerStatusFrequencia.setOnItemSelectedListener(
                    new android.widget.AdapterView.OnItemSelectedListener() {

                        @Override
                        public void onItemSelected(
                                android.widget.AdapterView<?> parent,
                                View view,
                                int position,
                                long id
                        ) {
                            aluno.status = STATUS_OPCOES[position];
                        }

                        @Override
                        public void onNothingSelected(
                                android.widget.AdapterView<?> parent
                        ) {
                        }
                    }
            );
        }

        @Override
        public int getItemCount() {
            return alunos.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvNomeAlunoFrequencia;
            Spinner spinnerStatusFrequencia;

            public ViewHolder(View itemView) {
                super(itemView);

                tvNomeAlunoFrequencia = itemView.findViewById(R.id.tvNomeAlunoFrequencia);
                spinnerStatusFrequencia = itemView.findViewById(R.id.spinnerStatusFrequencia);
            }
        }
    }
}
