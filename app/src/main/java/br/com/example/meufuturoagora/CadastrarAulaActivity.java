package br.com.example.meufuturoagora;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;

import br.com.example.meufuturoagora.dados.Aula;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Cadastro de aula pelo professor da disciplina. O professor é conferido pelo
// servidor (RLS), e o ano letivo e o bimestre saem da data da aula.
public class CadastrarAulaActivity extends AppCompatActivity {

    // O banco aceita de 1 a 20 aulas num mesmo registro
    private static final int MAXIMO_AULAS = 20;

    private EditText edtQuantidadeAulas;
    private EditText edtDescricaoAula;
    private EditText edtDataAula;
    private MaterialButton btnSalvarAula;

    private String disciplinaId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_cadastrar_aula);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        disciplinaId = getIntent().getStringExtra("disciplinaId");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        edtQuantidadeAulas = findViewById(R.id.edtQuantidadeAulas);
        edtDescricaoAula = findViewById(R.id.edtDescricaoAula);
        edtDataAula = findViewById(R.id.edtDataAula);

        edtDataAula.setOnClickListener(v -> abrirCalendario());

        btnSalvarAula = findViewById(R.id.btnSalvarAula);
        btnSalvarAula.setOnClickListener(v -> salvarAula());
    }

    // Abre no dia já escolhido; sem data, em hoje
    private void abrirCalendario() {

        LocalDate inicial;

        try {
            LocalDate escolhida = Datas.ler(edtDataAula.getText().toString());
            inicial = escolhida != null ? escolhida : LocalDate.now();
        } catch (IllegalArgumentException e) {
            inicial = LocalDate.now();
        }

        new DatePickerDialog(
                this,
                (view, ano, mes, dia) -> edtDataAula.setText(Datas.formatar(LocalDate.of(ano, mes + 1, dia))),
                inicial.getYear(),
                inicial.getMonthValue() - 1,
                inicial.getDayOfMonth()
        ).show();
    }

    private void salvarAula() {

        String quantidadeTexto = edtQuantidadeAulas.getText().toString().trim();
        String descricao = edtDescricaoAula.getText().toString().trim();
        String dataTexto = edtDataAula.getText().toString().trim();

        if (quantidadeTexto.isEmpty()) {

            edtQuantidadeAulas.setError("Digite a quantidade de aulas");
            return;
        }

        if (dataTexto.isEmpty()) {

            edtDataAula.setError("Selecione a data");
            return;
        }

        if (disciplinaId == null) {

            Toast.makeText(this, "Disciplina não identificada.", Toast.LENGTH_SHORT).show();
            return;
        }

        int quantidade;

        try {

            quantidade = Integer.parseInt(quantidadeTexto);

        } catch (NumberFormatException e) {

            edtQuantidadeAulas.setError("Digite um número válido");
            return;
        }

        if (quantidade < 1 || quantidade > MAXIMO_AULAS) {

            edtQuantidadeAulas.setError("Digite de 1 a " + MAXIMO_AULAS + " aulas");
            return;
        }

        LocalDate data;

        try {

            data = Datas.ler(dataTexto);

        } catch (IllegalArgumentException e) {

            edtDataAula.setError("Data inválida");
            return;
        }

        btnSalvarAula.setEnabled(false);

        Tarefas.executar(this,
                () -> Servicos.get(this).frequencias.cadastrarAula(disciplinaId, data, quantidade, descricao),
                new Tarefas.Retorno<Aula>() {
                    @Override
                    public void sucesso(Aula aula) {

                        Toast.makeText(CadastrarAulaActivity.this, "Aula cadastrada!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void erro(ErroApi erro) {

                        btnSalvarAula.setEnabled(true);
                        Toast.makeText(CadastrarAulaActivity.this, "Erro ao cadastrar aula: " + erro.getMessage(),
                                Toast.LENGTH_LONG).show();
                    }
                });
    }
}
