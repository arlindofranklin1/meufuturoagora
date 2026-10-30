package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import mockwebserver3.MockResponse;
import mockwebserver3.RecordedRequest;

// Cabeçalhos, renovação de token ao receber 401 e tradução de erros
public class ClienteSupabaseTest {

    private ServidorFalso falso;

    @Before
    public void preparar() throws Exception {
        falso = new ServidorFalso();
    }

    @After
    public void encerrar() throws Exception {
        falso.close();
    }

    private static ErroApi erroDe(ServidorFalso.Acao acao) {
        return ServidorFalso.erroDe(acao);
    }

    // ===== Cabeçalhos =====

    @Test
    public void semLoginVaiSoComAChavePublica() throws Exception {
        falso.responder("[]");

        falso.rest.selecionar("turmas", Consulta.campos("*"));

        RecordedRequest requisicao = falso.proxima();
        assertEquals(ServidorFalso.CHAVE_PUBLICA, requisicao.getHeaders().get("apikey"));
        assertNull("a chave publicável não é JWT e não vai no Authorization",
                requisicao.getHeaders().get("Authorization"));
    }

    @Test
    public void comLoginVaiOTokenDoUsuario() throws Exception {
        falso.logar();
        falso.responder("[]");

        falso.rest.selecionar("turmas", Consulta.campos("*"));

        RecordedRequest requisicao = falso.proxima();
        assertEquals(ServidorFalso.CHAVE_PUBLICA, requisicao.getHeaders().get("apikey"));
        assertEquals("Bearer " + ServidorFalso.TOKEN, requisicao.getHeaders().get("Authorization"));
    }

    // ===== 401: renova e repete =====

    @Test
    public void tokenRecusadoRenovaERepeteComONovo() throws Exception {
        falso.logar();
        falso.responder(401, "{\"code\":\"PGRST301\",\"message\":\"JWT expired\"}");
        falso.responderSessao("token-novo", "renovacao-nova", falso.agora.get() + 3600);
        falso.responder("[{\"id\":\"t1\",\"nome\":\"6º A\"}]");

        assertEquals(1, falso.rest.selecionar("turmas", Consulta.campos("*")).length());

        falso.proxima();
        RecordedRequest renovacao = falso.proxima();
        assertEquals("/auth/v1/token?grant_type=refresh_token", renovacao.getUrl().encodedPath()
                + "?" + renovacao.getUrl().encodedQuery());
        RecordedRequest repetida = falso.proxima();
        assertEquals("Bearer token-novo", repetida.getHeaders().get("Authorization"));
        assertEquals("renovacao-nova", falso.armazenamento.salvo);
    }

    @Test
    public void segundoRecusoDesiste() throws Exception {
        falso.logar();
        falso.responder(401, "{}");
        falso.responderSessao("token-novo", "renovacao-nova", falso.agora.get() + 3600);
        falso.responder(401, "{}");

        ErroApi erro = erroDe(() -> falso.rest.selecionar("turmas", Consulta.campos("*")));

        assertEquals(401, erro.codigoHttp());
        assertEquals(3, falso.requisicoes());
    }

    @Test
    public void renovacaoRecusadaEncerraASessao() throws Exception {
        falso.logar();
        falso.responder(401, "{}");
        falso.responder(400, "{\"error_code\":\"refresh_token_not_found\",\"msg\":\"Invalid Refresh Token\"}");

        ErroApi erro = erroDe(() -> falso.rest.selecionar("turmas", Consulta.campos("*")));

        assertEquals(401, erro.codigoHttp());
        assertEquals("Sua sessão expirou. Entre de novo.", erro.getMessage());
        assertFalse(falso.sessao.temSessao());
        assertNull("o token salvo no aparelho é apagado", falso.armazenamento.salvo);
    }

    @Test
    public void semLogin401NaoTentaRenovar() throws Exception {
        falso.responder(401, "{}");

        erroDe(() -> falso.rest.selecionar("turmas", Consulta.campos("*")));

        assertEquals(1, falso.requisicoes());
    }

    @Test
    public void tokenPertoDeVencerERenovadoAntes() throws Exception {
        falso.logar();
        falso.agora.addAndGet(3600 - 30);
        falso.responderSessao("token-novo", "renovacao-nova", falso.agora.get() + 3600);
        falso.responder("[]");

        falso.rest.selecionar("turmas", Consulta.campos("*"));

        assertTrue(falso.proxima().getUrl().encodedPath().endsWith("/auth/v1/token"));
        assertEquals("Bearer token-novo", falso.proxima().getHeaders().get("Authorization"));
    }

