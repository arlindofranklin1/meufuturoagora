package br.com.example.meufuturoagora;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.Map;

// Tudo que é criado no aplicativo (disciplinas, trilhas, aulas, entregas,
// frequências e penalidades) leva o código do ano letivo e o número do bimestre
// em que foi feito. Assim os dados de anos e bimestres anteriores continuam no
// banco e o administrador consegue consultá-los no histórico.
class AnoLetivoUtil {

    static final String CAMPO_ANO = "anoLetivo";
    static final String CAMPO_BIMESTRE = "bimestre";

    // Configuração atual (configuracoes/geral), mantida em memória
    private static String anoAtual;
    private static DocumentSnapshot configuracao;

    private AnoLetivoUtil() {
    }

    // Lê o ano letivo e os bimestres atuais; chamado ao abrir as telas iniciais
    static void carregar(FirebaseFirestore db) {

        db.collection("configuracoes")
                .document("geral")
                .get()
                .addOnSuccessListener(AnoLetivoUtil::atualizar);
    }

    static void atualizar(DocumentSnapshot documento) {

        if (documento == null || !documento.exists()) {
            return;
        }

        configuracao = documento;

        String ano = documento.getString(CAMPO_ANO);

        if (ano != null && !ano.isEmpty()) {
            anoAtual = ano;
        }
    }

    static void definirAnoAtual(String ano) {
        anoAtual = ano;
    }

    // Sem configuração carregada, usa o ano do calendário
    static String atual() {

        if (anoAtual != null) {
            return anoAtual;
        }

        return String.valueOf(Calendar.getInstance().get(Calendar.YEAR));
    }

    // 1 a 4, ou 0 quando hoje não está em nenhum bimestre definido
    static int bimestreAtual() {
        return configuracao != null ? BimestreUtil.numeroAtual(configuracao) : 0;
    }

    // Vincula o registro ao ano letivo (e bimestre) atual
    static void marcar(Map<String, Object> dados) {

        dados.put(CAMPO_ANO, atual());

        int bimestre = bimestreAtual();

        if (bimestre > 0) {
            dados.put(CAMPO_BIMESTRE, bimestre);
        }
    }
}
