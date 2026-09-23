package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CadastrarUsuarioActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private RadioGroup radioGroupTipo;
    private EditText edtNomeUsuario;
    private EditText edtEmailUsuario;
    private EditText edtAnoTurma;
    private LinearLayout containerTurma;
    private Spinner spinnerTurma;

    private final List<String> turmaIds = new ArrayList<>();
    private final List<String> turmaNomes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cadastrar_usuario);

        db = FirebaseFirestore.getInstance();

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        radioGroupTipo = findViewById(R.id.radioGroupTipo);
        edtNomeUsuario = findViewById(R.id.edtNomeUsuario);
        edtEmailUsuario = findViewById(R.id.edtEmailUsuario);
        edtAnoTurma = findViewById(R.id.edtAnoTurma);
        containerTurma = findViewById(R.id.containerTurma);
        spinnerTurma = findViewById(R.id.spinnerTurma);

        radioGroupTipo.setOnCheckedChangeListener((group, checkedId) -> atualizarCampos(checkedId));

        carregarTurmas();

        MaterialButton btnSalvarUsuario = findViewById(R.id.btnSalvarUsuario);
        btnSalvarUsuario.setOnClickListener(v -> salvarUsuario());
    }

    private void atualizarCampos(int checkedId) {

        boolean ehTurma = checkedId == R.id.radioTurma;
        boolean ehAluno = checkedId == R.id.radioAluno;

        edtNomeUsuario.setHint(ehTurma ? "Nome da turma (ex: 6º ano)" : "Nome");
        edtEmailUsuario.setVisibility(ehTurma ? LinearLayout.GONE : LinearLayout.VISIBLE);
        edtAnoTurma.setVisibility(ehTurma ? LinearLayout.VISIBLE : LinearLayout.GONE);
        containerTurma.setVisibility(ehAluno ? LinearLayout.VISIBLE : LinearLayout.GONE);
    }

    private void carregarTurmas() {

        db.collection("turmas")
                .whereEqualTo("ativo", true)
                .get()
                .addOnSuccessListener(turmas -> {

                    turmaIds.clear();
                    turmaNomes.clear();

                    for (QueryDocumentSnapshot documento : turmas) {

                        String nome = documento.getString("nome");

                        if (nome != null) {
                            turmaIds.add(documento.getId());
                            turmaNomes.add(nome);
                        }
                    }

                    ArrayAdapter<String> adapter = new ArrayAdapter<>(
                            this,
                            android.R.layout.simple_spinner_item,
                            turmaNomes
                    );

                    adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                    spinnerTurma.setAdapter(adapter);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao carregar turmas: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void salvarUsuario() {

        int tipo = radioGroupTipo.getCheckedRadioButtonId();
        String nome = edtNomeUsuario.getText().toString().trim();

        if (nome.isEmpty()) {
            edtNomeUsuario.setError(
                    tipo == R.id.radioTurma ? "Digite o nome da turma" : "Digite o nome"
            );
            return;
        }

        if (tipo == R.id.radioTurma) {

            String anoTexto = edtAnoTurma.getText().toString().trim();

            if (anoTexto.isEmpty()) {
                edtAnoTurma.setError("Digite o ano da turma");
                return;
            }

            salvarTurma(nome, Integer.parseInt(anoTexto));
            return;
        }

        String email = edtEmailUsuario.getText().toString().trim();

        if (email.isEmpty() || !email.contains("@")) {
            edtEmailUsuario.setError("Digite um e-mail válido");
            return;
        }

        if (tipo == R.id.radioAluno) {

            if (turmaIds.isEmpty()) {
                Toast.makeText(this, "Nenhuma turma ativa cadastrada.", Toast.LENGTH_LONG).show();
                return;
            }

            int posicao = spinnerTurma.getSelectedItemPosition();

            salvarAluno(nome, email, turmaIds.get(posicao), turmaNomes.get(posicao));

        } else {

            salvarProfessor(nome, email);
        }
    }

    private void salvarTurma(String nome, int ano) {

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("ano", ano);
        dados.put("ativo", true);

        db.collection("turmas")
                .whereEqualTo("nome", nome)
                .get()
                .addOnSuccessListener(existentes -> {

                    String documentoId = existentes.isEmpty()
                            ? null
                            : existentes.getDocuments().get(0).getId();

                    salvarDocumento("turmas", documentoId, dados);
                    carregarTurmas();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao cadastrar turma: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void salvarProfessor(String nome, String email) {

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("email", email);
        dados.put("ativo", true);

        db.collection("professores")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(existentes -> {

                    String documentoId = existentes.isEmpty()
                            ? null
                            : existentes.getDocuments().get(0).getId();

                    salvarDocumento("professores", documentoId, dados);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao cadastrar professor: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void salvarAluno(String nome, String email, String turmaId, String turmaNome) {

        Map<String, Object> dados = new HashMap<>();
        dados.put("nome", nome);
        dados.put("email", email);
        dados.put("ativo", true);
        dados.put("turmaId", turmaId);
        dados.put("turmaNome", turmaNome);

        db.collection("alunos")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(existentes -> {

                    String documentoId = existentes.isEmpty()
                            ? null
                            : existentes.getDocuments().get(0).getId();

                    salvarDocumento("alunos", documentoId, dados);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao cadastrar aluno: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }

    private void salvarDocumento(String colecao, String documentoId, Map<String, Object> dados) {

        com.google.android.gms.tasks.Task<Void> tarefa = documentoId != null
                ? db.collection(colecao).document(documentoId).set(dados, SetOptions.merge())
                : db.collection(colecao).document().set(dados);

        tarefa.addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Cadastrado!", Toast.LENGTH_SHORT).show();

                    edtNomeUsuario.setText("");
                    edtEmailUsuario.setText("");
                    edtAnoTurma.setText("");
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao salvar: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
