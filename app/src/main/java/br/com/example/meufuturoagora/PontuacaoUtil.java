package br.com.example.meufuturoagora;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;

// Pontuação do ranking = soma das notas das trilhas avaliadas
//                        - 5 pontos por dia com falta sem justificativa.
// É sempre recalculada a partir dos dados (em vez de somada aos poucos), assim
// reavaliar uma entrega ou corrigir uma falta não soma nem desconta duas vezes.
// Depois que o administrador encerra o bimestre, só conta o que veio depois.
class PontuacaoUtil {

    private PontuacaoUtil() {
    }

    static void recalcular(FirebaseFirestore db, String alunoId) {
        recalcular(db, alunoId, null);
    }

    // depois: executado quando o recálculo termina (com sucesso ou não)
    static void recalcular(FirebaseFirestore db, String alunoId, Runnable depois) {

        if (alunoId == null) {
            if (depois != null) {
                depois.run();
            }
            return;
        }

        Task<DocumentSnapshot> config = db.collection("configuracoes").document("geral").get();

        Task<QuerySnapshot> entregas = db.collection("entregas")
                .whereEqualTo("alunoId", alunoId)
                .whereEqualTo("avaliado", true)
                .get();

        Task<QuerySnapshot> penalidades = db.collection("penalidades")
                .whereEqualTo("alunoId", alunoId)
                .get();

        Tasks.whenAllSuccess(Arrays.asList(config, entregas, penalidades))
                .addOnSuccessListener(resultados -> {

                    Timestamp zeradaEm = config.getResult().getTimestamp("pontuacaoZeradaEm");

                    double total = 0;

                    for (DocumentSnapshot entrega : entregas.getResult()) {

                        Double nota = entrega.getDouble("nota");

                        if (nota != null && contaDepoisDoEncerramento(entrega, "avaliadoEm", zeradaEm)) {
                            total += nota;
                        }
                    }

                    for (DocumentSnapshot penalidade : penalidades.getResult()) {

                        if (contaDepoisDoEncerramento(penalidade, "criadoEm", zeradaEm)) {
                            total -= PenalidadeFaltaUtil.PONTOS_POR_DIA;
                        }
                    }

                    db.collection("alunos")
                            .document(alunoId)
                            .update("pontuacao", Math.round(total))
                            .addOnCompleteListener(tarefa -> {
                                if (depois != null) {
                                    depois.run();
                                }
                            });
                })
                .addOnFailureListener(e -> {
                    if (depois != null) {
                        depois.run();
                    }
                });
    }

    // Sem encerramento de bimestre, tudo conta; depois dele, só o que tem data posterior
    private static boolean contaDepoisDoEncerramento(DocumentSnapshot documento, String campoData,
                                                     Timestamp zeradaEm) {

        if (zeradaEm == null) {
            return true;
        }

        Timestamp data = documento.getTimestamp(campoData);

        return data != null && data.compareTo(zeradaEm) > 0;
    }
}
