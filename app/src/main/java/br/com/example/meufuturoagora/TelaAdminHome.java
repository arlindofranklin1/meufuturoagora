package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TelaAdminHome extends AppCompatActivity {

    private FirebaseFirestore db;

    private TextView tvAnoLetivoAdmin;
    private TextView tvBimestreAdmin;
    private TextView tvPeriodoBimestreAdmin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_admin_home);

        db = FirebaseFirestore.getInstance();

        // Só contas marcadas como admin no banco podem ficar nesta tela
        AdminUtil.verificar(db, ehAdmin -> {

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

        db.collection("configuracoes")
                .document("geral")
                .get()
                .addOnSuccessListener(documento -> {

                    AnoLetivoUtil.atualizar(documento);

                    String ano = documento.getString("anoLetivo");

                    tvAnoLetivoAdmin.setText(
                            ano != null && !ano.isEmpty()
                                    ? "Ano letivo " + ano
                                    : "Ano letivo não definido"
                    );

                    int numero = BimestreUtil.numeroAtual(documento);

                    if (numero == 0) {

                        tvBimestreAdmin.setText("Fora de bimestre / não definido");
                        tvPeriodoBimestreAdmin.setText("Defina os bimestres do ano letivo");
                        return;
                    }

                    tvBimestreAdmin.setText(numero + "º Bimestre");
                    tvPeriodoBimestreAdmin.setText(
                            "De " + documento.getString("bimestre" + numero + "Inicio")
                                    + " até " + documento.getString("bimestre" + numero + "Fim")
                    );
                });
    }
}
