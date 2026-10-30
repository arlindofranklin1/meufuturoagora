package br.com.example.meufuturoagora.dados;

import org.json.JSONException;
import org.json.JSONObject;

// Sessão do Supabase Auth. O token de acesso só existe em memória; o token de
// renovação é o único guardado no aparelho (ArmazenamentoSessao).
public final class Sessao {

    final String tokenDeAcesso;
    final String tokenDeRenovacao;
    // Segundos desde 1970, como o "expires_at" do Auth
    final long expiraEm;
    public final String usuarioId;
    public final String email;

    Sessao(String tokenDeAcesso, String tokenDeRenovacao, long expiraEm, String usuarioId, String email) {
        this.tokenDeAcesso = tokenDeAcesso;
        this.tokenDeRenovacao = tokenDeRenovacao;
        this.expiraEm = expiraEm;
        this.usuarioId = usuarioId;
        this.email = email;
    }

    // Resposta de /auth/v1/token. Sem expires_at, calcula pelo expires_in.
    static Sessao deJson(JSONObject json, long agoraEmSegundos) throws JSONException {

        JSONObject usuario = json.optJSONObject("user");

        long expiraEm = json.has("expires_at")
                ? json.getLong("expires_at")
                : agoraEmSegundos + json.getLong("expires_in");

        return new Sessao(
                json.getString("access_token"),
                json.getString("refresh_token"),
                expiraEm,
                usuario != null ? usuario.optString("id", null) : null,
                usuario != null ? usuario.optString("email", null) : null
        );
    }

    // Nunca mostrar os tokens (logs, mensagens de erro, depuração)
    @Override
    public String toString() {
        return "Sessao(usuario=" + usuarioId + ", expiraEm=" + expiraEm + ")";
    }
}
