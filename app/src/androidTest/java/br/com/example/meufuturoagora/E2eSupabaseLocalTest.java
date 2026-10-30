package br.com.example.meufuturoagora;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.RootMatchers.isPlatformPopup;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withEffectiveVisibility;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withSpinnerText;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.widget.EditText;

import androidx.test.core.app.ActivityScenario;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.SupabaseLocal;

// Ponta a ponta: telas de verdade, camada de dados de verdade e o banco LOCAL
// de verdade (RLS, gatilhos e funções), com as contas fictícias do seed.sql.
// Precisa do Supabase local ligado e do banco recém-resetado
// (supabase db reset), porque alguns testes cadastram e excluem usuários.
// Sem a chave local nos argumentos, os testes são pulados.
@RunWith(AndroidJUnit4.class)
public class E2eSupabaseLocalTest {

    private static final String ADMIN = "coordenacao@exemplo.test";
    private static final String MARTA = "professora.marta@exemplo.test";
    private static final String JOAO = "professor.joao@exemplo.test";
    private static final String LIA = "aluna.lia@exemplo.test";
    private static final String RUI = "aluno.rui@exemplo.test";
    private static final String NOVO = "aluno.novo@exemplo.test";

    private SupabaseLocal local;

    @Before
    public void preparar() {
        Assume.assumeTrue("Supabase local não informado", SupabaseLocal.disponivel());
        local = SupabaseLocal.instalar();
    }

    @After
    public void encerrar() {
        if (local != null) {
            local.sair();
        }
        Servicos.substituir(null);
    }

    @Test
    public void professoraVeOProprioPerfilComAsDisciplinasDela() throws Exception {
        Perfil marta = local.entrar(MARTA);
        assertTrue(marta.ehProfessor());

        ActivityScenario.launch(TelaPerfilProfessor.class);

        Espera.ate(() -> onView(withId(R.id.tvNomePerfil)).check(matches(withText("Marta Fictícia"))));
        // Seed: Marta dá Lógica de Programação (1º ano) e Banco de Dados (2º ano)
        Espera.ate(() -> onView(withId(R.id.tvQtdDisciplinasPerfilProfessor)).check(matches(withText("2"))));
        onView(withId(R.id.btnAreaAdministrador))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
        Captura.salvar("01_perfil_professora");
    }

    @Test
    public void alunaVeOProprioPerfilComATurma() throws Exception {
        local.entrar(LIA);

        ActivityScenario.launch(TelaPerfilAluno.class);

        Espera.ate(() -> onView(withId(R.id.tvNomePerfilAluno)).check(matches(withText("Lia Fictícia"))));
        Espera.ate(() -> onView(withId(R.id.tvTurmaContaPerfilAluno)).check(matches(withText("1º ano Informática"))));
        // Matrícula automática (D9): as duas disciplinas do 1º ano
        onView(withId(R.id.tvQtdDisciplinasPerfil)).check(matches(withText("2")));
        onView(withId(R.id.tvPontuacaoTotalAluno)).check(matches(withText("0")));
        Captura.salvar("02_perfil_aluna");
    }

    @Test
    public void adminVeAAreaDoAdministradorComOAnoLetivo() throws Exception {
        local.entrar(ADMIN);

        ActivityScenario.launch(TelaPerfilProfessor.class);
        Espera.ate(() -> onView(withId(R.id.btnAreaAdministrador)).check(matches(isDisplayed())));
        Captura.salvar("03_perfil_admin");

        ActivityScenario.launch(TelaAdminHome.class);
        Espera.ate(() -> onView(withId(R.id.tvAnoLetivoAdmin)).check(matches(withText("Ano letivo 2026"))));
        Captura.salvar("04_area_admin");
    }

    @Test
    public void rlsAlunaNaoEnxergaOutrosAlunos() throws Exception {
        Perfil lia = local.entrar(LIA);

        List<Perfil> alunos = local.servicos.perfis.listar(Perfil.ALUNO);

        assertEquals("a aluna só vê o próprio perfil", 1, alunos.size());
        assertEquals(lia.id, alunos.get(0).id);

        try {
            local.servicos.escola.criarTurma("Turma da Lia", 9);
            fail("Aluna não pode criar turma");
        } catch (ErroApi esperado) {
            assertTrue(esperado.semPermissao());
        }
    }

