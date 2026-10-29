package br.com.example.meufuturoagora.dados;

import android.content.Context;

import java.util.concurrent.TimeUnit;

import br.com.example.meufuturoagora.BuildConfig;
import okhttp3.OkHttpClient;

// Localizador simples dos repositórios: as telas pedem daqui, e os testes
// instrumentados trocam por versões falsas com substituir(). Sem Hilt/Dagger
// de propósito (ver o plano, Seção 4.1).
public final class Servicos {

    private static Servicos instancia;

    public final AuthRepositorio auth;
    public final PerfilRepositorio perfis;
    public final EscolaRepositorio escola;
    public final DisciplinaRepositorio disciplinas;
    public final AtividadeRepositorio atividades;
    public final FrequenciaRepositorio frequencias;
    public final RankingRepositorio ranking;
    public final DiarioRepositorio diario;
    public final NovidadeRepositorio novidades;
    public final ArquivoRepositorio arquivos;

    public Servicos(AuthRepositorio auth, PerfilRepositorio perfis, EscolaRepositorio escola,
                    DisciplinaRepositorio disciplinas, AtividadeRepositorio atividades,
                    FrequenciaRepositorio frequencias, RankingRepositorio ranking,
                    DiarioRepositorio diario, NovidadeRepositorio novidades,
                    ArquivoRepositorio arquivos) {
        this.auth = auth;
        this.perfis = perfis;
        this.escola = escola;
        this.disciplinas = disciplinas;
        this.atividades = atividades;
        this.frequencias = frequencias;
        this.ranking = ranking;
        this.diario = diario;
        this.novidades = novidades;
        this.arquivos = arquivos;
    }

    public static synchronized Servicos get(Context context) {

        if (instancia == null) {
            instancia = criar(context.getApplicationContext());
        }

        return instancia;
    }

    // Testes instrumentados: repositórios falsos, sem rede
    public static synchronized void substituir(Servicos falsos) {
        instancia = falsos;
    }

    private static Servicos criar(Context context) {

        OkHttpClient okHttp = new OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        HttpSupabase http = new HttpSupabase(okHttp, BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_CHAVE_PUBLICA);
        AuthSupabase authSupabase = new AuthSupabase(http, Servicos::agoraEmSegundos);

        GerenciadorSessao sessao = new GerenciadorSessao(
                authSupabase,
                new ArmazenamentoSessaoPreferencias(
                        context.getSharedPreferences("sessao_supabase", Context.MODE_PRIVATE),
                        new CifraKeystore()),
                Servicos::agoraEmSegundos);

        ClienteSupabase cliente = new ClienteSupabase(http, sessao);
        RestSupabase rest = new RestSupabase(cliente);

        return new Servicos(
                new AuthRepositorioSupabase(authSupabase, sessao, rest),
                new PerfilRepositorioSupabase(rest, sessao),
                new EscolaRepositorioSupabase(rest),
                new DisciplinaRepositorioSupabase(rest),
                new AtividadeRepositorioSupabase(rest, sessao),
                new FrequenciaRepositorioSupabase(rest),
                new RankingRepositorioSupabase(rest),
                new DiarioRepositorioSupabase(rest),
                new NovidadeRepositorioSupabase(rest),
                new StorageSupabase(cliente)
        );
    }

    private static long agoraEmSegundos() {
        return System.currentTimeMillis() / 1000;
    }
}
