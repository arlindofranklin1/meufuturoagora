package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;

import mockwebserver3.RecordedRequest;

// RestSupabase (PostgREST) e StorageSupabase: URL, método, cabeçalhos e corpo
public class RestEStorageTest {

    private ServidorFalso falso;

    @Before
    public void preparar() throws Exception {
        falso = new ServidorFalso();
        falso.logar();
    }

    @After
    public void encerrar() throws Exception {
        falso.close();
    }

    // ===== REST =====

    @Test
    public void filtrosSaoCodificadosNaUrl() throws Exception {
        falso.responder("[]");

        falso.rest.selecionar("perfis", Consulta.campos("id,nome")
                .igual("nome", "Ana & Bia, 6º")
                .maiorQue("criado_em", "2026-01-01")
                .em("tipo", Arrays.asList("aluno", "professor"))
                .ordenar("nome.desc")
                .limite(5));

        RecordedRequest requisicao = falso.proxima();
        assertEquals("GET", requisicao.getMethod());
        assertEquals("/rest/v1/perfis", requisicao.getUrl().encodedPath());
        assertEquals("id,nome", requisicao.getUrl().queryParameter("select"));
        assertEquals("eq.Ana & Bia, 6º", requisicao.getUrl().queryParameter("nome"));
        assertEquals("gt.2026-01-01", requisicao.getUrl().queryParameter("criado_em"));
        assertEquals("in.(aluno,professor)", requisicao.getUrl().queryParameter("tipo"));
        assertEquals("nome.desc", requisicao.getUrl().queryParameter("order"));
        assertEquals("5", requisicao.getUrl().queryParameter("limit"));
        assertTrue("o & do valor não pode virar separador",
                requisicao.getUrl().encodedQuery().contains("%26"));
    }

    @Test
    public void selecionarUmDevolveNuloSemLinhas() throws Exception {
        falso.responder("[]");

        assertNull(falso.rest.selecionarUm("perfis", Consulta.campos("*").igual("id", "x")));
        assertEquals("1", falso.proxima().getUrl().queryParameter("limit"));
    }

    @Test
    public void inserirPedeARepresentacaoDeVolta() throws Exception {
        falso.responder(201, "[{\"id\":\"t1\",\"nome\":\"7º A\"}]");

        JSONObject linha = falso.rest.inserir("turmas", Json.objeto("nome", "7º A", "serie", null));

        RecordedRequest requisicao = falso.proxima();
        assertEquals("POST", requisicao.getMethod());
        assertEquals("return=representation", requisicao.getHeaders().get("Prefer"));
        assertTrue(requisicao.getHeaders().get("Content-Type").startsWith("application/json"));
        JSONObject corpo = new JSONObject(requisicao.getBody().utf8());
        assertEquals("7º A", corpo.getString("nome"));
        assertTrue("null vai como null de JSON", corpo.isNull("serie"));
        assertEquals("t1", linha.getString("id"));
    }

    @Test
    public void inserirSemLinhaDeVoltaEErro() {
        falso.responder(201, "[]");

        ErroApi erro = ServidorFalso.erroDe(() -> falso.rest.inserir("turmas", Json.objeto("nome", "x")));

        assertEquals("O servidor mandou uma resposta inesperada.", erro.getMessage());
    }

    @Test
    public void atualizarEApagarUsamFiltro() throws Exception {
        falso.responder("[{\"id\":\"t1\"}]");
        falso.responder("[]");

        JSONArray alteradas = falso.rest.atualizar("turmas", Consulta.campos("id").igual("id", "t1"),
                Json.objeto("nome", "Novo"));
        JSONArray apagadas = falso.rest.apagar("turmas", Consulta.campos("id").igual("id", "t2"));

        RecordedRequest patch = falso.proxima();
        assertEquals("PATCH", patch.getMethod());
        assertEquals("eq.t1", patch.getUrl().queryParameter("id"));
        assertEquals("Novo", new JSONObject(patch.getBody().utf8()).getString("nome"));
        assertEquals(1, alteradas.length());

        RecordedRequest delete = falso.proxima();
        assertEquals("DELETE", delete.getMethod());
        assertEquals("eq.t2", delete.getUrl().queryParameter("id"));
        assertEquals(0, apagadas.length());
    }

    @Test
    public void rpcDevolveObjetoListaNumeroOuNada() throws Exception {
        falso.responder("{\"id\":\"p1\"}");
        falso.responder("[{\"posicao\":1}]");
        falso.responder("7");
        falso.responder(204, "");
        falso.responder("null");

        assertEquals("p1", falso.rest.rpcObjeto("vincular_conta", null).getString("id"));
        assertEquals(1, falso.rest.rpcLista("ranking_turma", Json.objeto("p_turma", "t")).length());
        assertEquals(7, ((Number) falso.rest.rpc("pontuacao", null)).intValue());
        assertNull(falso.rest.rpc("definir_ano_letivo", Json.objeto("p_ano", 2027)));
        assertNull(falso.rest.rpc("excluir_usuario", null));

        RecordedRequest primeira = falso.proxima();
        assertEquals("/rest/v1/rpc/vincular_conta", primeira.getUrl().encodedPath());
        assertEquals("{}", primeira.getBody().utf8());
        assertEquals("t", new JSONObject(falso.proxima().getBody().utf8()).getString("p_turma"));
    }