    // ===== Erros =====

    @Test
    public void erroDeRegraDeNegocioTrazAMensagemDoServidor() throws Exception {
        falso.logar();
        falso.responder(400, "{\"code\":\"P0001\",\"message\":\"A nota precisa ficar entre 0 e 5.\"}");

        ErroApi erro = erroDe(() -> falso.rest.rpc("avaliar_entrega", null));

        assertEquals(400, erro.codigoHttp());
        assertEquals("P0001", erro.codigo());
        assertEquals("A nota precisa ficar entre 0 e 5.", erro.getMessage());
    }

    @Test
    public void erros403404409E500ViramMensagensAmigaveis() throws Exception {
        falso.logar();
        falso.responder(403, "{\"code\":\"42501\",\"message\":\"permission denied for table perfis\"}");
        falso.responder(404, "{\"code\":\"PGRST202\",\"message\":\"Could not find the function\"}");
        falso.responder(409, "{\"code\":\"23505\",\"message\":\"duplicate key value violates unique constraint \\\"perfis_email_key\\\"\"}");
        falso.responder(503, "<html>indisponível</html>");

        ErroApi e403 = erroDe(() -> falso.rest.selecionar("perfis", Consulta.campos("*")));
        ErroApi e404 = erroDe(() -> falso.rest.rpc("nao_existe", null));
        ErroApi e409 = erroDe(() -> falso.rest.inserir("perfis", Json.objeto("email", "x@y.z")));
        ErroApi e503 = erroDe(() -> falso.rest.selecionar("perfis", Consulta.campos("*")));

        assertEquals("Você não tem permissão para fazer isso.", e403.getMessage());
        assertTrue(e403.semPermissao());
        assertEquals("Não encontrado.", e404.getMessage());
        assertEquals("Já existe um cadastro com esses dados.", e409.getMessage());
        assertTrue(e409.jaExiste());
        assertEquals("O servidor está com problemas. Tente de novo em instantes.", e503.getMessage());
    }

    @Test
    public void tempoEsgotadoViraErroSemResposta() throws Exception {
        falso.servidor.enqueue(new MockResponse.Builder()
                .body("[]")
                .headersDelay(2, TimeUnit.SECONDS)
                .build());

        ErroApi erro = erroDe(() -> falso.rest.selecionar("turmas", Consulta.campos("*")));

        assertEquals(ErroApi.SEM_RESPOSTA, erro.codigoHttp());
        assertEquals("A conexão demorou demais. Tente de novo.", erro.getMessage());
    }

    @Test
    public void servidorForaDoArViraSemConexao() throws Exception {
        falso.close();

        ErroApi erro = erroDe(() -> falso.rest.selecionar("turmas", Consulta.campos("*")));

        assertEquals(ErroApi.SEM_RESPOSTA, erro.codigoHttp());
        assertTrue(erro.getMessage().startsWith("Sem conexão com a internet"));
    }

    @Test
    public void respostaQueNaoEJsonViraRespostaInvalida() throws Exception {
        falso.responder("isto não é json");

        ErroApi erro = erroDe(() -> falso.rest.selecionar("turmas", Consulta.campos("*")));

        assertEquals("O servidor mandou uma resposta inesperada.", erro.getMessage());
    }

    // ===== Nenhum token vaza =====

    @Test
    public void tokensNaoAparecemEmMensagensNemNaSessao() throws Exception {
        falso.logar();
        // Um servidor que devolvesse o próprio token no corpo do erro
        falso.responder(400, "{\"code\":\"XX000\",\"message\":\"falhou com " + ServidorFalso.TOKEN + "\"}");

        ErroApi erro = erroDe(() -> falso.rest.selecionar("turmas", Consulta.campos("*")));

        assertFalse(erro.getMessage().contains(ServidorFalso.TOKEN));
        assertFalse(erro.toString().contains(ServidorFalso.TOKEN));
        assertFalse(falso.sessao.atual().toString().contains(ServidorFalso.TOKEN));
        assertFalse(falso.sessao.atual().toString().contains(ServidorFalso.TOKEN_RENOVACAO));
    }
}
