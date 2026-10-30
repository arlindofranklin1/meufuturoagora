package br.com.example.meufuturoagora.dados;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InterruptedIOException;

// Erro único da camada de dados. getMessage() já é a frase para mostrar ao
// usuário, em português; a resposta crua do servidor nunca vai para a tela
// (pode trazer detalhes internos), só o código, para o app decidir o que fazer.
public class ErroApi extends Exception {

    // Sem resposta do servidor (sem internet, tempo esgotado)
    public static final int SEM_RESPOSTA = 0;

    private final int codigoHttp;
    private final String codigo;

    ErroApi(int codigoHttp, String codigo, String mensagem, Throwable causa) {
        super(mensagem, causa);
        this.codigoHttp = codigoHttp;
        this.codigo = codigo;
    }

    public int codigoHttp() {
        return codigoHttp;
    }

    // Código do Postgres (ex.: 23505) ou do Auth/Storage, quando houver
    public String codigo() {
        return codigo;
    }

    public boolean semPermissao() {
        return codigoHttp == 401 || codigoHttp == 403 || "42501".equals(codigo);
    }

    public boolean jaExiste() {
        return "23505".equals(codigo) || codigoHttp == 409;
    }

    static ErroApi daResposta(int codigoHttp, String corpo) {

        String codigo = null;
        String mensagemServidor = null;

        try {
            JSONObject json = new JSONObject(corpo);

            // PostgREST: code/message; Auth: error_code/msg; Storage: error/message
            codigo = texto(json, "code", "error_code", "error");
            mensagemServidor = texto(json, "message", "msg", "error_description");

        } catch (JSONException | NullPointerException ignorado) {
            // Corpo que não é JSON: fica só o código HTTP
        }

        return new ErroApi(codigoHttp, codigo, mensagemAmigavel(codigoHttp, codigo, mensagemServidor), null);
    }

    static ErroApi semConexao(IOException causa) {

        String mensagem = causa instanceof InterruptedIOException
                ? "A conexão demorou demais. Tente de novo."
                : "Sem conexão com a internet. Confira a rede e tente de novo.";

        return new ErroApi(SEM_RESPOSTA, null, mensagem, causa);
    }

    static ErroApi sessaoExpirada() {
        return new ErroApi(401, null, "Sua sessão expirou. Entre de novo.", null);
    }

    static ErroApi respostaInvalida(Throwable causa) {
        return new ErroApi(SEM_RESPOSTA, null, "O servidor mandou uma resposta inesperada.", causa);
    }

    static ErroApi inesperado(Throwable causa) {
        return new ErroApi(SEM_RESPOSTA, null, "Algo deu errado. Tente de novo.", causa);
    }

    private static String mensagemAmigavel(int codigoHttp, String codigo, String mensagemServidor) {

        boolean temMensagem = mensagemServidor != null && !mensagemServidor.trim().isEmpty();

        // Regras de negócio das funções do banco: a mensagem já é para o usuário
        if (temMensagem && ("P0001".equals(codigo) || "P0002".equals(codigo))) {
            return mensagemServidor;
        }

        // As recusas das funções do app (42501) também vêm prontas; as do
        // próprio Postgres ("permission denied ...") não
        if (temMensagem && "42501".equals(codigo)
                && !mensagemServidor.startsWith("permission denied")
                && !mensagemServidor.startsWith("new row violates")) {
            return mensagemServidor;
        }

        if ("23505".equals(codigo) || codigoHttp == 409) {
            return "Já existe um cadastro com esses dados.";
        }

        if (codigo != null && (codigo.startsWith("22") || codigo.startsWith("23"))) {
            return "Algum dado informado é inválido.";
        }

        if (codigoHttp == 413 || (temMensagem && mensagemServidor.contains("exceeded the maximum allowed size"))) {
            return "O arquivo é grande demais. O limite é de 3 MB.";
        }

        if ("invalid_mime_type".equals(codigo) || (temMensagem && mensagemServidor.contains("mime type"))) {
            return "Tipo de arquivo não permitido. Envie um PDF ou uma imagem.";
        }

        if (codigoHttp == 401) {
            return "Sua sessão expirou. Entre de novo.";
        }

        if (codigoHttp == 403 || "42501".equals(codigo)) {
            return "Você não tem permissão para fazer isso.";
        }

        if (codigoHttp == 404) {
            return "Não encontrado.";
        }

        if (codigoHttp >= 500) {
            return "O servidor está com problemas. Tente de novo em instantes.";
        }

        return "Não foi possível concluir a operação.";
    }

    private static String texto(JSONObject json, String... campos) {

        for (String campo : campos) {
            Object valor = json.opt(campo);

            if (valor instanceof String && !((String) valor).isEmpty()) {
                return (String) valor;
            }
        }

        return null;
    }
}
