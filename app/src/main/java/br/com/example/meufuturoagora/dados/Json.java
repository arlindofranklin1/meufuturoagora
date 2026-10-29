package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

// Leitura de JSON com null de verdade: o optString do org.json devolve ""
// para null, e isso confundiria "sem foto" com "foto vazia".
final class Json {

    private Json() {
    }

    interface Conversor<T> {
        T converter(JSONObject json) throws JSONException;
    }

    static String texto(JSONObject json, String campo) {
        Object valor = json.opt(campo);
        return valor == null || valor == JSONObject.NULL ? null : String.valueOf(valor);
    }

    static Integer inteiro(JSONObject json, String campo) {
        Object valor = json.opt(campo);
        return valor instanceof Number ? ((Number) valor).intValue() : null;
    }

    static Double decimal(JSONObject json, String campo) {
        Object valor = json.opt(campo);

        if (valor instanceof Number) {
            return ((Number) valor).doubleValue();
        }

        // O PostgREST manda numeric como número; por garantia aceita texto também
        if (valor instanceof String) {
            try {
                return Double.parseDouble((String) valor);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    static boolean logico(JSONObject json, String campo) {
        return json.optBoolean(campo, false);
    }

    static LocalDate data(JSONObject json, String campo) {
        String valor = texto(json, campo);
        return valor != null ? LocalDate.parse(valor) : null;
    }

    static OffsetDateTime momento(JSONObject json, String campo) {
        String valor = texto(json, campo);
        return valor != null ? OffsetDateTime.parse(valor) : null;
    }

    static <T> List<T> lista(JSONArray linhas, Conversor<T> conversor) throws ErroApi {

        List<T> itens = new ArrayList<>();

        try {
            for (int i = 0; i < linhas.length(); i++) {
                itens.add(conversor.converter(linhas.getJSONObject(i)));
            }
        } catch (JSONException | RuntimeException e) {
            throw ErroApi.respostaInvalida(e);
        }

        return itens;
    }

    static <T> T um(JSONObject linha, Conversor<T> conversor) throws ErroApi {

        if (linha == null) {
            return null;
        }

        try {
            return conversor.converter(linha);
        } catch (JSONException | RuntimeException e) {
            throw ErroApi.respostaInvalida(e);
        }
    }

    static JSONObject objeto(Object... chavesEValores) {

        JSONObject json = new JSONObject();

        try {
            for (int i = 0; i < chavesEValores.length; i += 2) {
                Object valor = chavesEValores[i + 1];
                json.put((String) chavesEValores[i], valor != null ? valor : JSONObject.NULL);
            }
        } catch (JSONException e) {
            throw new IllegalArgumentException(e);
        }

        return json;
    }
}
