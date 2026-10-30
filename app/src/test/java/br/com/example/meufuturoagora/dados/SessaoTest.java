package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import mockwebserver3.RecordedRequest;

// AuthSupabase e GerenciadorSessao: login com ID token, renovação, sessão
// salva, sessão ausente ou corrompida, logout
public class SessaoTest {

    private ServidorFalso falso;

    @Before
    public void preparar() throws Exception {
        falso = new ServidorFalso();
    }

    @After
    public void encerrar() throws Exception {
        falso.close();
    }

    // ===== Auth =====

    @Test
    public void loginComGoogleMandaIdTokenNonceEProvedor() throws Exception {
        falso.responderSessao("acesso", "renovacao", falso.agora.get() + 3600);

        Sessao sessao = falso.auth.entrarComIdTokenGoogle("id-token-do-google", "nonce-cru");

        RecordedRequest requisicao = falso.proxima();
        assertEquals("POST", requisicao.getMethod());
        assertEquals("/auth/v1/token", requisicao.getUrl().encodedPath());
        assertEquals("id_token", requisicao.getUrl().queryParameter("grant_type"));
        assertEquals(ServidorFalso.CHAVE_PUBLICA, requisicao.getHeaders().get("apikey"));

        JSONObject corpo = new JSONObject(requisicao.getBody().utf8());
        assertEquals("google", corpo.getString("provider"));
        assertEquals("id-token-do-google", corpo.getString("id_token"));
        assertEquals("nonce-cru", corpo.getString("nonce"));

        assertEquals(ServidorFalso.USUARIO_ID, sessao.usuarioId);
        assertEquals("ana@escola.test", sessao.email);
        assertEquals("acesso", sessao.tokenDeAcesso);
        assertEquals("renovacao", sessao.tokenDeRenovacao);
    }

    @Test
    public void semExpiresAtCalculaPeloExpiresIn() throws Exception {
        falso.responder("{\"access_token\":\"a\",\"refresh_token\":\"r\",\"expires_in\":3600,\"user\":{\"id\":\"u\"}}");

        Sessao sessao = falso.auth.renovar("r0");

        assertEquals(falso.agora.get() + 3600, sessao.expiraEm);
        assertNull(sessao.email);
    }

    @Test
    public void respostaSemTokenViraRespostaInvalida() throws Exception {
        falso.responder("{\"user\":{\"id\":\"u\"}}");

        ErroApi erro = ServidorFalso.erroDe(() -> falso.auth.renovar("r0"));

        assertEquals("O servidor mandou uma resposta inesperada.", erro.getMessage());
    }

    @Test
    public void logoutRevogaSoEsteAparelho() throws Exception {
        falso.logar();
        falso.responder(204, "");

        falso.sessao.sair();

        RecordedRequest requisicao = falso.proxima();
        assertEquals("/auth/v1/logout", requisicao.getUrl().encodedPath());
        assertEquals("local", requisicao.getUrl().queryParameter("scope"));
        assertEquals("Bearer " + ServidorFalso.TOKEN, requisicao.getHeaders().get("Authorization"));
        assertFalse(falso.sessao.temSessao());
        assertNull(falso.armazenamento.salvo);
    }

    // ===== GerenciadorSessao =====

    @Test
    public void iniciarGuardaSoOTokenDeRenovacao() {
        falso.logar();

        assertEquals(ServidorFalso.TOKEN_RENOVACAO, falso.armazenamento.salvo);
        assertTrue(falso.sessao.temSessao());
    }

    @Test
    public void restaurarSemNadaSalvoNaoChamaOServidor() throws Exception {
        assertFalse(falso.sessao.restaurar());
        assertEquals(0, falso.requisicoes());
    }

    @Test
    public void restaurarUsaOTokenSalvo() throws Exception {
        falso.armazenamento.salvo = "renovacao-salva";
        falso.responderSessao("acesso-novo", "renovacao-nova", falso.agora.get() + 3600);

        assertTrue(falso.sessao.restaurar());

        JSONObject corpo = new JSONObject(falso.proxima().getBody().utf8());
        assertEquals("renovacao-salva", corpo.getString("refresh_token"));
        assertEquals("acesso-novo", falso.sessao.tokenDeAcesso());
        assertEquals("renovacao-nova", falso.armazenamento.salvo);
    }

    @Test
    public void restaurarComSessaoEmMemoriaNaoRenovaDeNovo() throws Exception {
        falso.logar();

        assertTrue(falso.sessao.restaurar());
        assertEquals(0, falso.requisicoes());
    }

    @Test
    public void sessaoSalvaRecusadaEApagada() throws Exception {
        falso.armazenamento.salvo = "renovacao-revogada";
        falso.responder(400, "{\"error_code\":\"refresh_token_not_found\"}");

        ErroApi erro = ServidorFalso.erroDe(() -> falso.sessao.restaurar());

        assertEquals(401, erro.codigoHttp());
        assertNull(falso.armazenamento.salvo);
    }

    @Test
    public void semInternetNaRenovacaoMantemASessaoSalva() throws Exception {
        falso.armazenamento.salvo = "renovacao-salva";
        falso.close();

        ErroApi erro = ServidorFalso.erroDe(() -> falso.sessao.restaurar());

        assertEquals(ErroApi.SEM_RESPOSTA, erro.codigoHttp());
        assertEquals("renovacao-salva", falso.armazenamento.salvo);
    }

    @Test
    public void renovarSemNenhumTokenEExpirada() {
        ErroApi erro = ServidorFalso.erroDe(() -> falso.sessao.renovar());

        assertEquals(401, erro.codigoHttp());
    }

    @Test
    public void semLoginNaoHaToken() throws Exception {
        assertNull(falso.sessao.tokenDeAcesso());
    }

    @Test
    public void sairSemInternetLimpaOAparelhoMesmoAssim() throws Exception {
        falso.logar();
        falso.close();

        falso.sessao.sair();

        assertFalse(falso.sessao.temSessao());
        assertNull(falso.armazenamento.salvo);
    }

    @Test
    public void sairSemSessaoNaoChamaOServidor() {
        falso.sessao.sair();

        assertEquals(0, falso.requisicoes());
    }
}
