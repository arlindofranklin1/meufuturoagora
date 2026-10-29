package br.com.example.meufuturoagora.dados;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

// Arquivos no Supabase Storage (/storage/v1). Rotas conferidas no código do
// Storage: src/http/routes/object (createObject, getSignedURL, deleteObjects,
// getObject). Os buckets são privados: ler é sempre por URL assinada ou
// download autenticado.
public class StorageSupabase implements ArquivoRepositorio {

    private final ClienteSupabase cliente;

    public StorageSupabase(ClienteSupabase cliente) {
        this.cliente = cliente;
    }

    // Sem x-upsert: enviar para um caminho que já existe dá erro em vez de
    // sobrescrever (a entrega, por exemplo, não pode ser trocada)
    @Override
    public void enviar(String bucket, String caminho, byte[] conteudo, String tipoMime) throws ErroApi {

        Request.Builder requisicao = cliente.requisicao("/storage/v1/object/" + bucket + "/" + caminho)
                .post(RequestBody.create(conteudo, MediaType.get(tipoMime)));

        cliente.executar(requisicao);
    }

    @Override
    public String urlAssinada(String bucket, String caminho, int segundos) throws ErroApi {

        Request.Builder requisicao = cliente.requisicao("/storage/v1/object/sign/" + bucket + "/" + caminho)
                .post(RequestBody.create(Json.objeto("expiresIn", segundos).toString(), HttpSupabase.JSON));

        try {
            JSONObject resposta = new JSONObject(cliente.executar(requisicao).texto());
            // signedURL vem relativa a /storage/v1: "/object/sign/...?token=..."
            return cliente.http().urlBase() + "/storage/v1" + resposta.getString("signedURL");
        } catch (JSONException e) {
            throw ErroApi.respostaInvalida(e);
        }
    }

    @Override
    public byte[] baixar(String bucket, String caminho) throws ErroApi {
        return cliente.executar(
                cliente.requisicao("/storage/v1/object/authenticated/" + bucket + "/" + caminho).get()
        ).corpo;
    }

    @Override
    public void apagar(String bucket, List<String> caminhos) throws ErroApi {

        if (caminhos.isEmpty()) {
            return;
        }

        JSONObject corpo = Json.objeto("prefixes", new JSONArray(caminhos));

        cliente.executar(cliente.requisicao("/storage/v1/object/" + bucket)
                .delete(RequestBody.create(corpo.toString(), HttpSupabase.JSON)));
    }
}
