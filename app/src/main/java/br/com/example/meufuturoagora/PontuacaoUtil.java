package br.com.example.meufuturoagora;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// Pontuação do ranking = soma das notas das trilhas avaliadas (trilhas e disciplinas
//                        não excluídas) - 5 pontos por dia com falta sem justificativa.
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

        // Só contam as notas de trilhas que ainda existem (não excluídas) em disciplinas
        // ativas — a mesma regra da tela de desempenho, para os totais baterem.
        Task<Set<String>> atividadesValidas = entregas.continueWithTask(t ->
                atividadesValidas(db, t.getResult().getDocuments()));

        Tasks.whenAllSuccess(Arrays.asList(config, entregas, penalidades, atividadesValidas))
                .addOnSuccessListener(resultados -> {

                    DocumentSnapshot configuracao = config.getResult();
                    Set<String> validas = atividadesValidas.getResult();

                    double total = 0;

                    for (DocumentSnapshot entrega : entregas.getResult()) {

                        Double nota = entrega.getDouble("nota");

                        if (nota != null && validas.contains(entrega.getString("atividadeId"))
                                && contaNoAnoAtual(entrega, configuracao,
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

    // IDs das atividades das entregas que estão ativas e cuja disciplina também está ativa
    private static Task<Set<String>> atividadesValidas(FirebaseFirestore db,
                                                       List<DocumentSnapshot> entregas) {

        Set<String> idsAtividades = new HashSet<>();

        for (DocumentSnapshot entrega : entregas) {

            String atividadeId = entrega.getString("atividadeId");

            if (atividadeId != null) {
                idsAtividades.add(atividadeId);
            }
        }

        return buscarPorIds(db, "atividades", idsAtividades).continueWithTask(t -> {

            Map<String, String> disciplinaDaAtividade = new HashMap<>();

            for (DocumentSnapshot atividade : t.getResult()) {

                String disciplinaId = atividade.getString("disciplinaId");

                if (disciplinaId != null && !Boolean.FALSE.equals(atividade.getBoolean("ativo"))) {
                    disciplinaDaAtividade.put(atividade.getId(), disciplinaId);
                }
            }

            return buscarPorIds(db, "disciplinas", new HashSet<>(disciplinaDaAtividade.values()))
                    .continueWith(td -> {

                        Set<String> disciplinasAtivas = new HashSet<>();

                        for (DocumentSnapshot disciplina : td.getResult()) {
                            if (!Boolean.FALSE.equals(disciplina.getBoolean("ativo"))) {
                                disciplinasAtivas.add(disciplina.getId());
                            }
                        }

                        Set<String> validas = new HashSet<>();

                        for (Map.Entry<String, String> item : disciplinaDaAtividade.entrySet()) {
                            if (disciplinasAtivas.contains(item.getValue())) {
                                validas.add(item.getKey());
                            }
                        }

                        return validas;
                    });
        });
    }

    // Busca documentos pelo ID em lotes de até 10 (limite do whereIn do Firestore)
    private static Task<List<DocumentSnapshot>> buscarPorIds(FirebaseFirestore db, String colecao,
                                                             Collection<String> ids) {

        List<String> lista = new ArrayList<>(ids);
        List<Task<QuerySnapshot>> consultas = new ArrayList<>();

        for (int i = 0; i < lista.size(); i += 10) {
            consultas.add(db.collection(colecao)
                    .whereIn(FieldPath.documentId(),
                            new ArrayList<>(lista.subList(i, Math.min(i + 10, lista.size()))))
                    .get());
        }

        return Tasks.whenAllSuccess(consultas).continueWith(t -> {

            List<DocumentSnapshot> documentos = new ArrayList<>();

            for (Object resultado : t.getResult()) {
                documentos.addAll(((QuerySnapshot) resultado).getDocuments());
            }

            return documentos;
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
