package br.com.example.meufuturoagora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.materialswitch.MaterialSwitch;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class ConfiguracoesActivity extends AppCompatActivity {

    private static final String PREFS_NOME = NotificacaoUtil.PREFS_NOME;
    private static final String CHAVE_NOTIFICACOES = NotificacaoUtil.CHAVE_NOTIFICACOES;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_configuracoes);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

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

        switchNotificacoes.setOnCheckedChangeListener((buttonView, isChecked) -> {

            preferencias.edit()
                    .putBoolean(CHAVE_NOTIFICACOES, isChecked)
                    .apply();

            // Desligado: para de aparecer na barra do celular, mas a lista do sino continua
            if (isChecked) {

                NotificacaoUtil.pedirPermissao(this);
                NotificacaoUtil.agendar(this);
            }
        });

        // =========================
        // SOBRE
        // =========================

        LinearLayout itemSobre = findViewById(R.id.itemSobre);

        itemSobre.setOnClickListener(v ->
                startActivity(new Intent(this, SobreActivity.class))
        );

        // =========================
        // EXCLUSÃO DOS DADOS (decisão D12)
        // =========================

        findViewById(R.id.itemExcluirDados).setOnClickListener(v -> pedirExclusao());

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

    // O pedido vai para o admin, que aprova (apaga tudo de vez) ou recusa
    private void pedirExclusao() {

        EditText edtMotivo = new EditText(this);
        edtMotivo.setHint("Motivo (opcional)");
        edtMotivo.setMaxLines(3);

        FrameLayout moldura = new FrameLayout(this);
        int margem = Math.round(20 * getResources().getDisplayMetrics().density);
        moldura.setPadding(margem, 0, margem, 0);
        moldura.addView(edtMotivo);

        new AlertDialog.Builder(this)
                .setTitle("Solicitar exclusão dos meus dados")
                .setMessage("A coordenação vai analisar o pedido. Se for aprovado, a sua conta e todos os "
                        + "seus dados no app (entregas, notas, frequência, diário e foto) são apagados de vez.")
                .setView(moldura)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Enviar pedido", (dialog, which) -> {

                    String motivo = edtMotivo.getText().toString().trim();

                    Tarefas.executar(this, () -> Servicos.get(this).perfis.solicitarExclusao(motivo),
                            new Tarefas.Retorno<String>() {
                                @Override
                                public void sucesso(String id) {
                                    Toast.makeText(ConfiguracoesActivity.this,
                                            "Pedido enviado. A coordenação vai analisar.", Toast.LENGTH_LONG).show();
                                }

                                @Override
                                public void erro(ErroApi erro) {
                                    Toast.makeText(ConfiguracoesActivity.this, erro.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .show();
    }

    private void sairDaConta() {

        // Apaga a sessão do aparelho e revoga no servidor, em segundo plano. Sem
        // Activity dona: esta tela fecha logo abaixo e o logout termina mesmo assim.
        Tarefas.executar(null, () -> {
            Servicos.get(this).auth.sair();
            return null;
        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {
            }

            @Override
            public void erro(ErroApi erro) {
                // sair() já limpa o aparelho mesmo sem resposta do servidor
            }
        });

        Intent intent = new Intent(this, TelaEntrar.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
