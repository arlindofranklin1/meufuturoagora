package br.com.example.meufuturoagora;

import static br.com.example.meufuturoagora.DocumentoFalso.mapa;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Test;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

// Caracterização: descreve o comportamento atual, inclusive as esquisitices,
// para a versão no servidor (Fase 3) seguir as mesmas regras.
public class BimestreUtilTest {

    // Bimestres de 2026 sem buracos, exceto o recesso de julho (01/07 a 31/07)
    private static final DocumentSnapshot BIMESTRES_2026 = DocumentoFalso.de("geral", mapa(
            "anoLetivo", "2026",
            "bimestre1Inicio", "02/02/2026", "bimestre1Fim", "30/04/2026",
            "bimestre2Inicio", "01/05/2026", "bimestre2Fim", "30/06/2026",
            "bimestre3Inicio", "01/08/2026", "bimestre3Fim", "30/09/2026",
            "bimestre4Inicio", "01/10/2026", "bimestre4Fim", "18/12/2026"
    ));

    private static Date data(String texto) throws ParseException {
        return new SimpleDateFormat("dd/MM/yyyy HH:mm:ss.SSS", Locale.getDefault()).parse(texto);
    }

    // ===== converter =====

    @Test
    public void converterLeDataNoFormatoBrasileiro() throws ParseException {
        assertEquals(data("15/03/2026 00:00:00.000"), BimestreUtil.converter("15/03/2026"));
    }

    @Test
    public void converterDevolveNuloParaTextoVazioNuloOuInvalido() {
        assertNull(BimestreUtil.converter(""));
        assertNull(BimestreUtil.converter(null));
        assertNull(BimestreUtil.converter("abc"));
    }

    @Test
    public void converterAceitaDiaEMesSemZeroAEsquerda() throws ParseException {
        assertEquals(data("01/02/2026 00:00:00.000"), BimestreUtil.converter("1/2/2026"));
    }

    @Test
    public void converterEhTolerante31DeFevereiroViraMarco() throws ParseException {
        // SimpleDateFormat é leniente por padrão: datas impossíveis "transbordam"
        assertEquals(data("03/03/2026 00:00:00.000"), BimestreUtil.converter("31/02/2026"));
    }

    // ===== fimDoDia =====

    @Test
    public void fimDoDiaEhOUltimoMilissegundoDoDia() throws ParseException {
        assertEquals(
                data("30/04/2026 23:59:59.999"),
                BimestreUtil.fimDoDia(data("30/04/2026 00:00:00.000"))
        );
    }

    @Test
    public void fimDoDiaDeNuloEhNulo() {
        assertNull(BimestreUtil.fimDoDia(null));
    }

    // ===== numeroDaData =====

    @Test
    public void numeroDaDataEncontraCadaBimestre() throws ParseException {
        assertEquals(1, BimestreUtil.numeroDaData(BIMESTRES_2026, data("15/03/2026 10:00:00.000")));
        assertEquals(2, BimestreUtil.numeroDaData(BIMESTRES_2026, data("15/06/2026 10:00:00.000")));
        assertEquals(3, BimestreUtil.numeroDaData(BIMESTRES_2026, data("15/08/2026 10:00:00.000")));
        assertEquals(4, BimestreUtil.numeroDaData(BIMESTRES_2026, data("15/12/2026 10:00:00.000")));
    }

    @Test
    public void primeiroEUltimoDiaContamPorInteiro() throws ParseException {
        assertEquals(1, BimestreUtil.numeroDaData(BIMESTRES_2026, data("02/02/2026 00:00:00.000")));
        assertEquals(1, BimestreUtil.numeroDaData(BIMESTRES_2026, data("30/04/2026 23:59:59.999")));
    }

    @Test
    public void foraDosBimestresEhZero() throws ParseException {
        assertEquals(0, BimestreUtil.numeroDaData(BIMESTRES_2026, data("01/02/2026 23:59:59.999")));
        assertEquals(0, BimestreUtil.numeroDaData(BIMESTRES_2026, data("15/07/2026 10:00:00.000")));
        assertEquals(0, BimestreUtil.numeroDaData(BIMESTRES_2026, data("19/12/2026 00:00:00.000")));
    }

