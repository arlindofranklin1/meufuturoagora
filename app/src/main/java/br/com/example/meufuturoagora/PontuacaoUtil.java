package br.com.example.meufuturoagora;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.Arrays;
import java.util.Date;

// Pontuação do ranking = soma das notas das trilhas avaliadas
//                        - 5 pontos por dia com falta sem justificativa.
// É sempre recalculada a partir dos dados (em vez de somada aos poucos), assim
// reavaliar uma entrega ou corrigir uma falta não soma nem desconta duas vezes.
// Só conta o que pertence ao ano letivo atual: ao trocar de ano letivo a pontuação
// recomeça, mas as notas e faltas antigas continuam salvas para o histórico (e
// voltam a contar se o ano letivo voltar a ser aquele).
class PontuacaoUtil {

    private PontuacaoUtil() {
    }

    static void recalcular(FirebaseFirestore db, String alunoId) {
        recalcular(db, alunoId, null);
    }

    // Recalcula todos os alunos (ex.: depois de trocar o ano letivo ou os bimestres),
    // para o ranking não mostrar pontos do ano anterior
    static void recalcularTodos(FirebaseFirestore db) {

        db.collection("alunos")
                .get()
                .addOnSuccessListener(alunos -> {
                    for (DocumentSnapshot aluno : alunos) {
                        recalcular(db, aluno.getId());
                    }
                });
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

                    DocumentSnapshot configuracao = config.getResult();

                    double total = 0;

                    for (DocumentSnapshot entrega : entregas.getResult()) {

                        Double nota = entrega.getDouble("nota");

                        if (nota != null && contaNoAnoAtual(entrega, configuracao,
                                data(entrega, "enviadoEm", "avaliadoEm"), "avaliadoEm")) {
                            total += nota;
                        }
                    }

                    for (DocumentSnapshot penalidade : penalidades.getResult()) {

                        if (contaNoAnoAtual(penalidade, configuracao,
                                data(penalidade, "data", "criadoEm"), "criadoEm")) {
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

    // Registro com ano letivo gravado conta só no próprio ano. Registro antigo (de antes
    // dessa marcação) conta se a data dele cair dentro do ano letivo atual — a mesma
    // regra usada no desempenho e no histórico do administrador.
    private static boolean contaNoAnoAtual(DocumentSnapshot documento, DocumentSnapshot configuracao,
                                           Date dataDoRegistro, String campoTimestamp) {

        String anoAtual = configuracao.getString(AnoLetivoUtil.CAMPO_ANO);
        String anoDoRegistro = documento.getString(AnoLetivoUtil.CAMPO_ANO);

        if (anoAtual != null && !anoAtual.isEmpty()) {

            if (anoDoRegistro != null) {
                return anoAtual.equals(anoDoRegistro);
            }

            return BimestreUtil.dataNoAno(dataDoRegistro, anoAtual, configuracao);
        }

        // Ano letivo nunca definido: regra antiga do encerramento de bimestre
        Timestamp zeradaEm = configuracao.getTimestamp("pontuacaoZeradaEm");

        if (zeradaEm == null) {
            return true;
        }

        Timestamp data = documento.getTimestamp(campoTimestamp);

        return data != null && data.compareTo(zeradaEm) > 0;
    }

    // Primeira data disponível entre os campos (Timestamp ou texto "dd/MM/yyyy")
    private static Date data(DocumentSnapshot documento, String... campos) {

        for (String campo : campos) {

            Object valor = documento.get(campo);

            if (valor instanceof Timestamp) {
                return ((Timestamp) valor).toDate();
            }

            if (valor instanceof String) {

                Date convertida = BimestreUtil.converter((String) valor);

                if (convertida != null) {
                    return convertida;
                }
            }
        }

        return null;
    }
}
