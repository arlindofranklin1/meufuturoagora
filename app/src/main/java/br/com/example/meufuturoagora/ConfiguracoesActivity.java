package br.com.example.meufuturoagora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;

public class ConfiguracoesActivity extends AppCompatActivity {

    private static final String PREFS_NOME = "preferencias_app";
    private static final String CHAVE_NOTIFICACOES = "notificacoes_ativas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configuracoes);

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        // =========================
        // PREFERÊNCIA DE NOTIFICAÇÕES
        // =========================

        SharedPreferences preferencias = getSharedPreferences(PREFS_NOME, MODE_PRIVATE);

        MaterialSwitch switchNotificacoes = findViewById(R.id.switchNotificacoes);

        switchNotificacoes.setChecked(
                preferencias.getBoolean(CHAVE_NOTIFICACOES, true)
        );

        switchNotificacoes.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferencias.edit()
                        .putBoolean(CHAVE_NOTIFICACOES, isChecked)
                        .apply()
        );

        // =========================
        // SOBRE
        // =========================

        LinearLayout itemSobre = findViewById(R.id.itemSobre);

        itemSobre.setOnClickListener(v ->
                startActivity(new Intent(this, SobreActivity.class))
        );

        // =========================
        // SAIR DA CONTA
        // =========================

        LinearLayout itemSair = findViewById(R.id.itemSair);

        itemSair.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Sair da conta")
                        .setMessage("Tem certeza que deseja sair?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Sair", (dialog, which) -> sairDaConta())
                        .show()
        );
    }

    private void sairDaConta() {

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, TelaEntrar.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
