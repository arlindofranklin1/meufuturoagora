package br.com.example.meufuturoagora;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

class ArquivoUtil {

    // Limite de tamanho para anexos (trilhas e entregas)
    static final long LIMITE_TAMANHO_BYTES = 3 * 1024 * 1024;

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
}
