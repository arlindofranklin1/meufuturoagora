package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.EscolaRepositorio;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TelaAdminHome extends AppCompatActivity {

    private TextView tvAnoLetivoAdmin;
    private TextView tvBimestreAdmin;
    private TextView tvPeriodoBimestreAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_admin_home);

        // Só contas marcadas como admin no banco podem ficar nesta tela
        AdminUtil.verificar(this, ehAdmin -> {

            if (!ehAdmin && !isFinishing()) {

                Toast.makeText(this, "Acesso restrito ao administrador.", Toast.LENGTH_LONG).show();
                finish();
            }
        });

        tvAnoLetivoAdmin = findViewById(R.id.tvAnoLetivoAdmin);
        tvBimestreAdmin = findViewById(R.id.tvBimestreAdmin);
        tvPeriodoBimestreAdmin = findViewById(R.id.tvPeriodoBimestreAdmin);

        findViewById(R.id.itemAnoLetivo).setOnClickListener(v ->
                startActivity(new Intent(this, DefinirAnoLetivoActivity.class))
        );

        findViewById(R.id.itemBimestres).setOnClickListener(v ->
                startActivity(new Intent(this, DefinirBimestresActivity.class))
        );

        findViewById(R.id.itemCadastrarUsuario).setOnClickListener(v ->
                startActivity(new Intent(this, CadastrarUsuarioActivity.class))
        );

        findViewById(R.id.itemListaUsuarios).setOnClickListener(v ->
                startActivity(new Intent(this, ListaUsuariosActivity.class))
        );

        findViewById(R.id.itemHistorico).setOnClickListener(v ->
                startActivity(new Intent(this, HistoricoAnoLetivoActivity.class))
        );

        // Volta para o perfil, de onde a área do administrador foi aberta
        findViewById(R.id.itemSairAdmin).setOnClickListener(v -> finish());
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Recarrega ao voltar das telas de ano letivo e bimestres
        carregarCabecalho();
    }

    private void carregarCabecalho() {

        String hoje = new SimpleDateFormat("EEEE, dd/MM/yyyy", new Locale("pt", "BR"))
                .format(new Date());

        ((TextView) findViewById(R.id.tvDataHojeAdmin)).setText(
                "Hoje: " + hoje.substring(0, 1).toUpperCase() + hoje.substring(1)
        );

        EscolaRepositorio escola = Servicos.get(this).escola;

        Tarefas.executar(this, () -> {

            AnoLetivo ano = escola.anoAtual();

            if (ano == null) {
                return new Periodo(null, null);
            }

            // Mesmo critério do servidor: o bimestre que contém a data de hoje em Brasília
            LocalDate dataDeHoje = LocalDate.now(ZoneId.of("America/Sao_Paulo"));

            for (Bimestre bimestre : escola.bimestres(ano.ano)) {
                if (bimestre.contem(dataDeHoje)) {
                    return new Periodo(ano, bimestre);
                }
            }

            return new Periodo(ano, null);

        }, new Tarefas.Retorno<Periodo>() {
            @Override
            public void sucesso(Periodo periodo) {

                AnoLetivo ano = periodo.ano;
                Bimestre atual = periodo.bimestre;

                tvAnoLetivoAdmin.setText(ano != null ? "Ano letivo " + ano.ano : "Ano letivo não definido");

                if (atual == null) {
                    tvBimestreAdmin.setText("Fora de bimestre / não definido");
                    tvPeriodoBimestreAdmin.setText("Defina os bimestres do ano letivo");
                    return;
                }

                DateTimeFormatter formato = DateTimeFormatter.ofPattern("dd/MM/yyyy");

                tvBimestreAdmin.setText(atual.numero + "º Bimestre");
                tvPeriodoBimestreAdmin.setText(
                        "De " + atual.inicio.format(formato) + " até " + atual.fim.format(formato));
            }

            @Override
            public void erro(ErroApi erro) {
                tvAnoLetivoAdmin.setText("Ano letivo indisponível");
                Toast.makeText(TelaAdminHome.this, erro.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private static final class Periodo {

        final AnoLetivo ano;
        final Bimestre bimestre;

        Periodo(AnoLetivo ano, Bimestre bimestre) {
            this.ano = ano;
            this.bimestre = bimestre;
        }
    }
}
