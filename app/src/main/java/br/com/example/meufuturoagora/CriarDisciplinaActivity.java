package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;
import br.com.example.meufuturoagora.dados.Turma;

// O professor cria as próprias disciplinas (decisão D1). O responsável é o
// professor logado, o ano letivo e o bimestre vêm do servidor, e os alunos da
// turma são matriculados na hora pelo banco (decisão D9).
public class CriarDisciplinaActivity extends AppCompatActivity {

    private EditText edtNomeDisciplina;
    private Spinner spinnerTurmaDisciplina;
    private MaterialButton btnSalvarDisciplina;

    private final List<String> turmaIds = new ArrayList<>();
    private final List<String> turmaNomes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_criar_disciplina);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        edtNomeDisciplina = findViewById(R.id.edtNomeDisciplina);
        spinnerTurmaDisciplina = findViewById(R.id.spinnerTurmaDisciplina);

        carregarTurmas();

        btnSalvarDisciplina = findViewById(R.id.btnSalvarDisciplina);
        btnSalvarDisciplina.setOnClickListener(v -> salvarDisciplina());
    }

    private void carregarTurmas() {

        Tarefas.executar(this, () -> Servicos.get(this).escola.turmas(), new Tarefas.Retorno<List<Turma>>() {
            @Override
            public void sucesso(List<Turma> turmas) {

                turmaIds.clear();
                turmaNomes.clear();

                for (Turma turma : turmas) {
                    if (turma.ativo) {
                        turmaIds.add(turma.id);
                        turmaNomes.add(turma.nome);
                    }
                }

                ArrayAdapter<String> adapter = new ArrayAdapter<>(
                        CriarDisciplinaActivity.this,
                        R.layout.item_spinner,
                        turmaNomes
                );

                adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
                spinnerTurmaDisciplina.setAdapter(adapter);
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(CriarDisciplinaActivity.this, "Erro ao carregar turmas: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void salvarDisciplina() {

        String nome = edtNomeDisciplina.getText().toString().trim();

        if (nome.isEmpty()) {
            edtNomeDisciplina.setError("Digite o nome da disciplina");
            return;
        }

        if (turmaIds.isEmpty()) {
            Toast.makeText(this, "Nenhuma turma ativa cadastrada.", Toast.LENGTH_LONG).show();
            return;
        }

        String turmaId = turmaIds.get(spinnerTurmaDisciplina.getSelectedItemPosition());

        btnSalvarDisciplina.setEnabled(false);

        Tarefas.executar(this, () -> Servicos.get(this).disciplinas.criar(nome, turmaId),
                new Tarefas.Retorno<Disciplina>() {
                    @Override
                    public void sucesso(Disciplina criada) {
                        Toast.makeText(CriarDisciplinaActivity.this, "Disciplina criada!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        btnSalvarDisciplina.setEnabled(true);
                        Toast.makeText(CriarDisciplinaActivity.this, "Erro ao criar disciplina: " + erro.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