    @Test
    public void rpcListaComRespostaQueNaoEListaDevolveVazia() throws Exception {
        falso.responder("{\"x\":1}");

        assertEquals(0, falso.rest.rpcLista("qualquer", null).length());
    }

    @Test
    public void rpcObjetoComRespostaQueNaoEObjetoEErro() {
        falso.responder("[]");

        ServidorFalso.erroDe(() -> falso.rest.rpcObjeto("vincular_conta", null));
    }

    // ===== Storage =====

    @Test
    public void enviarArquivoNoCaminhoDoBucketSemSobrescrever() throws Exception {
        StorageSupabase storage = new StorageSupabase(falso.cliente);
        falso.responder("{\"Key\":\"entregas/a/b/c.pdf\"}");
        byte[] pdf = "%PDF-1.7".getBytes(StandardCharsets.UTF_8);

        storage.enviar(Arquivos.BUCKET_ENTREGAS, "a/b/c.pdf", pdf, "application/pdf");

        RecordedRequest requisicao = falso.proxima();
        assertEquals("POST", requisicao.getMethod());
        assertEquals("/storage/v1/object/entregas/a/b/c.pdf", requisicao.getUrl().encodedPath());
        assertEquals("application/pdf", requisicao.getHeaders().get("Content-Type"));
        assertNull("sem x-upsert: não substitui arquivo existente", requisicao.getHeaders().get("x-upsert"));
        assertEquals("Bearer " + ServidorFalso.TOKEN, requisicao.getHeaders().get("Authorization"));
        assertArrayEquals(pdf, requisicao.getBody().toByteArray());
    }

    @Test
    public void urlAssinadaViraEnderecoCompleto() throws Exception {
        StorageSupabase storage = new StorageSupabase(falso.cliente);
        falso.responder("{\"signedURL\":\"/object/sign/atividades/d/t/x.pdf?token=abc\"}");

        String url = storage.urlAssinada(Arquivos.BUCKET_ATIVIDADES, "d/t/x.pdf", 600);

        RecordedRequest requisicao = falso.proxima();
        assertEquals("/storage/v1/object/sign/atividades/d/t/x.pdf", requisicao.getUrl().encodedPath());
        assertEquals(600, new JSONObject(requisicao.getBody().utf8()).getInt("expiresIn"));
        assertEquals(falso.http.urlBase() + "/storage/v1/object/sign/atividades/d/t/x.pdf?token=abc", url);
    }

    @Test
    public void baixarUsaARotaAutenticada() throws Exception {
        StorageSupabase storage = new StorageSupabase(falso.cliente);
        falso.responder("conteúdo");

        byte[] bytes = storage.baixar(Arquivos.BUCKET_FOTOS, "p/f.jpg");

        assertEquals("/storage/v1/object/authenticated/fotos-perfil/p/f.jpg", falso.proxima().getUrl().encodedPath());
        assertEquals("conteúdo", new String(bytes, StandardCharsets.UTF_8));
    }

    @Test
    public void apagarMandaOsPrefixos() throws Exception {
        StorageSupabase storage = new StorageSupabase(falso.cliente);
        falso.responder("[]");

        storage.apagar(Arquivos.BUCKET_ENTREGAS, Arrays.asList("a/b/1.pdf", "a/c/2.png"));
        storage.apagar(Arquivos.BUCKET_ENTREGAS, Collections.emptyList());

        RecordedRequest requisicao = falso.proxima();
        assertEquals("DELETE", requisicao.getMethod());
        assertEquals("/storage/v1/object/entregas", requisicao.getUrl().encodedPath());
        JSONArray prefixos = new JSONObject(requisicao.getBody().utf8()).getJSONArray("prefixes");
        assertEquals("a/b/1.pdf", prefixos.getString(0));
        assertEquals("a/c/2.png", prefixos.getString(1));
        assertEquals("lista vazia não chama o servidor", 1, falso.requisicoes());
    }

    @Test
    public void arquivoGrandeOuDeTipoProibidoTemMensagemPropria() {
        StorageSupabase storage = new StorageSupabase(falso.cliente);
        falso.responder(413, "{\"statusCode\":\"413\",\"error\":\"Payload too large\",\"message\":\"The object exceeded the maximum allowed size\"}");
        falso.responder(400, "{\"statusCode\":\"415\",\"error\":\"invalid_mime_type\",\"message\":\"mime type application/zip is not supported\"}");

        ErroApi grande = ServidorFalso.erroDe(() -> storage.enviar("entregas", "a/b/c.pdf", new byte[1], "application/pdf"));
        ErroApi tipo = ServidorFalso.erroDe(() -> storage.enviar("entregas", "a/b/c.zip", new byte[1], "application/zip"));

        assertEquals("O arquivo é grande demais. O limite é de 3 MB.", grande.getMessage());
        assertEquals("Tipo de arquivo não permitido. Envie um PDF ou uma imagem.", tipo.getMessage());
    }
}
