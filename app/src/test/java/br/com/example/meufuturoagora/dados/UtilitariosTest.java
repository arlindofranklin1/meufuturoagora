package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Test;

import java.io.IOException;
import java.net.SocketTimeoutException;

// ErroApi, Json, Arquivos e Nonce
public class UtilitariosTest {

    // ===== ErroApi =====

    @Test
    public void mensagensDoPostgresNaoVaoParaATela() {
        ErroApi negado = ErroApi.daResposta(403, "{\"code\":\"42501\",\"message\":\"permission denied for function x\"}");
        ErroApi rls = ErroApi.daResposta(403, "{\"code\":\"42501\",\"message\":\"new row violates row-level security policy\"}");
        ErroApi check = ErroApi.daResposta(400, "{\"code\":\"23514\",\"message\":\"new row for relation \\\"atividades\\\" violates check constraint\"}");
        ErroApi tipo = ErroApi.daResposta(400, "{\"code\":\"22P02\",\"message\":\"invalid input syntax for type uuid\"}");

        assertEquals("Você não tem permissão para fazer isso.", negado.getMessage());
        assertEquals("Você não tem permissão para fazer isso.", rls.getMessage());
        assertEquals("Algum dado informado é inválido.", check.getMessage());
        assertEquals("Algum dado informado é inválido.", tipo.getMessage());
    }

    @Test
    public void recusasDasFuncoesDoAppVaoParaATela() {
        ErroApi recusa = ErroApi.daResposta(403, "{\"code\":\"42501\",\"message\":\"Apenas administradores podem fazer isto.\"}");
        ErroApi naoEncontrado = ErroApi.daResposta(400, "{\"code\":\"P0002\",\"message\":\"Pedido não encontrado ou já decidido.\"}");

        assertEquals("Apenas administradores podem fazer isto.", recusa.getMessage());
        assertTrue(recusa.semPermissao());
        assertEquals("Pedido não encontrado ou já decidido.", naoEncontrado.getMessage());
    }

    @Test
    public void corpoQueNaoEJsonFicaSoComOCodigoHttp() {
        ErroApi erro = ErroApi.daResposta(502, "<html>Bad gateway</html>");

        assertNull(erro.codigo());
        assertEquals(502, erro.codigoHttp());
        assertEquals("O servidor está com problemas. Tente de novo em instantes.", erro.getMessage());
        assertEquals("Não foi possível concluir a operação.", ErroApi.daResposta(418, "").getMessage());
        assertEquals("Sua sessão expirou. Entre de novo.", ErroApi.daResposta(401, null).getMessage());
    }

    @Test
    public void semConexaoDiferenciaTempoEsgotado() {
        assertEquals("A conexão demorou demais. Tente de novo.",
                ErroApi.semConexao(new SocketTimeoutException()).getMessage());
        assertTrue(ErroApi.semConexao(new IOException("falhou")).getMessage().startsWith("Sem conexão"));
        assertEquals("Algo deu errado. Tente de novo.", ErroApi.inesperado(new RuntimeException()).getMessage());
    }

    // ===== Json =====

    @Test
    public void nullDeJsonViraNullDeJava() throws Exception {
        JSONObject json = new JSONObject("{\"a\":null,\"b\":\"x\",\"n\":\"7.25\",\"m\":\"não\",\"i\":3}");

        assertNull(Json.texto(json, "a"));
        assertNull(Json.texto(json, "inexistente"));
        assertEquals("x", Json.texto(json, "b"));
        assertEquals(7.25, Json.decimal(json, "n"), 0.0001);
        assertNull(Json.decimal(json, "m"));
        assertNull(Json.decimal(json, "a"));
        assertNull(Json.inteiro(json, "b"));
        assertEquals(Integer.valueOf(3), Json.inteiro(json, "i"));
        assertNull(Json.data(json, "a"));
        assertNull(Json.momento(json, "a"));
    }

    @Test
    public void linhaMalformadaViraRespostaInvalida() throws Exception {
        JSONArray linhas = new JSONArray("[{\"prazo\":\"amanhã\"}]");

        ErroApi erro = ServidorFalso.erroDe(() -> Json.lista(linhas, Atividade::deJson));

        assertEquals("O servidor mandou uma resposta inesperada.", erro.getMessage());
        assertNull(Json.um(null, Atividade::deJson));
    }

    // ===== Arquivos =====

    @Test
    public void soPdfEImagensSaoAceitos() {
        assertTrue(Arquivos.tipoPermitido("application/pdf"));
        assertTrue(Arquivos.tipoPermitido("image/jpeg"));
        assertTrue(Arquivos.tipoPermitido("image/png"));
        assertTrue(Arquivos.tipoPermitido("image/webp"));
        assertFalse(Arquivos.tipoPermitido("application/vnd.openxmlformats-officedocument.wordprocessingml.document"));
        assertFalse(Arquivos.tipoPermitido("video/mp4"));
        assertFalse(Arquivos.tipoPermitido(null));
        assertEquals(3_145_728L, Arquivos.LIMITE_BYTES);
    }

    @Test
    public void caminhosSeguemAsPastasDasPoliticas() {
        String atividade = Arquivos.caminhoAtividade("d1", "t1", "application/pdf");
        String entrega = Arquivos.caminhoEntrega("t1", "a1", "image/png");
        String foto = Arquivos.caminhoFoto("p1");

        assertTrue(atividade.matches("d1/t1/[0-9a-f-]{36}\\.pdf"));
        assertTrue(entrega.matches("t1/a1/[0-9a-f-]{36}\\.png"));
        assertTrue(foto.matches("p1/[0-9a-f-]{36}\\.jpg"));
        assertNotEquals("cada arquivo tem um nome novo", foto, Arquivos.caminhoFoto("p1"));
        assertEquals("webp", Arquivos.extensao("image/webp"));
        assertEquals("jpg", Arquivos.extensao("image/jpeg"));
        assertEquals("bin", Arquivos.extensao("text/plain"));
        assertEquals("bin", Arquivos.extensao(null));
    }

    // ===== Nonce =====

    @Test
    public void nonceTemHashSha256DoValorCru() {
        Nonce nonce = Nonce.gerar();

        assertEquals(Nonce.sha256Hex(nonce.cru), nonce.hash);
        assertEquals(64, nonce.hash.length());
        assertNotEquals(nonce.cru, Nonce.gerar().cru);
        // Vetor conhecido do SHA-256
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", Nonce.sha256Hex("abc"));
    }
}
