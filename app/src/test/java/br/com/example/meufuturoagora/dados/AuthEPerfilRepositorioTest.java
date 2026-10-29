package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import mockwebserver3.RecordedRequest;

public class AuthEPerfilRepositorioTest {

    private static final String PERFIL_JSON = "{\"id\":\"p1\",\"usuario_id\":\"" + ServidorFalso.USUARIO_ID
            + "\",\"email\":\"ana@escola.test\",\"nome\":\"Ana\",\"tipo\":\"aluno\",\"admin\":false,"
            + "\"ativo\":true,\"turma_id\":\"t1\",\"foto_google_url\":null,\"foto_path\":null}";

    private ServidorFalso falso;
    private AuthRepositorioSupabase auth;
    private PerfilRepositorioSupabase perfis;

    @Before
    public void preparar() throws Exception {
        falso = new ServidorFalso();
        auth = new AuthRepositorioSupabase(falso.auth, falso.sessao, falso.rest);
        perfis = new PerfilRepositorioSupabase(falso.rest, falso.sessao);
    }

    @After
    public void encerrar() throws Exception {
        falso.close();
    }

    // ===== AuthRepositorio =====

    @Test
    public void entrarTrocaOTokenEVinculaAConta() throws Exception {
        falso.responderSessao("acesso", "renovacao", falso.agora.get() + 3600);
        falso.responder(PERFIL_JSON);

        Perfil perfil = auth.entrarComGoogle("id-token", "nonce");

        falso.proxima();
        RecordedRequest vincular = falso.proxima();
        assertEquals("/rest/v1/rpc/vincular_conta", vincular.getUrl().encodedPath());
        assertEquals("Bearer acesso", vincular.getHeaders().get("Authorization"));
        assertEquals("p1", perfil.id);
        assertTrue(perfil.ehAluno());
        assertTrue(falso.sessao.temSessao());
    }

    @Test
    public void emailNaoCadastradoNaoDeixaSessaoAberta() throws Exception {
        falso.responderSessao("acesso", "renovacao", falso.agora.get() + 3600);
        falso.responder(403, "{\"code\":\"42501\",\"message\":\"Seu e-mail não está autorizado a acessar o aplicativo. Fale com a coordenação da escola.\"}");
        falso.responder(204, "");

        ErroApi erro = ServidorFalso.erroDe(() -> auth.entrarComGoogle("id-token", "nonce"));

        assertEquals("Seu e-mail não está autorizado a acessar o aplicativo. Fale com a coordenação da escola.",
                erro.getMessage());
        assertFalse(falso.sessao.temSessao());
        assertNull(falso.armazenamento.salvo);
        falso.proxima();
        falso.proxima();
        assertEquals("/auth/v1/logout", falso.proxima().getUrl().encodedPath());
    }

    @Test
    public void erroDeRedeNoVinculoMantemASessao() throws Exception {
        falso.responderSessao("acesso", "renovacao", falso.agora.get() + 3600);
        falso.responder(503, "");

        ServidorFalso.erroDe(() -> auth.entrarComGoogle("id-token", "nonce"));

        assertTrue(falso.sessao.temSessao());
    }

    @Test
    public void restaurarSemSessaoDevolveNulo() throws Exception {
        assertNull(auth.restaurarSessao());
    }

    @Test
    public void restaurarRenovaEVincula() throws Exception {
        falso.armazenamento.salvo = "renovacao-salva";
        falso.responderSessao("acesso", "renovacao", falso.agora.get() + 3600);
        falso.responder(PERFIL_JSON);

        assertEquals("p1", auth.restaurarSessao().id);
    }

    @Test
    public void sairApagaASessao() throws Exception {
        falso.logar();
        falso.responder(204, "");

        auth.sair();

        assertFalse(falso.sessao.temSessao());
    }

    // ===== PerfilRepositorio =====

    @Test
    public void meuPerfilFiltraPelaContaDaSessao() throws Exception {
        falso.logar();
        falso.responder("[" + PERFIL_JSON + "]");

        Perfil perfil = perfis.meu();

        assertEquals("eq." + ServidorFalso.USUARIO_ID, falso.proxima().getUrl().queryParameter("usuario_id"));
        assertEquals("Ana", perfil.nome);
        assertEquals("t1", perfil.turmaId);
        assertNull(perfil.fotoPath);
        assertFalse(perfil.semConta());
    }

    @Test
    public void meuPerfilSemSessaoEErro() {
        ErroApi erro = ServidorFalso.erroDe(() -> perfis.meu());

        assertEquals(401, erro.codigoHttp());
    }

