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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class DefinirBimestresActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private final EditText[] edtInicios = new EditText[4];
    private final EditText[] edtFinais = new EditText[4];

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_definir_bimestres);

        db = FirebaseFirestore.getInstance();

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

        MaterialButton btnSalvarBimestres = findViewById(R.id.btnSalvarBimestres);
        btnSalvarBimestres.setOnClickListener(v -> salvarBimestres());

        carregarBimestres();
    }

    private void abrirCalendario(EditText campo) {

        Calendar calendario = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, ano, mes, dia) -> campo.setText(
                        String.format("%02d/%02d/%04d", dia, mes + 1, ano)
                ),
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void carregarBimestres() {

        db.collection("configuracoes")
                .document("geral")
                .get()
                .addOnSuccessListener(documento -> {

                    if (!documento.exists()) {
                        return;
                    }

                    for (int i = 1; i <= 4; i++) {

                        String inicio = documento.getString("bimestre" + i + "Inicio");
                        String fim = documento.getString("bimestre" + i + "Fim");

                        if (inicio != null) {
                            edtInicios[i - 1].setText(inicio);
                        }

                        if (fim != null) {
                            edtFinais[i - 1].setText(fim);
                        }
                    }
                });
    }

    private void salvarBimestres() {

        Map<String, Object> dados = new HashMap<>();

        for (int i = 1; i <= 4; i++) {

            dados.put("bimestre" + i + "Inicio", edtInicios[i - 1].getText().toString().trim());
            dados.put("bimestre" + i + "Fim", edtFinais[i - 1].getText().toString().trim());
        }

        db.collection("configuracoes")
                .document("geral")
                .set(dados, SetOptions.merge())
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Bimestres salvos!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao salvar: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