    @Test
    public void adminCadastraAlunoQueEntraJaMatriculado() throws Exception {
        local.entrar(ADMIN);

        ActivityScenario.launch(CadastrarUsuarioActivity.class);

        onView(withId(R.id.radioAluno)).perform(click());
        Espera.ate(() -> onView(withId(R.id.spinnerTurma)).check(matches(withSpinnerText("1º ano Informática"))));
        onView(withId(R.id.edtNomeUsuario)).perform(replaceText("Aluno Novo"));
        onView(withId(R.id.edtEmailUsuario)).perform(replaceText(NOVO), closeSoftKeyboard());
        Captura.salvar("05_cadastro_aluno");
        onView(withId(R.id.btnSalvarUsuario)).perform(click());

        Espera.ate(() -> {
            try {
                assertNotNull(local.servicos.perfis.buscarPorEmail(NOVO));
            } catch (ErroApi e) {
                throw new AssertionError(e);
            }
        });

        Perfil novo = local.entrar(NOVO);

        assertTrue(novo.ehAluno());
        assertEquals("matriculado nas 2 disciplinas da turma (D9)", 2, local.servicos.disciplinas.listar().size());
    }

    @Test
    public void alunoPedeExclusaoEAdminAprova() throws Exception {
        local.entrar(RUI);

        ActivityScenario.launch(ConfiguracoesActivity.class);
        onView(withId(R.id.itemExcluirDados)).perform(click());
        onView(Matchers.instanceOf(EditText.class)).perform(replaceText("Mudei de escola"), closeSoftKeyboard());
        Captura.salvar("06_pedido_exclusao");
        onView(withText("Enviar pedido")).perform(click());

        Espera.ate(() -> {
            try {
                assertEquals(1, local.servicos.perfis.solicitacoes().size());
            } catch (ErroApi e) {
                throw new AssertionError(e);
            }
        });

        local.entrar(ADMIN);

        ActivityScenario.launch(ListaUsuariosActivity.class);
        Espera.ate(() -> onView(withText("Pedidos de exclusão (1)")).perform(click()));
        Captura.salvar("07_lista_com_pedido");
        onView(withId(R.id.btnMenuPessoa)).perform(click());
        onView(withText("Aprovar exclusão")).inRoot(isPlatformPopup()).perform(click());
        onView(withText("Excluir")).perform(click());

        Espera.ate(() -> {
            try {
                assertEquals(null, local.servicos.perfis.buscarPorEmail(RUI));
            } catch (ErroApi e) {
                throw new AssertionError(e);
            }
        });
        Captura.salvar("08_lista_depois_da_exclusao");

        // A conta de login também saiu: o Rui não entra mais
        try {
            local.entrar(RUI);
            fail("A conta excluída não pode entrar");
        } catch (ErroApi esperado) {
            // ok
        }
    }

