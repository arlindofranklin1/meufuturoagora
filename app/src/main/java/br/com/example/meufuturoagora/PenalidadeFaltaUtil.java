package br.com.example.meufuturoagora;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

// Falta sem justificativa tira 5 pontos do ranking, no máximo uma vez por dia:
// várias faltas no mesmo dia (outras aulas ou outras disciplinas) não descontam de novo.
// Falta justificada não desconta. Se a falta for corrigida, os pontos voltam.
class PenalidadeFaltaUtil {

    static final int PONTOS_POR_DIA = 5;

    static final String STATUS_FALTA = "Falta";

    private PenalidadeFaltaUtil() {
    }

    // Confere se o aluno tem alguma falta sem justificativa na data e aplica
    // ou devolve os 5 pontos do dia conforme o caso.
    static void recalcular(FirebaseFirestore db, String alunoId, String data) {

        if (alunoId == null || data == null || data.isEmpty()) {
            return;
        }

        db.collection("frequencias")
                .whereEqualTo("alunoId", alunoId)
                .whereEqualTo("data", data)
                .get()
                .addOnSuccessListener(registros -> {

                    boolean faltouSemJustificativa = false;

                    for (QueryDocumentSnapshot registro : registros) {

                        if (STATUS_FALTA.equals(registro.getString("status"))) {
                            faltouSemJustificativa = true;
                            break;
                        }
                    }

                    aplicar(db, alunoId, data, faltouSemJustificativa);
                });
    }

    private static void aplicar(FirebaseFirestore db, String alunoId, String data,
                                boolean deveDescontar) {

        // Um documento por aluno e dia marca que os 5 pontos daquele dia já foram descontados
        DocumentReference penalidade = db.collection("penalidades")
                .document(alunoId + "_" + data.replace("/", ""));

        penalidade.get().addOnSuccessListener(existente -> {

            if (deveDescontar && !existente.exists()) {

                Map<String, Object> dados = new HashMap<>();
                dados.put("alunoId", alunoId);
                dados.put("data", data);
                dados.put("pontos", -PONTOS_POR_DIA);
                dados.put("criadoEm", FieldValue.serverTimestamp());

                penalidade.set(dados)
                        .addOnSuccessListener(unused -> PontuacaoUtil.recalcular(db, alunoId));

            } else if (!deveDescontar && existente.exists()) {

                penalidade.delete()
                        .addOnSuccessListener(unused -> PontuacaoUtil.recalcular(db, alunoId));
            }
        });
    }
}
