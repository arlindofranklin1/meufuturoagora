package br.com.example.meufuturoagora.dados;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import mockwebserver3.MockResponse;
import mockwebserver3.MockWebServer;
import mockwebserver3.RecordedRequest;
import okhttp3.OkHttpClient;

// Supabase de mentira (MockWebServer) com toda a camada de dados montada em
// cima, como em Servicos.criar, mas com relógio e armazenamento controlados.
final class ServidorFalso implements Closeable {

    static final String CHAVE_PUBLICA = "sb_publishable_teste";
    static final String TOKEN = "token-de-acesso-1";
    static final String TOKEN_RENOVACAO = "token-de-renovacao-1";
    static final String USUARIO_ID = "00000000-0000-4000-8000-0000000000aa";

    final MockWebServer servidor = new MockWebServer();
    final AtomicLong agora = new AtomicLong(1_800_000_000L);
    final ArmazenamentoMemoria armazenamento = new ArmazenamentoMemoria();

    final HttpSupabase http;
    final AuthSupabase auth;
    final GerenciadorSessao sessao;
    final ClienteSupabase cliente;
    final RestSupabase rest;

    ServidorFalso() throws IOException {

        servidor.start();

        OkHttpClient okHttp = new OkHttpClient.Builder()
                .readTimeout(500, TimeUnit.MILLISECONDS)
                .build();

        http = new HttpSupabase(okHttp, servidor.url("/").toString(), CHAVE_PUBLICA);
        auth = new AuthSupabase(http, agora::get);
        sessao = new GerenciadorSessao(auth, armazenamento, agora::get);
        cliente = new ClienteSupabase(http, sessao);
        rest = new RestSupabase(cliente);
    }

    void logar() {
        sessao.iniciar(new Sessao(TOKEN, TOKEN_RENOVACAO, agora.get() + 3600, USUARIO_ID, "ana@escola.test"));
    }

    void responder(int codigo, String corpo) {
        servidor.enqueue(new MockResponse.Builder()
                .code(codigo)
                .addHeader("Content-Type", "application/json")
                .body(corpo)
                .build());
    }

    void responder(String corpo) {
        responder(200, corpo);
    }

    // Resposta de /auth/v1/token
    void responderSessao(String token, String renovacao, long expiraEm) {
        responder(200, "{\"access_token\":\"" + token + "\",\"refresh_token\":\"" + renovacao
                + "\",\"expires_at\":" + expiraEm + ",\"expires_in\":3600,\"token_type\":\"bearer\","
                + "\"user\":{\"id\":\"" + USUARIO_ID + "\",\"email\":\"ana@escola.test\"}}");
    }

    RecordedRequest proxima() throws InterruptedException {
        RecordedRequest requisicao = servidor.takeRequest(2, TimeUnit.SECONDS);

        if (requisicao == null) {
            throw new AssertionError("O servidor não recebeu a requisição esperada");
        }

        return requisicao;
    }

    interface Acao {
        void executar() throws Exception;
    }

    // Executa e devolve o ErroApi que a ação precisa lançar
    static ErroApi erroDe(Acao acao) {

        try {
            acao.executar();
        } catch (ErroApi e) {
            return e;
        } catch (Exception e) {
            throw new AssertionError("Era esperado um ErroApi", e);
        }

        throw new AssertionError("Era esperado um ErroApi, mas a ação terminou sem erro");
    }

    int requisicoes() {
        return servidor.getRequestCount();
    }

    @Override
    public void close() throws IOException {
        servidor.close();
    }
}
