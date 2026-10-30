package br.com.example.meufuturoagora;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.Bimestres;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.EscolaRepositorio;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Datas dos quatro bimestres do ano letivo atual. A tela confere o básico na
// hora (Bimestres.validar) e o servidor confere de novo ao gravar
// (definir_bimestres), que troca todos os bimestres do ano de uma vez.
public class DefinirBimestresActivity extends AppCompatActivity {

    private final EditText[] edtInicios = new EditText[4];
    private final EditText[] edtFinais = new EditText[4];

    private MaterialButton btnSalvarBimestres;

    // Ano a que os bimestres pertencem; nulo até carregar (ou se não houver ano definido)
    private Integer ano;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_definir_bimestres);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        int[] idsCards = {R.id.bimestre1, R.id.bimestre2, R.id.bimestre3, R.id.bimestre4};

        for (int i = 0; i < idsCards.length; i++) {

            View card = findViewById(idsCards[i]);

            TextView tvTitulo = card.findViewById(R.id.tvTituloBimestre);
            tvTitulo.setText("Bimestre " + (i + 1));

            EditText edtInicio = card.findViewById(R.id.edtPrazoInicial);
            EditText edtFinal = card.findViewById(R.id.edtPrazoFinal);

            edtInicio.setOnClickListener(v -> abrirCalendario(edtInicio));
            edtFinal.setOnClickListener(v -> abrirCalendario(edtFinal));

            edtInicios[i] = edtInicio;
            edtFinais[i] = edtFinal;
        }

        btnSalvarBimestres = findViewById(R.id.btnSalvarBimestres);
        btnSalvarBimestres.setOnClickListener(v -> salvarBimestres());

        carregarBimestres();
    }

    // Abre no dia já escolhido; sem data, em hoje
    private void abrirCalendario(EditText campo) {

        LocalDate inicial;

        try {
            LocalDate escolhida = Bimestres.ler(campo.getText().toString());
            inicial = escolhida != null ? escolhida : LocalDate.now();
        } catch (IllegalArgumentException e) {
            inicial = LocalDate.now();
        }

        new DatePickerDialog(
                this,
                (view, anoEscolhido, mes, dia) -> campo.setText(
                        Bimestres.formatar(LocalDate.of(anoEscolhido, mes + 1, dia))
                ),
                inicial.getYear(),
                inicial.getMonthValue() - 1,
                inicial.getDayOfMonth()
        ).show();
    }

    private void carregarBimestres() {

        EscolaRepositorio escola = Servicos.get(this).escola;

        Tarefas.executar(this, () -> {

            AnoLetivo atual = escola.anoAtual();

            return atual == null
                    ? new Carregado(null, new ArrayList<>())
                    : new Carregado(atual.ano, escola.bimestres(atual.ano));

        }, new Tarefas.Retorno<Carregado>() {
            @Override
            public void sucesso(Carregado carregado) {

                ano = carregado.ano;

                if (ano == null) {
                    Toast.makeText(DefinirBimestresActivity.this,
                            "Defina o ano letivo antes dos bimestres.", Toast.LENGTH_LONG).show();
                    return;
                }

                for (Bimestre bimestre : carregado.bimestres) {

                    int posicao = bimestre.numero - 1;

                    if (posicao >= 0 && posicao < edtInicios.length) {
                        edtInicios[posicao].setText(Bimestres.formatar(bimestre.inicio));
                        edtFinais[posicao].setText(Bimestres.formatar(bimestre.fim));
                    }
                }
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(DefinirBimestresActivity.this, "Erro ao carregar os bimestres: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void salvarBimestres() {

        if (ano == null) {
            Toast.makeText(this, "Defina o ano letivo antes dos bimestres.", Toast.LENGTH_LONG).show();
            return;
        }

        List<Bimestres.Entrada> entradas = new ArrayList<>();

        for (int i = 0; i < edtInicios.length; i++) {
            entradas.add(new Bimestres.Entrada(
                    edtInicios[i].getText().toString(),
                    edtFinais[i].getText().toString()
            ));
        }

        Bimestres.Resultado resultado = Bimestres.validar(ano, entradas);

        if (!resultado.ok()) {
            Toast.makeText(this, resultado.erro, Toast.LENGTH_LONG).show();
            return;
        }

        int anoDosBimestres = ano;

        btnSalvarBimestres.setEnabled(false);

        Tarefas.executar(this, () -> {
            Servicos.get(this).escola.definirBimestres(anoDosBimestres, resultado.bimestres);
            return null;
        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object ignorado) {
                Toast.makeText(DefinirBimestresActivity.this, "Bimestres salvos!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void erro(ErroApi erro) {
                btnSalvarBimestres.setEnabled(true);
                Toast.makeText(DefinirBimestresActivity.this, "Erro ao salvar: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private static final class Carregado {

        final Integer ano;
        final List<Bimestre> bimestres;

        Carregado(Integer ano, List<Bimestre> bimestres) {
            this.ano = ano;
            this.bimestres = bimestres;
        }
    }
}