    @Test
    public void semDocumentoOuSemDataEhZero() throws ParseException {
        assertEquals(0, BimestreUtil.numeroDaData(null, data("15/03/2026 10:00:00.000")));
        assertEquals(0, BimestreUtil.numeroDaData(
                DocumentoFalso.inexistente("geral"), data("15/03/2026 10:00:00.000")));
        assertEquals(0, BimestreUtil.numeroDaData(BIMESTRES_2026, null));
    }

    @Test
    public void bimestreIncompletoEhIgnorado() throws ParseException {
        DocumentSnapshot soInicio = DocumentoFalso.de("geral", mapa(
                "bimestre1Inicio", "02/02/2026", "bimestre1Fim", "",
                "bimestre2Inicio", "01/05/2026", "bimestre2Fim", "30/06/2026"
        ));

        assertEquals(0, BimestreUtil.numeroDaData(soInicio, data("15/03/2026 10:00:00.000")));
        assertEquals(2, BimestreUtil.numeroDaData(soInicio, data("15/06/2026 10:00:00.000")));
    }

    @Test
    public void bimestresSobrepostosValeOPrimeiro() throws ParseException {
        DocumentSnapshot sobrepostos = DocumentoFalso.de("geral", mapa(
                "bimestre1Inicio", "01/03/2026", "bimestre1Fim", "31/05/2026",
                "bimestre2Inicio", "01/05/2026", "bimestre2Fim", "30/06/2026"
        ));

        assertEquals(1, BimestreUtil.numeroDaData(sobrepostos, data("15/05/2026 10:00:00.000")));
    }

    // ===== dataNoAno =====

    @Test
    public void dataNoAnoValeOPeriodoDosBimestres() throws ParseException {
        assertTrue(BimestreUtil.dataNoAno(data("02/02/2026 00:00:00.000"), "2026", BIMESTRES_2026));
        assertTrue(BimestreUtil.dataNoAno(data("18/12/2026 23:59:59.999"), "2026", BIMESTRES_2026));

        // O recesso fica entre o 1º início e o último fim, então conta
        assertTrue(BimestreUtil.dataNoAno(data("15/07/2026 10:00:00.000"), "2026", BIMESTRES_2026));

        // Mesmo ano do calendário, mas fora do período: não conta
        assertFalse(BimestreUtil.dataNoAno(data("20/01/2026 10:00:00.000"), "2026", BIMESTRES_2026));
        assertFalse(BimestreUtil.dataNoAno(data("20/12/2026 10:00:00.000"), "2026", BIMESTRES_2026));
    }

    @Test
    public void comBimestresOAnoInformadoNaoImporta() throws ParseException {
        // O parâmetro "ano" só é usado quando não há bimestres
        assertTrue(BimestreUtil.dataNoAno(data("15/03/2026 10:00:00.000"), "1999", BIMESTRES_2026));
    }

    @Test
    public void semBimestresValeOAnoDoCalendario() throws ParseException {
        DocumentSnapshot vazio = DocumentoFalso.de("geral", mapa("anoLetivo", "2026"));

        assertTrue(BimestreUtil.dataNoAno(data("01/01/2026 00:00:00.000"), "2026", vazio));
        assertFalse(BimestreUtil.dataNoAno(data("31/12/2025 23:59:59.999"), "2026", vazio));
        assertTrue(BimestreUtil.dataNoAno(data("15/03/2026 10:00:00.000"), "2026", null));
        assertTrue(BimestreUtil.dataNoAno(
                data("15/03/2026 10:00:00.000"), "2026", DocumentoFalso.inexistente("geral")));
    }

    @Test
    public void periodoParcialUsaOsBimestresDefinidos() throws ParseException {
        DocumentSnapshot doisBimestres = DocumentoFalso.de("geral", mapa(
                "bimestre1Inicio", "02/02/2026", "bimestre1Fim", "30/04/2026",
                "bimestre2Inicio", "01/05/2026", "bimestre2Fim", "30/06/2026"
        ));

        assertTrue(BimestreUtil.dataNoAno(data("30/06/2026 12:00:00.000"), "2026", doisBimestres));
        assertFalse(BimestreUtil.dataNoAno(data("01/07/2026 00:00:00.000"), "2026", doisBimestres));
    }

    @Test
    public void dataNoAnoSemDataOuSemAnoEhFalso() throws ParseException {
        assertFalse(BimestreUtil.dataNoAno(null, "2026", BIMESTRES_2026));
        assertFalse(BimestreUtil.dataNoAno(data("15/03/2026 10:00:00.000"), null, BIMESTRES_2026));
    }
}
