package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.WriteBatch;

import java.util.HashMap;
import java.util.Map;

public class DefinirAnoLetivoActivity extends AppCompatActivity {

    private FirebaseFirestore db;
    private EditText edtAnoLetivo;

    // Configuração atual, usada para arquivar o ano que está sendo encerrado
    private DocumentSnapshot configuracaoAtual;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_definir_ano_letivo);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        db = FirebaseFirestore.getInstance();

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        edtAnoLetivo = findViewById(R.id.edtAnoLetivo);

        MaterialButton btnSalvarAnoLetivo = findViewById(R.id.btnSalvarAnoLetivo);
        btnSalvarAnoLetivo.setOnClickListener(v -> validarAnoLetivo());

        carregarAnoLetivo();
    }

    private void carregarAnoLetivo() {

        db.collection("configuracoes")
                .document("geral")
                .get()
                .addOnSuccessListener(documento -> {

                    configuracaoAtual = documento;

                    String anoLetivo = documento.getString("anoLetivo");

                    if (anoLetivo != null) {
                        edtAnoLetivo.setText(anoLetivo);
                    }
                });
    }

    private void validarAnoLetivo() {

        String anoLetivo = edtAnoLetivo.getText().toString().trim();

        if (anoLetivo.length() != 4) {

            edtAnoLetivo.setError("Digite o ano letivo com 4 dígitos (ex: 2026)");
            return;
        }

        String anoAnterior = configuracaoAtual != null
                ? configuracaoAtual.getString("anoLetivo")
                : null;

        if (anoLetivo.equals(anoAnterior)) {

            Toast.makeText(this, "Esse já é o ano letivo atual.", Toast.LENGTH_SHORT).show();
            return;
        }

        pedirSenhaAdministrador(anoLetivo, anoAnterior);
    }

    // Trocar o ano letivo reinicia a pontuação do ranking: exige a senha do administrador
    private void pedirSenhaAdministrador(String anoLetivo, String anoAnterior) {

        EditText edtSenha = new EditText(this);
        edtSenha.setHint("Senha do administrador");
        edtSenha.setSingleLine(true);
        edtSenha.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        int padding = (int) (20 * getResources().getDisplayMetrics().density);

        FrameLayout container = new FrameLayout(this);
        container.setPadding(padding, padding / 2, padding, 0);
        container.addView(edtSenha);

        String mensagem = anoAnterior != null && !anoAnterior.isEmpty()
                ? "O ano letivo vai mudar de " + anoAnterior + " para " + anoLetivo + ". "
                + "Os dados de " + anoAnterior + " ficam salvos no histórico e a pontuação "
                + "do ranking recomeça. Digite a senha do administrador para confirmar."
                : "Digite a senha do administrador para definir o ano letivo " + anoLetivo + ".";

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Confirmar ano letivo")
                .setMessage(mensagem)
                .setView(container)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Confirmar", null)
                .create();

        dialog.setOnShowListener(d ->
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {

                    String senha = edtSenha.getText().toString().trim();

                    if (senha.isEmpty()) {
                        edtSenha.setError("Digite a senha");
                        return;
                    }

                    db.collection("administrador")
                            .whereEqualTo("senha", senha)
                            .get()
                            .addOnSuccessListener(resultado -> {

                                if (resultado.isEmpty()) {
                                    edtSenha.setError("Senha incorreta");
                                    return;
                                }

                                dialog.dismiss();
                                salvarAnoLetivo(anoLetivo, anoAnterior);
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    this,
                                    "Erro ao verificar a senha: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show());
                })
        );

        dialog.show();
    }

    private void salvarAnoLetivo(String anoLetivo, String anoAnterior) {

        WriteBatch batch = db.batch();

        // Arquiva o ano que termina com as datas dos bimestres que ele teve
        if (anoAnterior != null && !anoAnterior.isEmpty()) {

            Map<String, Object> arquivo = new HashMap<>();
            arquivo.put("ano", anoAnterior);
            arquivo.put("encerradoEm", FieldValue.serverTimestamp());

            for (int i = 1; i <= 4; i++) {

                String inicio = configuracaoAtual.getString("bimestre" + i + "Inicio");
                String fim = configuracaoAtual.getString("bimestre" + i + "Fim");

                arquivo.put("bimestre" + i + "Inicio", inicio != null ? inicio : "");
                arquivo.put("bimestre" + i + "Fim", fim != null ? fim : "");
            }

            batch.set(db.collection("anosLetivos").document(anoAnterior), arquivo, SetOptions.merge());
        }

        Map<String, Object> novoAno = new HashMap<>();
        novoAno.put("ano", anoLetivo);
        novoAno.put("iniciadoEm", FieldValue.serverTimestamp());

        batch.set(db.collection("anosLetivos").document(anoLetivo), novoAno, SetOptions.merge());

        Map<String, Object> dados = new HashMap<>();
        dados.put("anoLetivo", anoLetivo);

        if (anoAnterior != null && !anoAnterior.isEmpty()) {

            // O novo ano precisa de novos bimestres (os antigos ficaram arquivados)
            for (int i = 1; i <= 4; i++) {
                dados.put("bimestre" + i + "Inicio", "");
                dados.put("bimestre" + i + "Fim", "");
            }
        }

        batch.set(db.collection("configuracoes").document("geral"), dados, SetOptions.merge());

        batch.commit()
                .addOnSuccessListener(unused -> {

                    AnoLetivoUtil.definirAnoAtual(anoLetivo);
                    AnoLetivoUtil.carregar(db);

                    // Ranking passa a mostrar só os pontos do ano letivo escolhido
                    PontuacaoUtil.recalcularTodos(db);

                    Toast.makeText(
                            this,
                            anoAnterior != null && !anoAnterior.isEmpty()
                                    ? "Ano letivo alterado! Defina agora os bimestres de " + anoLetivo + "."
                                    : "Ano letivo salvo!",
                            Toast.LENGTH_LONG
                    ).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao salvar: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
