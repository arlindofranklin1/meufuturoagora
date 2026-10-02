package br.com.example.meufuturoagora;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class DisciplinaUtil {

    interface Callback {
        // disciplinaId -> nome atual, só de disciplinas que existem e estão ativas
        void onCarregadas(LinkedHashMap<String, String> disciplinas);
    }

    private DisciplinaUtil() {
    }

    // As matrículas guardam uma cópia do nome da disciplina. Aqui elas são conferidas
    // com a coleção "disciplinas": disciplina apagada do banco some da lista (e a
    // matrícula órfã é removida); disciplina excluída pelo professor (ativo = false)
    // fica escondida; e o nome exibido é sempre o atual.
    static void carregarDoAluno(FirebaseFirestore db, String alunoId, Callback callback) {

        db.collection("matriculas")
                .whereEqualTo("alunoId", alunoId)
                .get()
                .addOnSuccessListener(matriculas -> {

                    Map<String, DocumentSnapshot> matriculaPorDisciplina = new LinkedHashMap<>();

                    for (DocumentSnapshot matricula : matriculas) {

                        String disciplinaId = matricula.getString("disciplinaId");

                        if (disciplinaId != null) {
                            matriculaPorDisciplina.put(disciplinaId, matricula);
                        }
                    }

                    if (matriculaPorDisciplina.isEmpty()) {
                        callback.onCarregadas(new LinkedHashMap<>());
                        return;
                    }

                    buscarDisciplinas(db, new ArrayList<>(matriculaPorDisciplina.keySet()))
                            .addOnSuccessListener(encontradas -> {

                                LinkedHashMap<String, String> validas = new LinkedHashMap<>();
                                WriteBatch limpeza = db.batch();
                                boolean temOrfas = false;

                                for (Map.Entry<String, DocumentSnapshot> item : matriculaPorDisciplina.entrySet()) {

                                    DocumentSnapshot disciplina = encontradas.get(item.getKey());

                                    if (disciplina == null) {

                                        // Disciplina não existe mais no banco
                                        limpeza.delete(item.getValue().getReference());
                                        temOrfas = true;
                                        continue;
                                    }

                                    if (Boolean.FALSE.equals(disciplina.getBoolean("ativo"))) {
                                        continue;
                                    }

                                    String nome = disciplina.getString("nome");

                                    validas.put(item.getKey(), nome != null
                                            ? nome
                                            : item.getValue().getString("disciplinaNome"));
                                }

                                if (temOrfas) {
                                    limpeza.commit();
                                }

                                callback.onCarregadas(validas);
                            })
                            .addOnFailureListener(e -> callback.onCarregadas(new LinkedHashMap<>()));
                });
    }

    // Busca as disciplinas pelos ids, em lotes de 30 (limite do whereIn)
    private static Task<Map<String, DocumentSnapshot>> buscarDisciplinas(FirebaseFirestore db,
                                                                       List<String> ids) {

        List<Task<QuerySnapshot>> consultas = new ArrayList<>();

        for (int i = 0; i < ids.size(); i += 30) {

            consultas.add(db.collection("disciplinas")
                    .whereIn(FieldPath.documentId(), new ArrayList<>(ids.subList(i, Math.min(i + 30, ids.size()))))
                    .get());
        }

        return Tasks.whenAllSuccess(consultas).continueWith(tarefa -> {

            Map<String, DocumentSnapshot> porId = new HashMap<>();

            for (Object resultado : tarefa.getResult()) {
                for (DocumentSnapshot documento : ((QuerySnapshot) resultado).getDocuments()) {
                    porId.put(documento.getId(), documento);
                }
            }

            return porId;
        });
    }

    // Se a conta do professor foi apagada e criada de novo, o uid muda e ele perderia
    // as disciplinas, trilhas e aulas antigas. No login elas são transferidas para o
    // uid atual: pelo uid antigo salvo no cadastro e pelo e-mail gravado nas disciplinas.
    static void vincularAoProfessor(FirebaseFirestore db, String uidAtual, String email,
                                    String uidAntigoDoCadastro) {

        if (uidAtual == null) {
            return;
        }

        List<Task<QuerySnapshot>> consultas = new ArrayList<>();

        if (email != null && !email.isEmpty()) {
            consultas.add(db.collection("disciplinas").whereEqualTo("professorEmail", email).get());
        }

        if (uidAntigoDoCadastro != null && !uidAntigoDoCadastro.equals(uidAtual)) {
            consultas.add(db.collection("disciplinas").whereEqualTo("professorId", uidAntigoDoCadastro).get());
        }

        // Também grava o e-mail nas disciplinas atuais, para a próxima vez
        consultas.add(db.collection("disciplinas").whereEqualTo("professorId", uidAtual).get());

        Tasks.whenAllSuccess(consultas).addOnSuccessListener(resultados -> {

            List<String> uidsAntigos = new ArrayList<>();
            WriteBatch lote = db.batch();
            int alteracoes = 0;

            if (uidAntigoDoCadastro != null && !uidAntigoDoCadastro.equals(uidAtual)) {
                uidsAntigos.add(uidAntigoDoCadastro);
            }

            Map<String, DocumentSnapshot> disciplinas = new HashMap<>();

            for (Object resultado : resultados) {
                for (DocumentSnapshot d : ((QuerySnapshot) resultado).getDocuments()) {
                    disciplinas.put(d.getId(), d);
                }
            }

            for (DocumentSnapshot disciplina : disciplinas.values()) {

                String professorId = disciplina.getString("professorId");
                Map<String, Object> dados = new HashMap<>();

                if (!uidAtual.equals(professorId)) {

                    if (professorId != null && !uidsAntigos.contains(professorId)) {
                        uidsAntigos.add(professorId);
                    }

                    dados.put("professorId", uidAtual);
                }

                if (email != null && !email.equals(disciplina.getString("professorEmail"))) {
                    dados.put("professorEmail", email);
                }

                if (!dados.isEmpty() && alteracoes < 450) {
                    lote.update(disciplina.getReference(), dados);
                    alteracoes++;
                }
            }

            if (alteracoes > 0) {
                lote.commit();
            }

            // Trilhas e aulas do uid antigo passam para o atual
            for (String uidAntigo : uidsAntigos) {
                transferir(db, "atividades", uidAntigo, uidAtual);
                transferir(db, "aulas", uidAntigo, uidAtual);
            }
        });
    }

    private static void transferir(FirebaseFirestore db, String colecao, String uidAntigo, String uidAtual) {

        db.collection(colecao)
                .whereEqualTo("professorId", uidAntigo)
                .get()
                .addOnSuccessListener(documentos -> {

                    WriteBatch lote = db.batch();
                    int alteracoes = 0;

                    for (DocumentSnapshot documento : documentos) {

                        if (alteracoes >= 450) {
                            break;
                        }

                        lote.update(documento.getReference(), "professorId", uidAtual);
                        alteracoes++;
                    }

                    if (alteracoes > 0) {
                        lote.commit();
                    }
                });
    }
}
