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
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FrequenciaAulaActivity extends AppCompatActivity {

    private static final String[] STATUS_OPCOES = {"Presente", "Falta", "Falta justificada"};

    private FirebaseFirestore db;

    private String aulaId;
    private String disciplinaId;

    // Data da aula (dd/MM/yyyy): a perda de pontos por falta é contada por dia
    private String dataAula;

    private RecyclerView recyclerAlunosFrequencia;
    private TextView tvSemAlunos;

    private AlunoFrequenciaAdapter adapter;
    private final List<AlunoFrequencia> listaAlunos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_frequencia_aula);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

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

        carregarDataAula();
        carregarAlunosMatriculados();
    }

    private void carregarDataAula() {

        if (aulaId == null) {
            return;
        }

        db.collection("aulas")
                .document(aulaId)
                .get()
                .addOnSuccessListener(documento -> dataAula = documento.getString("data"));
    }

    // Mostra a frequência já salva desta aula, para não voltar tudo para "Presente"
    private void carregarFrequenciaSalva() {

        if (aulaId == null) {
            return;
        }

        db.collection("frequencias")
                .whereEqualTo("aulaId", aulaId)
                .get()
                .addOnSuccessListener(registros -> {

                    Map<String, String> statusPorAluno = new HashMap<>();

                    for (QueryDocumentSnapshot registro : registros) {
                        statusPorAluno.put(registro.getString("alunoId"), registro.getString("status"));
                    }

                    for (AlunoFrequencia aluno : listaAlunos) {

                        String status = statusPorAluno.get(aluno.alunoId);

                        if (status != null) {
                            aluno.status = status;
                        }
                    }

                    adapter.notifyDataSetChanged();
                });
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

                    List<String> idsAlunos = new ArrayList<>();

                    for (QueryDocumentSnapshot documento : querySnapshot) {

                        String alunoId = documento.getString("alunoId");
                        String alunoNome = documento.getString("alunoNome");

                        if (alunoId != null && alunoNome != null) {

                            listaAlunos.add(new AlunoFrequencia(alunoId, alunoNome));
                            idsAlunos.add(alunoId);
                        }
                    }

                    adapter.notifyDataSetChanged();

                    tvSemAlunos.setVisibility(listaAlunos.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerAlunosFrequencia.setVisibility(
                            listaAlunos.isEmpty() ? View.GONE : View.VISIBLE
                    );

                    carregarFotosAlunos(idsAlunos);
                    carregarFrequenciaSalva();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao carregar alunos.", Toast.LENGTH_SHORT
                ).show());
    }

    // Busca a foto de cada aluno na coleção "alunos" (em lotes de até 10,
    // limite do whereIn do Firestore) e preenche na lista já exibida.
    private void carregarFotosAlunos(List<String> idsAlunos) {

        for (int i = 0; i < idsAlunos.size(); i += 10) {

            List<String> lote = idsAlunos.subList(i, Math.min(i + 10, idsAlunos.size()));

            db.collection("alunos")
                    .whereIn(FieldPath.documentId(), lote)
                    .get()
                    .addOnSuccessListener(alunosSnapshot -> {

                        Map<String, String> fotosPorId = new HashMap<>();

                        for (QueryDocumentSnapshot documento : alunosSnapshot) {
                            fotosPorId.put(documento.getId(), documento.getString("fotoUrl"));
                        }

                        for (AlunoFrequencia aluno : listaAlunos) {

                            if (fotosPorId.containsKey(aluno.alunoId)) {
                                aluno.fotoUrl = fotosPorId.get(aluno.alunoId);
                            }
                        }

                        adapter.notifyDataSetChanged();
                    });
        }
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

            if (dataAula != null) {
                registro.put("data", dataAula);
            }

            String alunoId = aluno.alunoId;

            // Depois de gravar, confere a perda de 5 pontos do dia (falta sem justificativa)
            db.collection("frequencias")
                    .document(aulaId + "_" + alunoId)
                    .set(registro)
                    .addOnSuccessListener(unused ->
                            PenalidadeFaltaUtil.recalcular(db, alunoId, dataAula)
                    );
        }

        Toast.makeText(this, "Frequência salva!", Toast.LENGTH_SHORT).show();
        finish();
    }

    public static class AlunoFrequencia {

        String alunoId;
        String alunoNome;
        String fotoUrl;
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
            holder.imgAlunoFrequencia.setImageResource(R.drawable.ic_perfil);
            FotoUtil.carregar(FrequenciaAulaActivity.this, aluno.fotoUrl, holder.imgAlunoFrequencia);

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
                    Math.max(0, java.util.Arrays.asList(STATUS_OPCOES).indexOf(aluno.status))
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
            ImageView imgAlunoFrequencia;

            public ViewHolder(View itemView) {
                super(itemView);

                tvNomeAlunoFrequencia = itemView.findViewById(R.id.tvNomeAlunoFrequencia);
                spinnerStatusFrequencia = itemView.findViewById(R.id.spinnerStatusFrequencia);
                imgAlunoFrequencia = itemView.findViewById(R.id.imgAlunoFrequencia);
            }
        }
    }
}
