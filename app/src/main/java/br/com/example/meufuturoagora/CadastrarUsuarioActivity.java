package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;
import br.com.example.meufuturoagora.dados.Turma;

// Cadastro de turmas, professores e alunos pelo admin. O aluno é matriculado
// nas disciplinas da turma pelo banco (decisão D9) e a conta Google se liga
// ao cadastro no primeiro login (vincular_conta).
public class CadastrarUsuarioActivity extends AppCompatActivity {

    private RadioGroup radioGroupTipo;
    private EditText edtNomeUsuario;
    private EditText edtEmailUsuario;
    private EditText edtAnoTurma;
    private LinearLayout containerTurma;
    private Spinner spinnerTurma;
    private MaterialButton btnSalvarUsuario;

    private final List<String> turmaIds = new ArrayList<>();
    private final List<String> turmaNomes = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cadastrar_usuario);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

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

        btnSalvarUsuario = findViewById(R.id.btnSalvarUsuario);
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
                        CadastrarUsuarioActivity.this,
                        R.layout.item_spinner,
                        turmaNomes
                );

                adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
                spinnerTurma.setAdapter(adapter);
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(CadastrarUsuarioActivity.this,
                        "Erro ao carregar turmas: " + erro.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
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
            Integer serie;

            try {
                serie = anoTexto.isEmpty() ? null : Integer.parseInt(anoTexto);
            } catch (NumberFormatException e) {
                serie = null;
            }

            if (serie == null) {
                edtAnoTurma.setError("Digite o ano da turma");
                return;
            }

            salvarTurma(nome, serie);
            return;
        }

        String email = edtEmailUsuario.getText().toString().trim();

        if (!email.matches("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")) {
            edtEmailUsuario.setError("Digite um e-mail válido");
            return;
        }

        if (tipo == R.id.radioAluno) {

            if (turmaIds.isEmpty()) {
                Toast.makeText(this, "Nenhuma turma ativa cadastrada.", Toast.LENGTH_LONG).show();
                return;
            }

            int posicao = spinnerTurma.getSelectedItemPosition();

            salvarPessoa(nome, email, Perfil.ALUNO, turmaIds.get(posicao));

        } else {

            salvarPessoa(nome, email, Perfil.PROFESSOR, null);
        }
    }

    // "6º ano A" e "6º Ano A " são a mesma turma (o banco recusa a repetida)
    private void salvarTurma(String nome, int serie) {

        executar(() -> Servicos.get(this).escola.criarTurma(nome, serie), "Turma cadastrada!",
                "Já existe uma turma com esse nome.", () -> {
                    carregarTurmas();
                    limparCampos();
                });
    }

    private void salvarPessoa(String nome, String email, String tipo, String turmaId) {

        Servicos servicos = Servicos.get(this);
        btnSalvarUsuario.setEnabled(false);

        Tarefas.executar(this, () -> servicos.perfis.cadastrar(email, nome, tipo, turmaId),
                new Tarefas.Retorno<Perfil>() {
                    @Override
                    public void sucesso(Perfil perfil) {
                        btnSalvarUsuario.setEnabled(true);
                        Toast.makeText(CadastrarUsuarioActivity.this, "Cadastrado!", Toast.LENGTH_SHORT).show();
                        limparCampos();
                    }

                    @Override
                    public void erro(ErroApi erro) {

                        btnSalvarUsuario.setEnabled(true);

                        if (erro.jaExiste()) {
                            oferecerAtualizacao(nome, email, tipo, turmaId);
                            return;
                        }

                        Toast.makeText(CadastrarUsuarioActivity.this, "Erro ao salvar: " + erro.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Decisão D10: um e-mail, um perfil. Cadastrar de novo o mesmo e-mail
    // atualiza nome e turma, desde que seja do mesmo tipo.
    private void oferecerAtualizacao(String nome, String email, String tipo, String turmaId) {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> servicos.perfis.buscarPorEmail(email), new Tarefas.Retorno<Perfil>() {
            @Override
            public void sucesso(Perfil existente) {

                if (existente == null) {
                    Toast.makeText(CadastrarUsuarioActivity.this, "Já existe um cadastro com esse e-mail.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (!existente.tipo.equals(tipo)) {
                    Toast.makeText(CadastrarUsuarioActivity.this,
                            "Esse e-mail já está cadastrado como " + existente.tipo + ".", Toast.LENGTH_LONG).show();
                    return;
                }

                new AlertDialog.Builder(CadastrarUsuarioActivity.this)
                        .setTitle("E-mail já cadastrado")
                        .setMessage("\"" + existente.nome + "\" já usa esse e-mail. Deseja atualizar o cadastro"
                                + (Perfil.ALUNO.equals(tipo) ? " com o novo nome e a nova turma?" : " com o novo nome?"))
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Atualizar", (dialog, which) ->
                                executar(() -> servicos.perfis.atualizar(existente.id, email, nome, turmaId,
                                                existente.ativo, existente.admin),
                                        "Cadastro atualizado!", null, () -> limparCampos()))
                        .show();
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(CadastrarUsuarioActivity.this, erro.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private <T> void executar(Tarefas.Trabalho<T> trabalho, String sucesso, String seJaExiste, Runnable depois) {

        btnSalvarUsuario.setEnabled(false);

        Tarefas.executar(this, trabalho, new Tarefas.Retorno<T>() {
            @Override
            public void sucesso(T resultado) {
                btnSalvarUsuario.setEnabled(true);
                Toast.makeText(CadastrarUsuarioActivity.this, sucesso, Toast.LENGTH_SHORT).show();
                depois.run();
            }

            @Override
            public void erro(ErroApi erro) {
                btnSalvarUsuario.setEnabled(true);
                String mensagem = erro.jaExiste() && seJaExiste != null ? seJaExiste : "Erro ao salvar: " + erro.getMessage();
                Toast.makeText(CadastrarUsuarioActivity.this, mensagem, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void limparCampos() {
        edtNomeUsuario.setText("");
        edtEmailUsuario.setText("");
        edtAnoTurma.setText("");
    }
}