    // Muda o ano atual para 2027 e devolve 2026 no fim: os outros testes deste
    // arquivo contam com 2026, e a ordem de execução não é garantida
    @Test
    public void adminTrocaOAnoLetivoSemSenhaEDefineOsBimestresDoAnoNovo() throws Exception {
        local.entrar(ADMIN);

        try {
            ActivityScenario.launch(DefinirAnoLetivoActivity.class);
            Espera.ate(() -> onView(withId(R.id.edtAnoLetivo)).check(matches(withText("2026"))));
            onView(withId(R.id.edtAnoLetivo)).perform(replaceText("2027"), closeSoftKeyboard());
            onView(withId(R.id.btnSalvarAnoLetivo)).perform(click());
            Captura.salvar("32_confirmar_ano_letivo");
            onView(withText("Confirmar")).perform(click());

            Espera.ate(() -> {
                try {
                    assertEquals(2027, local.servicos.escola.anoAtual().ano);
                } catch (ErroApi e) {
                    throw new AssertionError(e);
                }
            });

            assertEquals("o ano novo começa sem bimestres", 0, local.servicos.escola.bimestres(2027).size());

            local.servicos.escola.definirBimestres(2027, java.util.Arrays.asList(
                    new br.com.example.meufuturoagora.dados.Bimestre(2027, 1,
                            java.time.LocalDate.of(2027, 2, 1), java.time.LocalDate.of(2027, 4, 30)),
                    new br.com.example.meufuturoagora.dados.Bimestre(2027, 2,
                            java.time.LocalDate.of(2027, 5, 3), java.time.LocalDate.of(2027, 7, 9))));
            assertEquals(2, local.servicos.escola.bimestres(2027).size());

            try {
                local.servicos.escola.definirBimestres(2027, java.util.Arrays.asList(
                        new br.com.example.meufuturoagora.dados.Bimestre(2027, 1,
                                java.time.LocalDate.of(2027, 2, 1), java.time.LocalDate.of(2027, 5, 10)),
                        new br.com.example.meufuturoagora.dados.Bimestre(2027, 2,
                                java.time.LocalDate.of(2027, 5, 3), java.time.LocalDate.of(2027, 7, 9))));
                fail("O servidor devia recusar bimestres sobrepostos");
            } catch (ErroApi esperado) {
                assertEquals("Os bimestres precisam estar em ordem e sem sobreposição.", esperado.getMessage());
            }

        } finally {
            local.servicos.escola.definirAnoLetivo(2026);
        }
    }

    @Test
    public void historicoDoAdminAbreEProfessorNaoPodeConsultar() throws Exception {
        local.entrar(MARTA);

        try {
            local.servicos.historico.anos();
            fail("Professor não pode consultar o histórico");
        } catch (ErroApi esperado) {
            assertTrue(esperado.semPermissao());
        }

        local.entrar(ADMIN);

        ActivityScenario.launch(HistoricoAnoLetivoActivity.class);

        Espera.ate(() -> onView(withId(R.id.tvResumoHistorico))
                .check(matches(withText("Nenhum registro em 2026 — ano inteiro"))));
        Captura.salvar("33_historico_vazio");
    }

    // A disciplina criada é desativada no fim (como o professor faria): os outros
    // testes contam as disciplinas da Marta e da Lia, que só enxergam as ativas
    @Test
    public void professoraCriaDisciplinaEOsAlunosDaTurmaSaoMatriculados() throws Exception {
        local.entrar(MARTA);

        try {
            ActivityScenario.launch(CriarDisciplinaActivity.class);

            Espera.ate(() -> onView(withId(R.id.spinnerTurmaDisciplina))
                    .check(matches(withSpinnerText("1º ano Informática"))));
            onView(withId(R.id.edtNomeDisciplina)).perform(replaceText("Física E2E"), closeSoftKeyboard());
            Captura.salvar("34_criar_disciplina");
            onView(withId(R.id.btnSalvarDisciplina)).perform(click());

            Espera.ate(() -> {
                try {
                    assertTrue(contem(local.servicos.disciplinas.listar(), "Física E2E"));
                } catch (ErroApi e) {
                    throw new AssertionError(e);
                }
            });

            // Entrada da turma no banco: a Lia (1º ano) já enxerga a disciplina nova
            local.entrar(LIA);
            assertTrue("matrícula automática (D9)", contem(local.servicos.disciplinas.listar(), "Física E2E"));

        } finally {

            local.entrar(MARTA);

            for (br.com.example.meufuturoagora.dados.Disciplina d : local.servicos.disciplinas.listar()) {
                if ("Física E2E".equals(d.nome)) {
                    local.servicos.disciplinas.desativar(d.id);
                }
            }
        }
    }

    private static boolean contem(List<br.com.example.meufuturoagora.dados.Disciplina> lista, String nome) {

        for (br.com.example.meufuturoagora.dados.Disciplina d : lista) {
            if (nome.equals(d.nome)) {
                return true;
            }
        }

        return false;
    }

    // Lógica de Programação, do seed.sql: da Marta, 1º ano (Lia e Rui)
    private static final String LOGICA = "00000000-0000-4000-8000-000000000d01";

