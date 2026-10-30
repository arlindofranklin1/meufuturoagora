package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Troca do ano letivo pelo admin. Sem senha extra (decisão N5): quem está
// logado e é admin basta, e o servidor confere (definir_ano_letivo). O ano
// anterior fica encerrado com os próprios bimestres, e a pontuação do ranking
// recomeça sozinha, porque é sempre calculada por ano letivo.
public class DefinirAnoLetivoActivity extends AppCompatActivity {

    private EditText edtAnoLetivo;
    private MaterialButton btnSalvarAnoLetivo;

    // Ano atual no servidor; nulo se o admin ainda não definiu nenhum
    private Integer anoAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_definir_ano_letivo);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        edtAnoLetivo = findViewById(R.id.edtAnoLetivo);

        btnSalvarAnoLetivo = findViewById(R.id.btnSalvarAnoLetivo);
        btnSalvarAnoLetivo.setOnClickListener(v -> validarAnoLetivo());

        carregarAnoLetivo();
    }

    private void carregarAnoLetivo() {

        Tarefas.executar(this, () -> Servicos.get(this).escola.anoAtual(), new Tarefas.Retorno<AnoLetivo>() {
            @Override
            public void sucesso(AnoLetivo ano) {

                if (ano != null) {
                    anoAtual = ano.ano;
                    edtAnoLetivo.setText(String.valueOf(ano.ano));
                }
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(DefinirAnoLetivoActivity.this, "Erro ao carregar o ano letivo: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void validarAnoLetivo() {

        String texto = edtAnoLetivo.getText().toString().trim();

        if (!texto.matches("\\d{4}")) {

            edtAnoLetivo.setError("Digite o ano letivo com 4 dígitos (ex: 2026)");
            return;
        }

        int novoAno = Integer.parseInt(texto);

        if (novoAno < 2000 || novoAno > 2100) {

            edtAnoLetivo.setError("Digite um ano entre 2000 e 2100");
            return;
        }

        if (anoAtual != null && novoAno == anoAtual) {

            Toast.makeText(this, "Esse já é o ano letivo atual.", Toast.LENGTH_SHORT).show();
            return;
        }

        confirmar(novoAno);
    }

    // Trocar o ano letivo recomeça a pontuação do ranking: pede uma confirmação
    private void confirmar(int novoAno) {

        String mensagem = anoAtual != null
                ? "O ano letivo vai mudar de " + anoAtual + " para " + novoAno + ". "
                + "Os dados de " + anoAtual + " ficam salvos no histórico e a pontuação "
                + "do ranking recomeça. Depois, defina os bimestres de " + novoAno + "."
                : "Definir " + novoAno + " como ano letivo?";

        new AlertDialog.Builder(this)
                .setTitle("Confirmar ano letivo")
                .setMessage(mensagem)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", (dialog, which) -> salvarAnoLetivo(novoAno))
                .show();
    }

    private void salvarAnoLetivo(int novoAno) {

        boolean trocou = anoAtual != null;

        btnSalvarAnoLetivo.setEnabled(false);

        Tarefas.executar(this, () -> {
            Servicos.get(this).escola.definirAnoLetivo(novoAno);
            return null;
        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {

                Toast.makeText(
                        DefinirAnoLetivoActivity.this,
                        trocou
                                ? "Ano letivo alterado! Defina agora os bimestres de " + novoAno + "."
                                : "Ano letivo salvo!",
                        Toast.LENGTH_LONG
                ).show();
                finish();
            }

            @Override
            public void erro(ErroApi erro) {
                btnSalvarAnoLetivo.setEnabled(true);
                Toast.makeText(DefinirAnoLetivoActivity.this, "Erro ao salvar: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }
}
