package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.work.ListenableWorker;
import androidx.work.testing.TestWorkerBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.concurrent.Executors;

import br.com.example.meufuturoagora.dados.Novidade;
import br.com.example.meufuturoagora.dados.Servicos;

// Módulo 8 da Fase 5: o NotificacaoWorker pergunta ao servidor o que mudou desde
// o último marco e grava a lista do sino. Quem pode ver o quê é do servidor.
@RunWith(AndroidJUnit4.class)
public class NotificacoesTest {

    private Context contexto;
    private RepositoriosFalsos falsos;

    @Before
    public void preparar() {
        contexto = InstrumentationRegistry.getInstrumentation().getTargetContext();

        falsos = new RepositoriosFalsos();
        falsos.auth.perfil = AuthFalso.perfil("aluno", false);
        falsos.instalar();

        // Sem aviso na barra do celular durante o teste; só a lista do sino
        NotificacaoUtil.preferencias(contexto).edit()
                .clear()
                .putBoolean(NotificacaoUtil.CHAVE_NOTIFICACOES, false)
                .apply();
        NotificacaoHistorico.limpar(contexto);
    }

    @After
    public void encerrar() {
        NotificacaoHistorico.limpar(contexto);
        NotificacaoUtil.preferencias(contexto).edit().clear().apply();
        Servicos.substituir(null);
    }

    private ListenableWorker.Result rodar() {
        return TestWorkerBuilder.from(contexto, NotificacaoWorker.class, Executors.newSingleThreadExecutor())
                .build()
                .doWork();
    }

    private static Novidade novidade(String tipo, String nome, long segundosDepois) {
        return new Novidade(tipo, "i-" + nome, nome, "d1", "Matemática", "a1", "Ana",
                OffsetDateTime.now().plusSeconds(segundosDepois));
    }

    @Test
    public void semSessaoNaoFazNada() {
        falsos.auth.perfil = null;

        assertEquals(ListenableWorker.Result.success(), rodar());

        assertTrue(falsos.chamadas.isEmpty());
    }

    @Test
    public void primeiraVezSoMarcaOPontoDePartidaSemAvisarOPassado() {
        falsos.novidades.add(novidade(Novidade.ATIVIDADE, "Antiga", -3600));

        assertEquals(ListenableWorker.Result.success(), rodar());

        assertTrue(NotificacaoHistorico.listar(contexto).isEmpty());
        assertFalse("nem consultou o servidor", falsos.chamadas.toString().contains("novidades.desde"));
    }

    @Test
    public void depoisDoMarcoAsNovidadesViramItensDoSinoUmaSoVez() throws Exception {
        rodar();
        Thread.sleep(50);

        falsos.novidades.add(novidade(Novidade.ATIVIDADE, "Frações", 5));
        falsos.novidades.add(novidade(Novidade.AVALIACAO, "Geometria", 6));

        assertEquals(ListenableWorker.Result.success(), rodar());

        List<NotificacaoHistorico.Item> itens = NotificacaoHistorico.listar(contexto);
        assertEquals(2, itens.size());
        assertEquals("Atividade avaliada em Matemática", itens.get(0).titulo);
        assertEquals("Nova atividade em Matemática", itens.get(1).titulo);

        // O marco avançou para a mais nova: a segunda rodada não repete nada
        assertEquals(ListenableWorker.Result.success(), rodar());
        assertEquals(2, NotificacaoHistorico.listar(contexto).size());
    }

    @Test
    public void cadaUsuarioTemOProprioMarcoEAPropriaLista() throws Exception {
        rodar();
        falsos.novidades.add(novidade(Novidade.DISCIPLINA, "Física", 5));
        rodar();
        assertEquals(1, NotificacaoHistorico.listar(contexto).size());

        // Outra conta no mesmo celular: lista própria, vazia
        falsos.auth.perfil = new br.com.example.meufuturoagora.dados.Perfil("p2", "u2", "outro@escola.test",
                "Outro", "aluno", false, true, "t1", null, null);

        assertTrue(NotificacaoHistorico.listar(contexto).isEmpty());
    }

    @Test
    public void erroDeRedeTentaDeNovoEASessaoExpiradaNao() {
        rodar();

        falsos.erroNasNovidades = RepositoriosFalsos.erro(503, "XX000", "fora do ar");
        assertEquals(ListenableWorker.Result.retry(), rodar());

        falsos.erroNasNovidades = RepositoriosFalsos.erro(401, "PGRST301", "JWT expired");
        assertEquals(ListenableWorker.Result.success(), rodar());
    }

    @Test
    public void sinoMostraAsNotificacoesNaListaDaTelaInicial() throws Exception {
        rodar();
        falsos.novidades.add(novidade(Novidade.ATIVIDADE, "Frações", 5));
        rodar();

        assertEquals(1, NotificacaoHistorico.listar(contexto).size());
        assertTrue(NotificacaoHistorico.temNaoLidas(contexto));
        NotificacaoHistorico.marcarTodasComoLidas(contexto);
        assertFalse(NotificacaoHistorico.temNaoLidas(contexto));
    }

    @Test
    public void tocarNaNotificacaoDeEntregaAbreAAvaliacaoPeloIdDaEntrega() {
        Novidade entrega = new Novidade(Novidade.ENTREGA, "e1", "Frações", "d1", "Matemática", "a1", "Ana",
                OffsetDateTime.now());

        NotificacaoHistorico.Item item = NotificacaoWorker.montarItem(entrega);
        Intent destino = item.destino(contexto);

        assertEquals(AvaliarEntregaActivity.class.getName(), destino.getComponent().getClassName());
        assertEquals("a1", destino.getStringExtra("atividadeId"));
        assertEquals("e1", destino.getStringExtra("entregaId"));
    }

    @Test
    public void avaliacaoPelaNotificacaoEncontraAEntregaSemSaberOAluno() {
        falsos.eu = RepositoriosFalsos.perfil("p1", "Paula", "professor", false, null);
        falsos.entregas.add(RepositoriosFalsos.entrega("e1", "a1", "s1", null, false));

        ActivityScenario.launch(new Intent(contexto, AvaliarEntregaActivity.class)
                .putExtra("atividadeId", "a1").putExtra("entregaId", "e1").putExtra("alunoNome", "Ana"));

        Espera.ate(() -> onView(withText("ok")).check(matches(isDisplayed())));
        onView(withId(R.id.btnAvaliar)).check(matches(isDisplayed()));
    }
}
