package br.com.example.meufuturoagora;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
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

    private void tipoETamanho(String tipo, long tamanho) {
        when(resolver.getType(uri)).thenReturn(tipo);
        when(cursor.moveToFirst()).thenReturn(true);
        when(cursor.getColumnIndex(OpenableColumns.SIZE)).thenReturn(1);
        when(cursor.isNull(1)).thenReturn(false);
        when(cursor.getLong(1)).thenReturn(tamanho);
    }

    @Test
    public void validarAceitaPdfEImagensAteOLimite() {
        for (String tipo : new String[]{"application/pdf", "image/jpeg", "image/png", "image/webp"}) {
            tipoETamanho(tipo, ArquivoUtil.LIMITE_TAMANHO_BYTES);
            assertNull(tipo, ArquivoUtil.validar(resolver, uri));
        }
    }

    @Test
    public void validarRecusaOutrosTiposESemTipo() {
        for (String tipo : new String[]{"application/zip", "text/html", "video/mp4", null}) {
            tipoETamanho(tipo, 10);
            assertNotNull(String.valueOf(tipo), ArquivoUtil.validar(resolver, uri));
        }
    }

    @Test
    public void validarRecusaArquivoMaiorQueOLimite() {
        tipoETamanho("application/pdf", ArquivoUtil.LIMITE_TAMANHO_BYTES + 1);

        assertEquals("O arquivo deve ter até 3 MB.", ArquivoUtil.validar(resolver, uri));
    }

    @Test
    public void lerDevolveOConteudo() throws Exception {
        when(resolver.openInputStream(uri)).thenReturn(new java.io.ByteArrayInputStream(new byte[]{1, 2, 3}));

        assertArrayEquals(new byte[]{1, 2, 3}, ArquivoUtil.ler(resolver, uri));
    }

    @Test(expected = java.io.IOException.class)
    public void lerRecusaArquivoMaiorQueOLimiteMesmoSemTamanhoInformado() throws Exception {
        byte[] grande = new byte[(int) ArquivoUtil.LIMITE_TAMANHO_BYTES + 1];
        when(resolver.openInputStream(uri)).thenReturn(new java.io.ByteArrayInputStream(grande));

        ArquivoUtil.ler(resolver, uri);
    }

    @Test(expected = java.io.IOException.class)
    public void lerSemFluxoFalha() throws Exception {
        when(resolver.openInputStream(uri)).thenReturn(null);

        ArquivoUtil.ler(resolver, uri);
    }
}