    @Test
    public void listarFiltraPorTipoEOrdenaPorNome() throws Exception {
        falso.logar();
        falso.responder("[" + PERFIL_JSON + "]");

        List<Perfil> lista = perfis.listar(Perfil.ALUNO);

        RecordedRequest requisicao = falso.proxima();
        assertEquals("eq.aluno", requisicao.getUrl().queryParameter("tipo"));
        assertEquals("nome", requisicao.getUrl().queryParameter("order"));
        assertEquals(1, lista.size());
    }

    @Test
    public void cadastrarLimpaEspacos() throws Exception {
        falso.logar();
        falso.responder(201, "[" + PERFIL_JSON + "]");

        perfis.cadastrar("  ana@escola.test ", " Ana ", Perfil.ALUNO, "t1");

        JSONObject corpo = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals("ana@escola.test", corpo.getString("email"));
        assertEquals("Ana", corpo.getString("nome"));
        assertEquals("aluno", corpo.getString("tipo"));
        assertEquals("t1", corpo.getString("turma_id"));
    }

    @Test
    public void cadastrarEmailRepetido() {
        falso.logar();
        falso.responder(409, "{\"code\":\"23505\",\"message\":\"duplicate key\"}");

        ErroApi erro = ServidorFalso.erroDe(() -> perfis.cadastrar("a@b.c", "A", Perfil.ALUNO, null));

        assertTrue(erro.jaExiste());
    }

    @Test
    public void atualizarSemLinhaAlteradaESemPermissao() throws Exception {
        falso.logar();
        falso.responder("[" + PERFIL_JSON + "]");
        falso.responder("[]");

        Perfil alterado = perfis.atualizar("p1", "ana@escola.test", "Ana", "t1", true, false);
        ErroApi erro = ServidorFalso.erroDe(() -> perfis.atualizar("p2", "x@y.z", "X", null, true, false));

        RecordedRequest requisicao = falso.proxima();
        assertEquals("PATCH", requisicao.getMethod());
        assertEquals("eq.p1", requisicao.getUrl().queryParameter("id"));
        JSONObject corpo = new JSONObject(requisicao.getBody().utf8());
        assertFalse(corpo.getBoolean("admin"));
        assertTrue(corpo.getBoolean("ativo"));
        assertEquals("p1", alterado.id);
        assertTrue(erro.semPermissao());
    }

    @Test
    public void definirFotoMandaSoOCaminho() throws Exception {
        falso.logar();
        falso.responder("[{\"id\":\"p1\"}]");
        falso.responder("[{\"id\":\"p1\"}]");

        perfis.definirFoto("p1", "p1/foto.jpg");
        perfis.definirFoto("p1", null);

        JSONObject comFoto = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals(1, comFoto.length());
        assertEquals("p1/foto.jpg", comFoto.getString("foto_path"));
        assertTrue("null volta para a foto do Google",
                new JSONObject(falso.proxima().getBody().utf8()).isNull("foto_path"));
    }

    @Test
    public void exclusaoTransferenciaEPedidosChamamAsFuncoes() throws Exception {
        falso.logar();
        falso.responder(204, "");
        falso.responder("2");
        falso.responder("\"s1\"");
        falso.responder("[{\"id\":\"s1\",\"perfil_id\":\"p1\",\"motivo\":null,\"status\":\"pendente\","
                + "\"criado_em\":\"2026-10-29T12:00:00.123456+00:00\",\"decidido_em\":null}]");
        falso.responder(204, "");

        perfis.excluir("p9");
        assertEquals(2, perfis.transferirDisciplinas("p1", "p2"));
        assertEquals("s1", perfis.solicitarExclusao("Vou mudar de escola"));
        List<SolicitacaoExclusao> pedidos = perfis.solicitacoes();
        perfis.decidirExclusao("s1", true);

        RecordedRequest excluir = falso.proxima();
        assertEquals("/rest/v1/rpc/excluir_usuario", excluir.getUrl().encodedPath());
        assertEquals("p9", new JSONObject(excluir.getBody().utf8()).getString("p_perfil"));
        JSONObject transferir = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals("p1", transferir.getString("p_de"));
        assertEquals("p2", transferir.getString("p_para"));
        assertEquals("Vou mudar de escola", new JSONObject(falso.proxima().getBody().utf8()).getString("p_motivo"));
        assertEquals("criado_em.desc", falso.proxima().getUrl().queryParameter("order"));
        JSONObject decidir = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals("s1", decidir.getString("p_solicitacao"));
        assertTrue(decidir.getBoolean("p_aprovar"));

        assertTrue(pedidos.get(0).pendente());
        assertNull(pedidos.get(0).motivo);
        assertEquals(2026, pedidos.get(0).criadoEm.getYear());
    }
}
