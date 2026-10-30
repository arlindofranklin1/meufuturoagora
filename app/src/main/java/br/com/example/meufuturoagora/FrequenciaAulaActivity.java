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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Frequencia;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Frequência de uma aula: um status por aluno matriculado. O servidor grava a
// lista toda de uma vez (registrar_frequencia: tudo ou nada) e calcula o
// desconto de 5 pontos por dia com falta sem justificativa a partir dela.
public class FrequenciaAulaActivity extends AppCompatActivity {

    // O que o professor vê; o banco guarda presente, falta e falta_justificada
    private static final String[] STATUS_OPCOES = {"Presente", "Falta", "Falta justificada"};
    private static final String[] STATUS_NO_BANCO = {Frequencia.PRESENTE, Frequencia.FALTA, Frequencia.FALTA_JUSTIFICADA};

    private String aulaId;
    private String disciplinaId;

    private RecyclerView recyclerAlunosFrequencia;
    private TextView tvSemAlunos;
    private MaterialButton btnSalvarFrequencia;

    private AlunoFrequenciaAdapter adapter;
    private final List<AlunoFrequencia> listaAlunos = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_frequencia_aula);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

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

        btnSalvarFrequencia = findViewById(R.id.btnSalvarFrequencia);
        btnSalvarFrequencia.setOnClickListener(v -> salvarFrequencia());

        carregarAlunos();
    }

    // Alunos matriculados (com nome e foto) e a frequência já salva, para não
    // voltar tudo para "Presente" ao reabrir a aula
    private void carregarAlunos() {

        if (disciplinaId == null || aulaId == null) {
            return;
        }

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            Map<String, String> statusPorAluno = new HashMap<>();

            for (Frequencia registro : servicos.frequencias.frequenciaDaAula(aulaId)) {
                statusPorAluno.put(registro.alunoId, registro.status);
            }

            List<AlunoFrequencia> alunos = new ArrayList<>();

            for (Participante participante : servicos.disciplinas.participantes(disciplinaId)) {

                if (!Perfil.ALUNO.equals(participante.tipo)) {
                    continue;
                }

                AlunoFrequencia aluno = new AlunoFrequencia(participante.perfilId, participante.nome,
                        participante.fotoPath, participante.fotoGoogleUrl);

                String salvo = statusPorAluno.get(participante.perfilId);

                if (salvo != null) {
                    aluno.status = rotuloDoStatus(salvo);
                }

                alunos.add(aluno);
            }

            return alunos;

        }, new Tarefas.Retorno<List<AlunoFrequencia>>() {
            @Override
            public void sucesso(List<AlunoFrequencia> alunos) {

                listaAlunos.clear();
                listaAlunos.addAll(alunos);
                adapter.notifyDataSetChanged();

                tvSemAlunos.setVisibility(listaAlunos.isEmpty() ? View.VISIBLE : View.GONE);
                recyclerAlunosFrequencia.setVisibility(listaAlunos.isEmpty() ? View.GONE : View.VISIBLE);
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(FrequenciaAulaActivity.this, "Erro ao carregar alunos: " + erro.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private static String rotuloDoStatus(String statusNoBanco) {

        int posicao = Arrays.asList(STATUS_NO_BANCO).indexOf(statusNoBanco);
        return posicao >= 0 ? STATUS_OPCOES[posicao] : STATUS_OPCOES[0];
    }

    private static String statusNoBanco(String rotulo) {

        int posicao = Arrays.asList(STATUS_OPCOES).indexOf(rotulo);
        return STATUS_NO_BANCO[Math.max(0, posicao)];
    }

    private void salvarFrequencia() {

        if (aulaId == null) {

            Toast.makeText(this, "Aula não identificada.", Toast.LENGTH_SHORT).show();
            return;
        }

        if (listaAlunos.isEmpty()) {

            Toast.makeText(this, "Não há alunos matriculados nesta disciplina.", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Frequencia> registros = new ArrayList<>();

        for (AlunoFrequencia aluno : listaAlunos) {
            registros.add(new Frequencia(aulaId, aluno.alunoId, statusNoBanco(aluno.status)));
        }

        btnSalvarFrequencia.setEnabled(false);

        Tarefas.executar(this, () -> Servicos.get(this).frequencias.registrar(aulaId, registros),
                new Tarefas.Retorno<Integer>() {
                    @Override
                    public void sucesso(Integer total) {

                        Toast.makeText(FrequenciaAulaActivity.this, "Frequência salva!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void erro(ErroApi erro) {

                        btnSalvarFrequencia.setEnabled(true);
                        Toast.makeText(FrequenciaAulaActivity.this, "Erro ao salvar a frequência: " + erro.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    public static class AlunoFrequencia {

        String alunoId;
        String alunoNome;
        String fotoPath;
        String fotoGoogleUrl;
        String status = STATUS_OPCOES[0];

        public AlunoFrequencia(String alunoId, String alunoNome, String fotoPath, String fotoGoogleUrl) {
            this.alunoId = alunoId;
            this.alunoNome = alunoNome;
            this.fotoPath = fotoPath;
            this.fotoGoogleUrl = fotoGoogleUrl;
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
            PerfilFotoUtil.mostrar(FrequenciaAulaActivity.this, aluno.fotoPath, aluno.fotoGoogleUrl,
                    holder.imgAlunoFrequencia);

            ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                    FrequenciaAulaActivity.this,
                    R.layout.item_spinner,
                    STATUS_OPCOES
            );

            spinnerAdapter.setDropDownViewResource(
                    R.layout.item_spinner_dropdown
            );

            holder.spinnerStatusFrequencia.setAdapter(spinnerAdapter);

            holder.spinnerStatusFrequencia.setSelection(
                    Math.max(0, Arrays.asList(STATUS_OPCOES).indexOf(aluno.status))
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
