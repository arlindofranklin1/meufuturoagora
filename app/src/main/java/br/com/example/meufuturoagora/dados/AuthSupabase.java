package br.com.example.meufuturoagora.dados;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.function.LongSupplier;

import okhttp3.Request;
import okhttp3.RequestBody;

// Chamadas ao Supabase Auth (/auth/v1). Endpoints conferidos no código do
// Supabase Auth: internal/api/token.go, token_oidc.go e logout.go.
public class AuthSupabase {

    private final HttpSupabase http;
    private final LongSupplier agoraEmSegundos;

    public AuthSupabase(HttpSupabase http, LongSupplier agoraEmSegundos) {
        this.http = http;
        this.agoraEmSegundos = agoraEmSegundos;
    }

    // Troca o ID token do Google por uma sessão. O nonce vai cru; ao Google foi
    // o hash dele (ver Nonce).
    public Sessao entrarComIdTokenGoogle(String idToken, String nonce) throws ErroApi {

        JSONObject corpo = new JSONObject();

        try {
            corpo.put("provider", "google");
            corpo.put("id_token", idToken);
            corpo.put("nonce", nonce);
        } catch (JSONException e) {
            throw ErroApi.inesperado(e);
        }

        return pedirToken("id_token", corpo);
    }

    public Sessao renovar(String tokenDeRenovacao) throws ErroApi {

        JSONObject corpo = new JSONObject();

        try {
            corpo.put("refresh_token", tokenDeRenovacao);
        } catch (JSONException e) {
            throw ErroApi.inesperado(e);
        }

        return pedirToken("refresh_token", corpo);
    }

    // Revoga só a sessão deste aparelho (scope=local); as de outros aparelhos continuam
    public void sair(String tokenDeAcesso) throws ErroApi {

        Request requisicao = http.requisicao("/auth/v1/logout?scope=local")
                .header("Authorization", "Bearer " + tokenDeAcesso)
                .post(RequestBody.create(new byte[0], HttpSupabase.JSON))
                .build();

        http.executar(requisicao);
    }

    private Sessao pedirToken(String tipo, JSONObject corpo) throws ErroApi {

        Request requisicao = http.requisicao("/auth/v1/token?grant_type=" + tipo)
                .post(RequestBody.create(corpo.toString(), HttpSupabase.JSON))
                .build();

        String resposta = http.executar(requisicao).texto();

        try {
            return Sessao.deJson(new JSONObject(resposta), agoraEmSegundos.getAsLong());
        } catch (JSONException e) {
            throw ErroApi.respostaInvalida(e);
        }
    }
}
