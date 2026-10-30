package br.com.example.meufuturoagora.dados;

import android.os.Bundle;

import androidx.test.platform.app.InstrumentationRegistry;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;

// A camada de dados de verdade, apontando para o Supabase LOCAL do computador
// (http://10.0.2.2:54321 visto do emulador), para os testes de ponta a ponta.
//
// Só existe no código de teste. As contas são as fictícias do supabase/seed.sql
// e entram com e-mail e senha, coisa que o app de verdade não oferece (lá é só
// Google). A chave pública local vem como argumento do teste:
//   -Pandroid.testInstrumentationRunnerArguments.supabaseChaveLocal=sb_publishable_...
// Sem ela, os testes de ponta a ponta são pulados.
public final class SupabaseLocal {

    public static final String SENHA = "senha-local-de-teste";
    private static final String URL = "http://10.0.2.2:54321";

    public final Servicos servicos;
    private final HttpSupabase http;
    private final GerenciadorSessao sessao;
    private final AuthSupabase auth;

    private SupabaseLocal(String chave) {

        OkHttpClient okHttp = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(20, TimeUnit.SECONDS)
                .build();

        http = new HttpSupabase(okHttp, URL, chave);
        auth = new AuthSupabase(http, () -> System.currentTimeMillis() / 1000);

        // Sessão só em memória: os testes não mexem no armazenamento do app
        sessao = new GerenciadorSessao(auth, new ArmazenamentoSessao() {
            private String salvo;

            @Override
            public void salvar(String tokenDeRenovacao) {
                salvo = tokenDeRenovacao;
            }

            @Override
            public String ler() {
                return salvo;
            }

            @Override
            public void apagar() {
                salvo = null;
            }
        }, () -> System.currentTimeMillis() / 1000);

        ClienteSupabase cliente = new ClienteSupabase(http, sessao);
        RestSupabase rest = new RestSupabase(cliente);

        servicos = new Servicos(
                new AuthRepositorioSupabase(auth, sessao, rest),
                new PerfilRepositorioSupabase(rest, sessao),
                new EscolaRepositorioSupabase(rest),
                new DisciplinaRepositorioSupabase(rest),
                new AtividadeRepositorioSupabase(rest, sessao),
                new FrequenciaRepositorioSupabase(rest),
                new RankingRepositorioSupabase(rest),
                new DiarioRepositorioSupabase(rest),
                new NovidadeRepositorioSupabase(rest),
                new StorageSupabase(cliente),
                new HistoricoRepositorioSupabase(rest)
        );
    }

    public static String chave() {
        Bundle argumentos = InstrumentationRegistry.getArguments();
        return argumentos.getString("supabaseChaveLocal");
    }

    public static boolean disponivel() {
        return chave() != null;
    }

    // Monta e instala no Servicos, para as telas usarem o banco local
    public static SupabaseLocal instalar() {
        SupabaseLocal local = new SupabaseLocal(chave());
        Servicos.substituir(local.servicos);
        return local;
    }

    // Entra com uma conta fictícia e liga ao cadastro (vincular_conta), como o
    // login com Google faria. Devolve o perfil, ou lança ErroApi se recusado.
    public Perfil entrar(String email) throws ErroApi {

        sessao.sair();

        JSONObject corpo = new JSONObject();

        try {
            corpo.put("email", email);
            corpo.put("password", SENHA);
        } catch (JSONException e) {
            throw ErroApi.inesperado(e);
        }

        Request requisicao = http.requisicao("/auth/v1/token?grant_type=password")
                .post(RequestBody.create(corpo.toString(), HttpSupabase.JSON))
                .build();

        try {
            sessao.iniciar(Sessao.deJson(new JSONObject(http.executar(requisicao).texto()),
                    System.currentTimeMillis() / 1000));
        } catch (JSONException e) {
            throw ErroApi.respostaInvalida(e);
        }

        return servicos.auth.restaurarSessao();
    }

    public void sair() {
        sessao.sair();
    }
}
