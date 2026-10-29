package br.com.example.meufuturoagora.dados;

import okhttp3.Request;

// Requisições em nome do usuário logado. Se o servidor responder 401 (token
// vencido ou revogado), renova uma vez e repete; na segunda recusa, desiste.
public class ClienteSupabase {

    private final HttpSupabase http;
    private final GerenciadorSessao sessao;

    public ClienteSupabase(HttpSupabase http, GerenciadorSessao sessao) {
        this.http = http;
        this.sessao = sessao;
    }

    HttpSupabase http() {
        return http;
    }

    Request.Builder requisicao(String caminho) {
        return http.requisicao(caminho);
    }

    HttpSupabase.Resposta executar(Request.Builder requisicao) throws ErroApi {

        String token = sessao.tokenDeAcesso();

        try {
            return http.executar(comToken(requisicao, token));

        } catch (ErroApi e) {

            if (e.codigoHttp() != 401 || token == null) {
                throw e;
            }

            sessao.renovar();
            return http.executar(comToken(requisicao, sessao.tokenDeAcesso()));
        }
    }

    // A chave publicável (sb_publishable_...) não é um JWT: sem login, vai só no
    // cabeçalho apikey, e o Supabase trata a requisição como anon
    private static Request comToken(Request.Builder requisicao, String token) {

        if (token == null) {
            return requisicao.removeHeader("Authorization").build();
        }

        return requisicao.header("Authorization", "Bearer " + token).build();
    }
}
