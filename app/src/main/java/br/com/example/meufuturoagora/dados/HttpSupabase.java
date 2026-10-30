package br.com.example.meufuturoagora.dados;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

// HTTP básico do Supabase: endereço do projeto, chave pública e tradução de
// erros. Não sabe nada de sessão (isso é do ClienteSupabase), para o Auth poder
// usá-lo antes de existir um login.
public class HttpSupabase {

    static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    private final OkHttpClient okHttp;
    private final String urlBase;
    private final String chavePublica;

    public HttpSupabase(OkHttpClient okHttp, String urlBase, String chavePublica) {
        this.okHttp = okHttp;
        this.urlBase = urlBase.endsWith("/") ? urlBase.substring(0, urlBase.length() - 1) : urlBase;
        this.chavePublica = chavePublica;
    }

    public String urlBase() {
        return urlBase;
    }

    // Toda requisição leva a chave pública; o token do usuário entra depois
    Request.Builder requisicao(String caminho) {
        return new Request.Builder()
                .url(urlBase + caminho)
                .header("apikey", chavePublica);
    }

    Resposta executar(Request requisicao) throws ErroApi {

        try (Response resposta = okHttp.newCall(requisicao).execute()) {

            ResponseBody corpo = resposta.body();
            byte[] bytes = corpo != null ? corpo.bytes() : new byte[0];

            if (!resposta.isSuccessful()) {
                throw ErroApi.daResposta(resposta.code(), new String(bytes, StandardCharsets.UTF_8));
            }

            return new Resposta(resposta.code(), bytes);

        } catch (IOException e) {
            throw ErroApi.semConexao(e);
        }
    }

    static final class Resposta {

        final int codigoHttp;
        final byte[] corpo;

        Resposta(int codigoHttp, byte[] corpo) {
            this.codigoHttp = codigoHttp;
            this.corpo = corpo;
        }

        String texto() {
            return new String(corpo, StandardCharsets.UTF_8);
        }
    }
}
