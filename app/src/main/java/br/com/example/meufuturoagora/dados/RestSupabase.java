package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.RequestBody;

// Tabelas e funções do banco pela API REST (/rest/v1). Quem pode ver e gravar
// o quê é decidido no servidor (RLS); aqui só se monta a requisição.
public class RestSupabase {

    private final ClienteSupabase cliente;

    public RestSupabase(ClienteSupabase cliente) {
        this.cliente = cliente;
    }

    public JSONArray selecionar(String tabela, Consulta consulta) throws ErroApi {
        return lista(cliente.executar(requisicao(tabela, consulta).get()).texto());
    }

    // Primeira linha, ou null se a consulta não trouxer nada (inclusive por RLS)
    public JSONObject selecionarUm(String tabela, Consulta consulta) throws ErroApi {
        JSONArray linhas = selecionar(tabela, consulta.limite(1));
        return linhas.length() > 0 ? linhas.optJSONObject(0) : null;
    }

    public JSONObject inserir(String tabela, JSONObject dados) throws ErroApi {

        Request.Builder requisicao = requisicao(tabela, null)
                .header("Prefer", "return=representation")
                .post(RequestBody.create(dados.toString(), HttpSupabase.JSON));

        JSONArray linhas = lista(cliente.executar(requisicao).texto());

        if (linhas.length() == 0) {
            throw ErroApi.respostaInvalida(null);
        }

        return linhas.optJSONObject(0);
    }

    // Devolve as linhas alteradas: lista vazia quer dizer que o RLS não deixou
    public JSONArray atualizar(String tabela, Consulta filtro, JSONObject dados) throws ErroApi {

        Request.Builder requisicao = requisicao(tabela, filtro)
                .header("Prefer", "return=representation")
                .patch(RequestBody.create(dados.toString(), HttpSupabase.JSON));

        return lista(cliente.executar(requisicao).texto());
    }

    public JSONArray apagar(String tabela, Consulta filtro) throws ErroApi {

        Request.Builder requisicao = requisicao(tabela, filtro)
                .header("Prefer", "return=representation")
                .delete();

        return lista(cliente.executar(requisicao).texto());
    }

    // Resposta crua da função: objeto, lista, número ou nada (void)
    public Object rpc(String funcao, JSONObject parametros) throws ErroApi {

        Request.Builder requisicao = cliente.requisicao("/rest/v1/rpc/" + funcao)
                .post(RequestBody.create(
                        (parametros != null ? parametros : new JSONObject()).toString(),
                        HttpSupabase.JSON));

        String texto = cliente.executar(requisicao).texto().trim();

        if (texto.isEmpty()) {
            return null;
        }

        try {
            Object valor = new JSONTokener(texto).nextValue();
            return valor == JSONObject.NULL ? null : valor;
        } catch (JSONException e) {
            throw ErroApi.respostaInvalida(e);
        }
    }

    public JSONArray rpcLista(String funcao, JSONObject parametros) throws ErroApi {
        Object valor = rpc(funcao, parametros);
        return valor instanceof JSONArray ? (JSONArray) valor : new JSONArray();
    }

    public JSONObject rpcObjeto(String funcao, JSONObject parametros) throws ErroApi {

        Object valor = rpc(funcao, parametros);

        if (!(valor instanceof JSONObject)) {
            throw ErroApi.respostaInvalida(null);
        }

        return (JSONObject) valor;
    }

    private Request.Builder requisicao(String tabela, Consulta consulta) {

        Request.Builder requisicao = cliente.requisicao("/rest/v1/" + tabela);

        if (consulta != null) {
            HttpUrl url = consulta.aplicar(requisicao.build().url());
            requisicao.url(url);
        }

        return requisicao;
    }

    private static JSONArray lista(String texto) throws ErroApi {

        if (texto.trim().isEmpty()) {
            return new JSONArray();
        }

        try {
            return new JSONArray(texto);
        } catch (JSONException e) {
            throw ErroApi.respostaInvalida(e);
        }
    }
}