    private String idDaLia() throws ErroApi {

        for (br.com.example.meufuturoagora.dados.Participante p : local.servicos.disciplinas.participantes(LOGICA)) {
            if ("Lia Fictícia".equals(p.nome)) {
                return p.perfilId;
            }
        }

        throw new AssertionError("A Lia devia estar na disciplina");
    }

    // A falta é registrada pela tela e o desconto aparece na pontuação da aluna.
    // O final volta a Lia para "presente": os outros testes contam com 0 ponto.
    @Test
    public void professoraRegistraFaltaPelaTelaEAPontuacaoDaAlunaCai() throws Exception {
        local.entrar(MARTA);
        String lia = idDaLia();
        br.com.example.meufuturoagora.dados.Aula aula = local.servicos.frequencias.aulas(LOGICA).get(0);

        try {
            ActivityScenario.launch(new android.content.Intent(
                    androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().getTargetContext(),
                    FrequenciaAulaActivity.class)
                    .putExtra("aulaId", aula.id)
                    .putExtra("disciplinaId", LOGICA)
                    .putExtra("aulaDescricao", "Aula de teste"));

            Espera.ate(() -> onView(withText("Lia Fictícia")).check(matches(isDisplayed())));
            ViewInteractionDeStatus.de("Lia Fictícia").perform(click());
            onView(withText("Falta")).inRoot(isPlatformPopup()).perform(click());
            Captura.salvar("41_frequencia_falta");
            onView(withId(R.id.btnSalvarFrequencia)).perform(click());

            Espera.ate(() -> {
                try {
                    boolean achou = false;
                    for (br.com.example.meufuturoagora.dados.Frequencia f : local.servicos.frequencias.frequenciaDaAula(aula.id)) {
                        achou |= f.alunoId.equals(lia) && "falta".equals(f.status);
                    }
                    assertTrue(achou);
                } catch (ErroApi e) {
                    throw new AssertionError(e);
                }
            });

            local.entrar(LIA);
            assertEquals("uma falta = -5 pontos", -5, local.servicos.ranking.pontuacao(lia, null));

        } finally {

            local.entrar(MARTA);
            local.servicos.frequencias.registrar(aula.id, java.util.Collections.singletonList(
                    new br.com.example.meufuturoagora.dados.Frequencia(aula.id, lia,
                            br.com.example.meufuturoagora.dados.Frequencia.PRESENTE)));
        }
    }

    @Test
    public void excluirAulaDevolveOsPontosDaFaltaESoOProfessorDaDisciplinaExclui() throws Exception {
        local.entrar(MARTA);
        String lia = idDaLia();

        br.com.example.meufuturoagora.dados.Aula aula = local.servicos.frequencias.cadastrarAula(
                LOGICA, java.time.LocalDate.now(), 1, "E2E: aula para excluir");
        boolean excluida = false;

        try {
            local.servicos.frequencias.registrar(aula.id, java.util.Collections.singletonList(
                    new br.com.example.meufuturoagora.dados.Frequencia(aula.id, lia,
                            br.com.example.meufuturoagora.dados.Frequencia.FALTA)));

            local.entrar(LIA);
            assertEquals(-5, local.servicos.ranking.pontuacao(lia, null));

            // O João dá outra disciplina: não exclui a aula da Marta
            local.entrar(JOAO);
            try {
                local.servicos.frequencias.excluirAula(aula.id);
                fail("Outro professor não pode excluir a aula");
            } catch (ErroApi esperado) {
                assertTrue(esperado.semPermissao());
            }

            local.entrar(MARTA);
            local.servicos.frequencias.excluirAula(aula.id);
            excluida = true;

            local.entrar(LIA);
            assertEquals("sem a aula, o desconto some", 0, local.servicos.ranking.pontuacao(lia, null));

        } finally {

            if (!excluida) {
                local.entrar(MARTA);
                local.servicos.frequencias.excluirAula(aula.id);
            }
        }
    }

    private interface Acao {
        void executar() throws Exception;
    }

