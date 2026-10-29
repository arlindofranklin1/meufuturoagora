package br.com.example.meufuturoagora;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.OpenableColumns;

import org.junit.Before;
import org.junit.Test;

public class ArquivoUtilTest {

    private ContentResolver resolver;
    private Uri uri;
    private Cursor cursor;

    @Before
    public void preparar() {
        resolver = mock(ContentResolver.class);
        uri = mock(Uri.class);
        cursor = mock(Cursor.class);
        when(resolver.query(uri, null, null, null, null)).thenReturn(cursor);
    }

    @Test
    public void limiteEh3MegabytesBinarios() {
        assertEquals(3_145_728L, ArquivoUtil.LIMITE_TAMANHO_BYTES);
    }

    @Test
    public void leOTamanhoDaColunaSize() {
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(2);
        when(cursor.isNull(2)).thenReturn(false);
        when(cursor.getLong(2)).thenReturn(1234L);

        assertEquals(1234L, ArquivoUtil.obterTamanho(resolver, uri));
        verify(cursor).close();
    }

    @Test
    public void semCursorDevolveMenosUm() {
        when(resolver.query(uri, null, null, null, null)).thenReturn(null);

        assertEquals(-1L, ArquivoUtil.obterTamanho(resolver, uri));
    }

    @Test
    public void cursorVazioDevolveMenosUmEFechaOCursor() {
        when(cursor.moveToFirst()).thenReturn(false);

        assertEquals(-1L, ArquivoUtil.obterTamanho(resolver, uri));
        verify(cursor).close();
    }

    @Test
    public void semColunaSizeDevolveMenosUm() {
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(-1);

        assertEquals(-1L, ArquivoUtil.obterTamanho(resolver, uri));
        verify(cursor).close();
    }

    @Test
    public void tamanhoNuloDevolveMenosUm() {
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(0);
        when(cursor.isNull(0)).thenReturn(true);

        assertEquals(-1L, ArquivoUtil.obterTamanho(resolver, uri));
        verify(cursor).close();
    }
}
