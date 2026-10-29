package br.com.example.meufuturoagora;

import static org.mockito.Mockito.mock;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiPredicate;

// Firestore em memória para os testes de caracterização. Responde só às
// operações que as classes testadas usam (document, whereEqualTo, whereIn, get,
// set, update, delete), com tarefas já concluídas. Qualquer outra chamada falha,
// para o teste não passar por acaso.
final class FirestoreFalso {

    final FirebaseFirestore db;

    // coleção -> id -> campos
    private final Map<String, Map<String, Map<String, Object>>> colecoes = new HashMap<>();

    FirestoreFalso() {

        db = mock(FirebaseFirestore.class, invocacao -> {

            if (invocacao.getMethod().getName().equals("collection")) {
                return consulta(CollectionReference.class, invocacao.getArgument(0), new ArrayList<>());
            }

            if (invocacao.getMethod().getName().equals("toString")) {
                return "FirestoreFalso";
            }

            throw new UnsupportedOperationException("FirebaseFirestore." + invocacao.getMethod().getName());
        });
    }

    void gravar(String colecao, String id, Map<String, Object> dados) {
        documentos(colecao).put(id, new HashMap<>(dados));
    }

    Map<String, Object> ler(String colecao, String id) {
        return documentos(colecao).get(id);
    }

    int quantidade(String colecao) {
        return documentos(colecao).size();
    }

    private Map<String, Map<String, Object>> documentos(String colecao) {
        return colecoes.computeIfAbsent(colecao, c -> new LinkedHashMap<>());
    }

    // Filtro sobre (id, campos) do documento
    private interface Filtro extends BiPredicate<String, Map<String, Object>> {
    }

    private <T extends Query> T consulta(Class<T> tipo, String colecao, List<Filtro> filtros) {

        return mock(tipo, invocacao -> {

            String metodo = invocacao.getMethod().getName();
            Object[] argumentos = invocacao.getArguments();

            switch (metodo) {

                case "document":
                    return documento(colecao, (String) argumentos[0]);

                case "whereEqualTo": {
                    String campo = (String) argumentos[0];
                    Object valor = argumentos[1];
                    return consulta(Query.class, colecao,
                            com(filtros, (id, dados) -> Objects.equals(dados.get(campo), valor)));
                }

                case "whereIn": {
                    List<?> valores = (List<?>) argumentos[1];

                    if (argumentos[0] instanceof FieldPath) {
                        return consulta(Query.class, colecao,
                                com(filtros, (id, dados) -> valores.contains(id)));
                    }

                    String campo = (String) argumentos[0];
                    return consulta(Query.class, colecao,
                            com(filtros, (id, dados) -> valores.contains(dados.get(campo))));
                }

                case "get":
                    if (argumentos.length == 0) {
                        return Tasks.forResult(resultado(colecao, filtros));
                    }
                    break;

                case "toString":
                    return "Consulta(" + colecao + ")";
            }

            throw new UnsupportedOperationException(colecao + "." + metodo);
        });
    }

    private static List<Filtro> com(List<Filtro> filtros, Filtro novo) {

        List<Filtro> lista = new ArrayList<>(filtros);
        lista.add(novo);
        return lista;
    }

    private QuerySnapshot resultado(String colecao, List<Filtro> filtros) {

        List<QueryDocumentSnapshot> encontrados = new ArrayList<>();

        for (Map.Entry<String, Map<String, Object>> item : documentos(colecao).entrySet()) {

            boolean passa = true;

            for (Filtro filtro : filtros) {
                if (!filtro.test(item.getKey(), item.getValue())) {
                    passa = false;
                    break;
                }
            }

            if (passa) {
                encontrados.add(DocumentoFalso.de(item.getKey(), item.getValue()));
            }
        }

        return mock(QuerySnapshot.class, invocacao -> {

            switch (invocacao.getMethod().getName()) {
                case "getDocuments":
                    return new ArrayList<DocumentSnapshot>(encontrados);
                case "iterator":
                    return new ArrayList<>(encontrados).iterator();
                case "isEmpty":
                    return encontrados.isEmpty();
                case "size":
                    return encontrados.size();
                case "toString":
                    return "Resultado(" + colecao + ", " + encontrados.size() + ")";
            }

            throw new UnsupportedOperationException("QuerySnapshot." + invocacao.getMethod().getName());
        });
    }

    private DocumentReference documento(String colecao, String id) {

        return mock(DocumentReference.class, invocacao -> {

            Object[] argumentos = invocacao.getArguments();

            switch (invocacao.getMethod().getName()) {

                case "getId":
                    return id;

                case "get":
                    return Tasks.forResult(DocumentoFalso.de(id, documentos(colecao).get(id)));

                case "set": {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> dados = (Map<String, Object>) argumentos[0];

                    // set(dados, SetOptions.merge()) junta; set(dados) substitui
                    if (argumentos.length > 1) {
                        documentos(colecao).computeIfAbsent(id, i -> new HashMap<>()).putAll(dados);
                    } else {
                        documentos(colecao).put(id, new HashMap<>(dados));
                    }

                    return Tasks.forResult(null);
                }

                case "update": {
                    Map<String, Object> atual = documentos(colecao).get(id);

                    // Como no Firestore real, update em documento inexistente falha
                    if (atual == null) {
                        return Tasks.forException(new IllegalStateException("NOT_FOUND " + colecao + "/" + id));
                    }

                    if (argumentos[0] instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> dados = (Map<String, Object>) argumentos[0];
                        atual.putAll(dados);
                    } else {
                        for (int i = 0; i + 1 < argumentos.length; i += 2) {
                            atual.put((String) argumentos[i], argumentos[i + 1]);
                        }
                    }

                    return Tasks.forResult(null);
                }

                case "delete":
                    documentos(colecao).remove(id);
                    return Tasks.forResult(null);

                case "toString":
                    return colecao + "/" + id;
            }

            throw new UnsupportedOperationException(
                    "DocumentReference." + invocacao.getMethod().getName());
        });
    }
}