    private static void assertSemAcesso(String o_que, Acao acao) throws Exception {

        try {
            acao.executar();
            fail(o_que + ": deveria ser recusado");
        } catch (ErroApi esperado) {
            // recusado pelo Storage ou pelo RLS
        }
    }

    @Test
    public void trilhaComArquivosEntregaAvaliacaoEExclusaoLimpamOStorage() throws Exception {
        local.entrar(MARTA);
        String lia = idDaLia();

        String atividadeId = java.util.UUID.randomUUID().toString();
        String anexo = br.com.example.meufuturoagora.dados.Arquivos.caminhoAtividade(LOGICA, atividadeId, "application/pdf");
        byte[] conteudoAnexo = "anexo da professora".getBytes();

        local.servicos.arquivos.enviar(br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ATIVIDADES, anexo,
                conteudoAnexo, "application/pdf");
        local.servicos.atividades.criar(atividadeId, LOGICA, "E2E: trilha com arquivos", "Descrição", 6,
                java.time.LocalDate.now().plusDays(7), null, anexo, "lista.pdf");
        boolean excluida = false;

        try {

            // A aluna matriculada baixa o anexo e entrega com arquivo
            local.entrar(LIA);
            assertEquals("anexo igual ao enviado", new String(conteudoAnexo), new String(
                    local.servicos.arquivos.baixar(br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ATIVIDADES, anexo)));

            String caminhoEntrega = br.com.example.meufuturoagora.dados.Arquivos.caminhoEntrega(atividadeId, lia, "application/pdf");
            byte[] conteudoEntrega = "entrega da aluna".getBytes();
            local.servicos.arquivos.enviar(br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ENTREGAS, caminhoEntrega,
                    conteudoEntrega, "application/pdf");
            br.com.example.meufuturoagora.dados.Entrega entrega = local.servicos.atividades.entregar(
                    atividadeId, "Segue o arquivo", caminhoEntrega, "entrega.pdf");
            assertEquals(0, entrega.diasAtraso);

            // Outro tipo de arquivo é recusado pelo bucket (D5)
            assertSemAcesso("arquivo executável", () -> local.servicos.arquivos.enviar(
                    br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ENTREGAS,
                    br.com.example.meufuturoagora.dados.Arquivos.caminhoEntrega(atividadeId, lia, "application/pdf"),
                    new byte[]{1}, "application/x-msdownload"));

            // O professor de outra disciplina não lê a entrega nem o anexo (o colega: pgTAP 100)
            local.entrar(JOAO);
            assertSemAcesso("outro professor lendo a entrega", () -> local.servicos.arquivos.baixar(
                    br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ENTREGAS, caminhoEntrega));
            assertSemAcesso("outro professor lendo o anexo", () -> local.servicos.arquivos.baixar(
                    br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ATIVIDADES, anexo));

            // A professora da disciplina lê a entrega por link assinado e avalia
            local.entrar(MARTA);
            assertEquals("entrega igual à enviada", new String(conteudoEntrega), new String(
                    local.servicos.arquivos.baixar(br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ENTREGAS, caminhoEntrega)));
            assertNotNull(local.servicos.arquivos.urlAssinada(
                    br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ENTREGAS, caminhoEntrega, 60));

            try {
                local.servicos.atividades.avaliar(entrega.id, 7, "Fora da escala");
                fail("nota maior que os pontos da trilha");
            } catch (ErroApi esperado) {
                // o servidor confere a escala
            }

            local.servicos.atividades.avaliar(entrega.id, 5.5, "Bom trabalho");

            // Exclusão: arquivos primeiro (Storage), depois a linha
            br.com.example.meufuturoagora.dados.AtividadeRepositorio.ArquivosDaAtividade arquivos =
                    local.servicos.atividades.arquivosDaAtividade(atividadeId);
            assertEquals(anexo, arquivos.anexo);
            assertEquals(java.util.Collections.singletonList(caminhoEntrega), arquivos.entregas);

            local.servicos.arquivos.apagar(br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ATIVIDADES,
                    java.util.Collections.singletonList(arquivos.anexo));
            local.servicos.arquivos.apagar(br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ENTREGAS, arquivos.entregas);
            local.servicos.atividades.excluir(atividadeId);
            excluida = true;

            assertSemAcesso("anexo depois de apagado", () -> local.servicos.arquivos.baixar(
                    br.com.example.meufuturoagora.dados.Arquivos.BUCKET_ATIVIDADES, anexo));
            assertEquals("a trilha some", null, local.servicos.atividades.buscar(atividadeId));

        } finally {

            if (!excluida) {
                local.entrar(MARTA);
                try {
                    local.servicos.atividades.excluir(atividadeId);
                } catch (ErroApi ignorado) {
                    // já não existe
                }
            }
        }
    }

