package br.com.example.meufuturoagora;

import static org.mockito.Mockito.mock;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.HashMap;
import java.util.Map;

// Documento do Firestore simulado a partir de um mapa, com as mesmas conversões
// de tipo dos getters reais (getDouble aceita Long, getString devolve null se o
// campo não for texto etc.).
final class DocumentoFalso {

    private DocumentoFalso() {
    }

    static QueryDocumentSnapshot de(String id, Map<String, Object> dados) {

        // Cópia: o documento lido não muda se o "banco" mudar depois
        Map<String, Object> copia = dados != null ? new HashMap<>(dados) : null;

        return mock(QueryDocumentSnapshot.class, invocacao -> {

            String metodo = invocacao.getMethod().getName();

            switch (metodo) {
                case "exists":
                    return copia != null;
                case "getId":
                    return id;
                case "getData":
                    return copia != null ? new HashMap<>(copia) : null;
                case "toString":
                    return "DocumentoFalso(" + id + ")";
            }

            Object valor = copia != null && invocacao.getArguments().length > 0
                    ? copia.get(invocacao.<String>getArgument(0))
                    : null;

            switch (metodo) {
                case "contains":
                    return copia != null && copia.containsKey(invocacao.<String>getArgument(0));
                case "get":
                    return valor;
                case "getString":
                    return valor instanceof String ? valor : null;
                case "getBoolean":
                    return valor instanceof Boolean ? valor : null;
                case "getDouble":
                    return valor instanceof Number ? ((Number) valor).doubleValue() : null;
                case "getLong":
                    return valor instanceof Number ? ((Number) valor).longValue() : null;
                case "getTimestamp":
                    return valor instanceof Timestamp ? valor : null;
                default:
                    throw new UnsupportedOperationException("DocumentoFalso." + metodo);
            }
        });
    }

    static QueryDocumentSnapshot inexistente(String id) {
        return de(id, null);
    }

    // mapa("nome", "Ana", "ativo", true)
    static Map<String, Object> mapa(Object... chavesEValores) {

        Map<String, Object> mapa = new HashMap<>();

        for (int i = 0; i < chavesEValores.length; i += 2) {
            mapa.put((String) chavesEValores[i], chavesEValores[i + 1]);
        }

        return mapa;
    }
}
