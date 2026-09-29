package br.com.example.meufuturoagora;

import android.content.ContentResolver;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Envio de arquivos (atividades do professor e entregas do aluno) para o Cloudinary.
// Usa um "upload preset" do tipo Unsigned, então nenhuma chave secreta fica no app.
class CloudinaryUtil {

    // Dashboard do Cloudinary > "Cloud name"
    static final String CLOUD_NAME = "gcs5nshe";

    // Settings > Upload > Upload presets > preset com "Signing mode: Unsigned"
    static final String UPLOAD_PRESET = "meufuturoagora_unsigned";

    interface Callback {
        void onSucesso(String url);

        void onErro(String mensagem);
    }

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();
    private static final Handler principal = new Handler(Looper.getMainLooper());

    private CloudinaryUtil() {
    }

    // pasta: ex. "atividades/<disciplinaId>" ou "entregas/<atividadeId>"
    static void enviar(ContentResolver resolver, Uri uri, String nomeArquivo,
                       String pasta, Callback callback) {

        executor.execute(() -> {
            try {
                String url = enviarSincrono(resolver, uri, nomeArquivo, pasta);
                principal.post(() -> callback.onSucesso(url));
            } catch (Exception e) {
                String mensagem = e.getMessage() != null ? e.getMessage() : e.toString();
                principal.post(() -> callback.onErro(mensagem));
            }
        });
    }

    private static String enviarSincrono(ContentResolver resolver, Uri uri,
                                         String nomeArquivo, String pasta) throws Exception {

        String mime = resolver.getType(uri);
        if (mime == null) {
            mime = "application/octet-stream";
        }

        // Imagens e vídeos vão como "image"/"video"; PDFs, DOCX etc. vão como "raw",
        // pois contas gratuitas bloqueiam a entrega de PDF enviado como imagem.
        String tipoRecurso;
        if (mime.startsWith("image/") && !mime.equals("image/svg+xml")) {
            tipoRecurso = "image";
        } else if (mime.startsWith("video/") || mime.startsWith("audio/")) {
            tipoRecurso = "video";
        } else {
            tipoRecurso = "raw";
        }

        if (nomeArquivo == null || nomeArquivo.isEmpty()) {
            nomeArquivo = "arquivo";
        }

        // Para "raw" o public_id precisa manter a extensão para o arquivo abrir corretamente
        String nomeSeguro = nomeArquivo.replaceAll("[^A-Za-z0-9._-]", "_");
        if (!tipoRecurso.equals("raw")) {
            int ponto = nomeSeguro.lastIndexOf('.');
            if (ponto > 0) {
                nomeSeguro = nomeSeguro.substring(0, ponto);
            }
        }
        String publicId = System.currentTimeMillis() + "_" + nomeSeguro;

        byte[] conteudo = lerBytes(resolver, uri);

        // Garante o limite mesmo quando o tamanho não pôde ser lido na seleção do arquivo
        if (conteudo.length > ArquivoUtil.LIMITE_TAMANHO_BYTES) {
            throw new Exception("o arquivo deve ter até 3 MB.");
        }

        String limite = "----MeuFuturoAgora" + System.currentTimeMillis();
        URL endpoint = new URL("https://api.cloudinary.com/v1_1/" + CLOUD_NAME
                + "/" + tipoRecurso + "/upload");

        HttpURLConnection conexao = (HttpURLConnection) endpoint.openConnection();
        try {
            conexao.setDoOutput(true);
            conexao.setRequestMethod("POST");
            conexao.setConnectTimeout(20000);
            conexao.setReadTimeout(60000);
            conexao.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + limite);

            try (DataOutputStream saida = new DataOutputStream(conexao.getOutputStream())) {
                escreverCampo(saida, limite, "upload_preset", UPLOAD_PRESET);
                escreverCampo(saida, limite, "folder", pasta);
                escreverCampo(saida, limite, "public_id", publicId);

                saida.write(("--" + limite + "\r\n").getBytes(StandardCharsets.UTF_8));
                saida.write(("Content-Disposition: form-data; name=\"file\"; filename=\""
                        + nomeSeguro + "\"\r\n").getBytes(StandardCharsets.UTF_8));
                saida.write(("Content-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.UTF_8));
                saida.write(conteudo);
                saida.write(("\r\n--" + limite + "--\r\n").getBytes(StandardCharsets.UTF_8));
            }

            int codigo = conexao.getResponseCode();
            InputStream resposta = codigo >= 200 && codigo < 300
                    ? conexao.getInputStream()
                    : conexao.getErrorStream();

            String corpo = resposta != null
                    ? new String(lerTudo(resposta), StandardCharsets.UTF_8)
                    : "";
            JSONObject json = corpo.isEmpty() ? new JSONObject() : new JSONObject(corpo);

            if (codigo < 200 || codigo >= 300) {
                JSONObject erro = json.optJSONObject("error");
                throw new Exception(erro != null
                        ? erro.optString("message", "HTTP " + codigo)
                        : "HTTP " + codigo);
            }

            return json.getString("secure_url");

        } finally {
            conexao.disconnect();
        }
    }

    private static void escreverCampo(DataOutputStream saida, String limite,
                                      String nome, String valor) throws Exception {
        saida.write(("--" + limite + "\r\n").getBytes(StandardCharsets.UTF_8));
        saida.write(("Content-Disposition: form-data; name=\"" + nome + "\"\r\n\r\n")
                .getBytes(StandardCharsets.UTF_8));
        saida.write(valor.getBytes(StandardCharsets.UTF_8));
        saida.write("\r\n".getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] lerBytes(ContentResolver resolver, Uri uri) throws Exception {
        try (InputStream entrada = resolver.openInputStream(uri)) {
            if (entrada == null) {
                throw new Exception("Não foi possível ler o arquivo selecionado.");
            }
            return lerTudo(entrada);
        }
    }

    private static byte[] lerTudo(InputStream entrada) throws Exception {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] bloco = new byte[8192];
        int lidos;
        while ((lidos = entrada.read(bloco)) != -1) {
            buffer.write(bloco, 0, lidos);
        }
        return buffer.toByteArray();
    }
}