    @Test
    public void diarioEPrivadoDoDonoEAsPaginasCriamEditamEApagam() throws Exception {
        local.entrar(LIA);
        br.com.example.meufuturoagora.dados.EntradaDiario pagina =
                local.servicos.diario.escrever("E2E: meu sonho");
        boolean apagada = false;

        try {

            assertEquals("E2E: meu sonho", local.servicos.diario.entradas().get(0).texto);

            local.servicos.diario.editar(pagina.id, "E2E: meu sonho maior");
            assertEquals("E2E: meu sonho maior", local.servicos.diario.entradas().get(0).texto);

            // Nem professor nem admin leem o diário de um aluno (D8)
            for (String outro : new String[]{MARTA, ADMIN}) {
                local.entrar(outro);
                for (br.com.example.meufuturoagora.dados.EntradaDiario e : local.servicos.diario.entradas()) {
                    assertTrue("diário alheio visível para " + outro, !e.id.equals(pagina.id));
                }
            }

            local.entrar(LIA);
            assertEquals("a página segue lá", pagina.id, local.servicos.diario.entradas().get(0).id);

            local.servicos.diario.apagar(pagina.id);
            apagada = true;
            assertTrue(local.servicos.diario.entradas().isEmpty());

        } finally {

            if (!apagada) {
                local.entrar(LIA);
                local.servicos.diario.apagar(pagina.id);
            }
        }
    }

    @Test
    public void notaViraPontosNoRankingERefazerAvaliacaoTiraOsPontos() throws Exception {
        local.entrar(MARTA);
        String lia = idDaLia();

        String atividadeId = java.util.UUID.randomUUID().toString();
        local.servicos.atividades.criar(atividadeId, LOGICA, "E2E: trilha do ranking", "", 6,
                java.time.LocalDate.now().plusDays(7), null, null, null);

        try {

            local.entrar(LIA);
            br.com.example.meufuturoagora.dados.Entrega entrega =
                    local.servicos.atividades.entregar(atividadeId, "Pronto", null, null);
            int antes = local.servicos.ranking.pontuacao(lia, null);

            local.entrar(MARTA);
            local.servicos.atividades.avaliar(entrega.id, 4, "Bom");

            // A professora vê a turma inteira, com a Lia e a pontuação nova
            java.util.List<br.com.example.meufuturoagora.dados.Turma> turmas = local.servicos.ranking.turmasDoRanking();
            assertTrue("a professora vê a turma da disciplina dela", !turmas.isEmpty());
            boolean achouLia = false;

            for (br.com.example.meufuturoagora.dados.Turma turma : turmas) {
                for (br.com.example.meufuturoagora.dados.LinhaRanking linha : local.servicos.ranking.ranking(turma.id)) {
                    if (linha.perfilId.equals(lia)) {
                        achouLia = true;
                        assertEquals(antes + 4, linha.pontos);
                    }
                }
            }

            assertTrue("a Lia aparece no ranking da turma", achouLia);

            // A aluna vê o pódio e a própria linha, e o total bate com a pontuação
            local.entrar(LIA);
            assertEquals(antes + 4, local.servicos.ranking.pontuacao(lia, null));
            boolean eu = false;

            for (br.com.example.meufuturoagora.dados.LinhaRanking linha :
                    local.servicos.ranking.ranking(local.servicos.ranking.turmasDoRanking().get(0).id)) {
                assertTrue("aluno só vê o pódio e a si mesmo", linha.posicao <= 3 || linha.ehVoce);
                eu |= linha.ehVoce;
            }

            assertTrue(eu);
            assertNotNull(local.servicos.ranking.minhaPosicao());

            // Refazer a avaliação tira a nota da pontuação
            local.entrar(MARTA);
            local.servicos.atividades.refazerAvaliacao(entrega.id);
            local.entrar(LIA);
            assertEquals(antes, local.servicos.ranking.pontuacao(lia, null));

        } finally {

            local.entrar(MARTA);
            local.servicos.atividades.excluir(atividadeId);
        }
    }

