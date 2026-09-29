package br.com.example.meufuturoagora;

import android.widget.TextView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

class BimestreUtil {

    private BimestreUtil() {
    }

    static void carregarPeriodoAtual(FirebaseFirestore db, TextView titulo, TextView periodo) {

        db.collection("configuracoes")
                .document("geral")
                .get()
                .addOnSuccessListener(documento -> {

                    int numero = numeroAtual(documento);

                    if (numero == 0) {
                        titulo.setText("Bimestre");
                        periodo.setText("Bimestre não definido");
                        return;
                    }

                    titulo.setText(numero + "º Bimestre");
                    periodo.setText(
                            documento.getString("bimestre" + numero + "Inicio")
                                    + " - "
                                    + documento.getString("bimestre" + numero + "Fim")
                    );
                });
    }

    // Retorna o número (1 a 4) do bimestre em que a data de hoje está, ou 0 se nenhum
    private static int numeroAtual(DocumentSnapshot documento) {

        if (!documento.exists()) {
            return 0;
        }

        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Date hoje = new Date();

        for (int i = 1; i <= 4; i++) {

            String inicioTexto = documento.getString("bimestre" + i + "Inicio");
            String fimTexto = documento.getString("bimestre" + i + "Fim");

            if (inicioTexto == null || fimTexto == null) {
                continue;
            }

            Date inicio = tentarConverter(formato, inicioTexto);
            Date fim = tentarConverter(formato, fimTexto);

            if (inicio == null || fim == null) {
                continue;
            }

            if (!hoje.before(inicio) && !hoje.after(fim)) {
                return i;
            }
        }

        return 0;
    }

    private static Date tentarConverter(SimpleDateFormat formato, String texto) {

        try {

            return formato.parse(texto);

        } catch (ParseException e) {

            return null;
        }
    }
}
