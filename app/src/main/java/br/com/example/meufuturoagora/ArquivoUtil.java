package br.com.example.meufuturoagora;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import br.com.example.meufuturoagora.dados.Arquivos;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

class ArquivoUtil {

    // Limite de tamanho para anexos (trilhas e entregas)
    static final long LIMITE_TAMANHO_BYTES = 3 * 1024 * 1024;

    // Tipos que o seletor de arquivos oferece (decisão D5)
    static final String[] TIPOS_DO_SELETOR = Arquivos.TIPOS_PERMITIDOS.toArray(new String[0]);

    private ArquivoUtil() {
    }

    // Lê o tamanho do arquivo apontado pela URI. Retorna -1 se não for possível obter.
    static long obterTamanho(ContentResolver resolver, Uri uri) {

        Cursor cursor = resolver.query(uri, null, null, null, null);

        if (cursor == null) {
            return -1;
        }

        try {

            if (cursor.moveToFirst()) {

                int indice = cursor.getColumnIndex(OpenableColumns.SIZE);

                if (indice >= 0 && !cursor.isNull(indice)) {
                    return cursor.getLong(indice);
                }
            }

            return -1;

        } finally {
            cursor.close();
        }
    }

    // Nome que o usuário vê (só vai para o banco; no Storage o caminho usa UUID)
    static String obterNome(ContentResolver resolver, Uri uri) {

        String resultado = null;

        if ("content".equals(uri.getScheme())) {

            Cursor cursor = resolver.query(uri, null, null, null, null);

            try {

                if (cursor != null && cursor.moveToFirst()) {

                    int indice = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (indice >= 0) {
                        resultado = cursor.getString(indice);
                    }
                }

            } finally {

                if (cursor != null) {
                    cursor.close();
                }
            }
        }

        if (resultado == null) {
            resultado = uri.getLastPathSegment();
        }

        return resultado;
    }

    // Confere tipo e tamanho antes de aceitar o arquivo. Devolve a mensagem de
    // erro para mostrar ao usuário, ou null se o arquivo serve.
    static String validar(ContentResolver resolver, Uri uri) {

        if (!Arquivos.tipoPermitido(resolver.getType(uri))) {
            return "Envie um PDF ou uma imagem (JPEG, PNG ou WEBP).";
        }

        if (obterTamanho(resolver, uri) > LIMITE_TAMANHO_BYTES) {
            return "O arquivo deve ter até 3 MB.";
        }

        return null;
    }

    // O limite vale também para quem não informa o tamanho: lê no máximo 3 MB + 1
    static byte[] ler(ContentResolver resolver, Uri uri) throws IOException {

        try (InputStream entrada = resolver.openInputStream(uri)) {

            if (entrada == null) {
                throw new IOException("Não foi possível abrir o arquivo.");
            }

            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            byte[] bloco = new byte[8192];
            int lidos;

            while ((lidos = entrada.read(bloco)) != -1) {

                saida.write(bloco, 0, lidos);

                if (saida.size() > LIMITE_TAMANHO_BYTES) {
                    throw new IOException("O arquivo deve ter até 3 MB.");
                }
            }

            return saida.toByteArray();
        }
    }

    // Abre um arquivo do Storage pelo link assinado (curto), no app do aparelho
    static void abrir(Activity tela, String bucket, String caminho) {

        if (caminho == null || caminho.isEmpty()) {
            return;
        }

        Tarefas.executar(tela,
                () -> Servicos.get(tela).arquivos.urlAssinada(bucket, caminho, Arquivos.VALIDADE_LINK_SEGUNDOS),
                new Tarefas.Retorno<String>() {
                    @Override
                    public void sucesso(String url) {
                        tela.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Toast.makeText(tela, "Não foi possível abrir o arquivo.", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