    private static br.com.example.meufuturoagora.dados.Novidade novidadeDe(
            java.util.List<br.com.example.meufuturoagora.dados.Novidade> lista, String tipo, String nome) {

        for (br.com.example.meufuturoagora.dados.Novidade n : lista) {
            if (tipo.equals(n.tipo) && nome.equals(n.nome)) {
                return n;
            }
        }

        return null;
    }

    @Test
    public void novidadesChegamParaQuemPodeVerEPelaFuncaoDoServidor() throws Exception {
        java.time.OffsetDateTime desde = java.time.OffsetDateTime.now().minusHours(1);
        String nome = "E2E: trilha das novidades";

        local.entrar(MARTA);
        String atividadeId = java.util.UUID.randomUUID().toString();
        local.servicos.atividades.criar(atividadeId, LOGICA, nome, "", 6,
                java.time.LocalDate.now().plusDays(7), null, null, null);

        try {

            // A aluna matriculada vê a trilha nova; ainda não há avaliação
            local.entrar(LIA);
            java.util.List<br.com.example.meufuturoagora.dados.Novidade> daLia = local.servicos.novidades.desde(desde);
            br.com.example.meufuturoagora.dados.Novidade nova = novidadeDe(daLia, "atividade", nome);
            assertNotNull("a Lia vê a trilha nova", nova);
            assertEquals(atividadeId, nova.atividadeId);
            assertEquals("Lógica de Programação", nova.disciplinaNome);
            assertEquals(null, novidadeDe(daLia, "avaliacao", nome));

            br.com.example.meufuturoagora.dados.Entrega entrega =
                    local.servicos.atividades.entregar(atividadeId, "Pronto", null, null);

            // A professora da disciplina vê a entrega, com o nome do aluno, e não vê a trilha como novidade
            local.entrar(MARTA);
            br.com.example.meufuturoagora.dados.Novidade deEntrega =
                    novidadeDe(local.servicos.novidades.desde(desde), "entrega", nome);
            assertNotNull("a professora vê a entrega", deEntrega);
            assertEquals(entrega.id, deEntrega.itemId);
            assertEquals("Lia Fictícia", deEntrega.alunoNome);
            assertEquals(null, novidadeDe(local.servicos.novidades.desde(desde), "atividade", nome));

            local.servicos.atividades.avaliar(entrega.id, 3, "Bom");

            // Outro professor não vê a entrega; a aluna vê a avaliação
            local.entrar(JOAO);
            assertEquals(null, novidadeDe(local.servicos.novidades.desde(desde), "entrega", nome));

            local.entrar(LIA);
            assertNotNull("a Lia vê a avaliação", novidadeDe(local.servicos.novidades.desde(desde), "avaliacao", nome));

            // "Desde agora" não traz o que já passou
            assertEquals(null, novidadeDe(local.servicos.novidades.desde(java.time.OffsetDateTime.now().plusMinutes(5)),
                    "atividade", nome));

        } finally {

            local.entrar(MARTA);
            local.servicos.atividades.excluir(atividadeId);
        }
    }

    @Test
    public void professorComumNaoTemAreaDoAdministrador() throws Exception {
        local.entrar(JOAO);

        ActivityScenario.launch(TelaPerfilProfessor.class);

        Espera.ate(() -> onView(withId(R.id.tvNomePerfil)).check(matches(withText("João Fictício"))));
        onView(withId(R.id.btnAreaAdministrador))
                .check(matches(withEffectiveVisibility(ViewMatchers.Visibility.GONE)));
    }
}
