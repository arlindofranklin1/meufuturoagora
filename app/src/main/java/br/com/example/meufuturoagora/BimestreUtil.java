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

    static void carregarPeriodoAtual(FirebaseFirestore db, TextView destino) {

        db.collection("configuracoes")
                .document("geral")
                .get()
                .addOnSuccessListener(documento -> destino.setText(
                        periodoAtual(documento)
                ));
    }

    private static String periodoAtual(DocumentSnapshot documento) {

        if (!documento.exists()) {
            return "Bimestre não definido";
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
                return inicioTexto + " - " + fimTexto;
            }
        }

        return "Bimestre não definido";
    }

    private static Date tentarConverter(SimpleDateFormat formato, String texto) {

        try {

            return formato.parse(texto);

        } catch (ParseException e) {

            return null;
        }
    }
}
