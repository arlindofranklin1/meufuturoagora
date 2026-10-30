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
}
