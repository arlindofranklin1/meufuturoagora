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

                    AnoLetivoUtil.atualizar(documento);

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
    static int numeroAtual(DocumentSnapshot documento) {
        return numeroDaData(documento, new Date());
    }

    // Retorna o número (1 a 4) do bimestre que contém a data, ou 0 se nenhum
    static int numeroDaData(DocumentSnapshot documento, Date data) {

        if (documento == null || !documento.exists() || data == null) {
            return 0;
        }

        for (int i = 1; i <= 4; i++) {

            Date inicio = converter(documento.getString("bimestre" + i + "Inicio"));
            Date fim = fimDoDia(converter(documento.getString("bimestre" + i + "Fim")));

            if (inicio == null || fim == null) {
                continue;
            }

            if (!data.before(inicio) && !data.after(fim)) {
                return i;
            }
        }

        return 0;
    }

    // A data está dentro do ano letivo? Vale o período dos bimestres (do 1º início ao
    // último fim); sem bimestres definidos, vale o ano do calendário.
    static boolean dataNoAno(Date data, String ano, DocumentSnapshot datas) {

        if (data == null || ano == null) {
            return false;
        }

        Date inicio = null;
        Date fim = null;

        if (datas != null && datas.exists()) {

            for (int i = 1; i <= 4; i++) {

                Date ini = converter(datas.getString("bimestre" + i + "Inicio"));
                Date f = fimDoDia(converter(datas.getString("bimestre" + i + "Fim")));

                if (ini != null && (inicio == null || ini.before(inicio))) inicio = ini;
                if (f != null && (fim == null || f.after(fim))) fim = f;
            }
        }

        if (inicio != null && fim != null) {
            return !data.before(inicio) && !data.after(fim);
        }

        java.util.Calendar calendario = java.util.Calendar.getInstance();
        calendario.setTime(data);

        return String.valueOf(calendario.get(java.util.Calendar.YEAR)).equals(ano);
    }

    // Converte "dd/MM/yyyy"; null se vazio ou inválido
    static Date converter(String texto) {

        if (texto == null || texto.isEmpty()) {
            return null;
        }

        try {

            return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(texto);

        } catch (ParseException e) {

            return null;
        }
    }

    // O último dia do bimestre vale até 23:59:59
    static Date fimDoDia(Date data) {
        return data != null ? new Date(data.getTime() + 24L * 60 * 60 * 1000 - 1) : null;
    }
}
