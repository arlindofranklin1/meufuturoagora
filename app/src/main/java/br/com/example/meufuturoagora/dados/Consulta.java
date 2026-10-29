package br.com.example.meufuturoagora.dados;

import java.util.ArrayList;
import java.util.List;

import okhttp3.HttpUrl;

// Filtros do PostgREST (?campo=eq.valor&order=...). Os valores são
// codificados pela HttpUrl: um nome com "&" ou "," não quebra a consulta.
public final class Consulta {

    private final List<String[]> parametros = new ArrayList<>();

    public static Consulta campos(String selecao) {
        return new Consulta().com("select", selecao);
    }

    public Consulta igual(String campo, Object valor) {
        return com(campo, "eq." + valor);
    }

    public Consulta maiorQue(String campo, Object valor) {
        return com(campo, "gt." + valor);
    }

    public Consulta em(String campo, List<String> valores) {
        return com(campo, "in.(" + String.join(",", valores) + ")");
    }

    public Consulta ordenar(String ordem) {
        return com("order", ordem);
    }

    public Consulta limite(int quantidade) {
        return com("limit", String.valueOf(quantidade));
    }

    Consulta com(String nome, String valor) {
        parametros.add(new String[]{nome, valor});
        return this;
    }

    HttpUrl aplicar(HttpUrl url) {

        HttpUrl.Builder construtor = url.newBuilder();

        for (String[] parametro : parametros) {
            construtor.addQueryParameter(parametro[0], parametro[1]);
        }

        return construtor.build();
    }
}
