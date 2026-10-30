package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

// Validação das datas dos bimestres na tela (o servidor confere de novo)
public class BimestresTest {

    private static Bimestres.Entrada e(String inicio, String fim) {
        return new Bimestres.Entrada(inicio, fim);
    }

    private static final Bimestres.Entrada VAZIO = e("", "");

    private static List<Bimestres.Entrada> quatro(Bimestres.Entrada b1, Bimestres.Entrada b2,
                                                  Bimestres.Entrada b3, Bimestres.Entrada b4) {
        return Arrays.asList(b1, b2, b3, b4);
    }

    // ===== ler / formatar =====

    @Test
    public void leEFormataNoPadraoBrasileiro() {
        assertEquals(LocalDate.of(2026, 3, 15), Bimestres.ler("15/03/2026"));
        assertEquals(LocalDate.of(2026, 3, 15), Bimestres.ler("  15/03/2026 "));
        assertEquals("05/02/2026", Bimestres.formatar(LocalDate.of(2026, 2, 5)));
        assertEquals("", Bimestres.formatar(null));
    }

    @Test
    public void textoVazioOuNuloNaoEData() {
        assertNull(Bimestres.ler(""));
        assertNull(Bimestres.ler("   "));
        assertNull(Bimestres.ler(null));
    }

    @Test
    public void dataImpossivelEhRecusadaSemVirarOMesSeguinte() {
        // O SimpleDateFormat antigo aceitava "31/02/2026" e virava 03/03 (ver BimestreUtilTest)
        for (String invalida : new String[]{"31/02/2026", "32/01/2026", "15/13/2026", "1/2/2026", "2026-03-15", "abc"}) {
            try {
                Bimestres.ler(invalida);
                fail("Devia recusar: " + invalida);
            } catch (IllegalArgumentException esperado) {
                // ok
            }
        }
    }

    @Test
    public void anoBissextoValeSoQuandoExiste() {
        assertEquals(LocalDate.of(2028, 2, 29), Bimestres.ler("29/02/2028"));

        try {
            Bimestres.ler("29/02/2026");
            fail("2026 não é bissexto");
        } catch (IllegalArgumentException esperado) {
            // ok
        }
    }

    // ===== validar =====

    @Test
    public void quatroBimestresValidosViramUmaListaNumerada() {
        Bimestres.Resultado r = Bimestres.validar(2026, quatro(
                e("02/02/2026", "30/04/2026"), e("04/05/2026", "10/07/2026"),
                e("03/08/2026", "30/09/2026"), e("01/10/2026", "18/12/2026")));

        assertTrue(r.ok());
        assertEquals(4, r.bimestres.size());
        assertEquals(2026, r.bimestres.get(0).ano);
        assertEquals(3, r.bimestres.get(2).numero);
        assertEquals(LocalDate.of(2026, 8, 3), r.bimestres.get(2).inicio);
    }

    @Test
    public void bimestreNaoPreenchidoFicaIndefinido() {
        Bimestres.Resultado r = Bimestres.validar(2026, quatro(
                e("02/02/2026", "30/04/2026"), VAZIO, e("03/08/2026", "30/09/2026"), VAZIO));

        assertTrue(r.ok());
        assertEquals(2, r.bimestres.size());
        assertEquals("o número vem da posição na tela, não da ordem na lista", 3, r.bimestres.get(1).numero);
    }

    @Test
    public void tudoVazioEhValidoEApagaOsBimestres() {
        Bimestres.Resultado r = Bimestres.validar(2026, quatro(VAZIO, VAZIO, VAZIO, VAZIO));

        assertTrue(r.ok());
        assertTrue(r.bimestres.isEmpty());
    }

    @Test
    public void inicioSemFimOuFimSemInicioEhRecusado() {
        Bimestres.Resultado soInicio = Bimestres.validar(2026, quatro(e("02/02/2026", ""), VAZIO, VAZIO, VAZIO));
        Bimestres.Resultado soFim = Bimestres.validar(2026, quatro(VAZIO, e("", "10/07/2026"), VAZIO, VAZIO));

        assertFalse(soInicio.ok());
        assertEquals("Informe o início e o fim do 1º bimestre.", soInicio.erro);
        assertEquals("Informe o início e o fim do 2º bimestre.", soFim.erro);
    }

    @Test
    public void fimAntesDoInicioEhRecusado() {
        Bimestres.Resultado r = Bimestres.validar(2026, quatro(e("30/04/2026", "02/02/2026"), VAZIO, VAZIO, VAZIO));

        assertEquals("No 1º bimestre, o fim vem antes do início.", r.erro);
    }

    @Test
    public void umDiaSoEhUmBimestreValido() {
        assertTrue(Bimestres.validar(2026, quatro(e("02/02/2026", "02/02/2026"), VAZIO, VAZIO, VAZIO)).ok());
    }

    @Test
    public void dataInvalidaApontaOBimestre() {
        Bimestres.Resultado r = Bimestres.validar(2026, quatro(
                e("02/02/2026", "30/04/2026"), VAZIO, e("31/02/2026", "30/09/2026"), VAZIO));

        assertEquals("Confira as datas do 3º bimestre (dia/mês/ano).", r.erro);
    }

    @Test
    public void bimestresSobrepostosOuForaDeOrdemSaoRecusados() {
        Bimestres.Resultado sobrepostos = Bimestres.validar(2026, quatro(
                e("02/02/2026", "10/05/2026"), e("04/05/2026", "10/07/2026"), VAZIO, VAZIO));
        Bimestres.Resultado foraDeOrdem = Bimestres.validar(2026, quatro(
                e("03/08/2026", "30/09/2026"), e("02/02/2026", "30/04/2026"), VAZIO, VAZIO));

        assertTrue(sobrepostos.erro.startsWith("O 2º bimestre começa antes do fim do 1º."));
        assertTrue(foraDeOrdem.erro.startsWith("O 2º bimestre começa antes do fim do 1º."));
    }

    @Test
    public void fimDeUmIgualAoInicioDoProximoEhSobreposicao() {
        // O último dia do bimestre vale inteiro (BimestreUtil.fimDoDia): o dia não pode ser dos dois
        Bimestres.Resultado r = Bimestres.validar(2026, quatro(
                e("02/02/2026", "30/04/2026"), e("30/04/2026", "10/07/2026"), VAZIO, VAZIO));

        assertFalse(r.ok());
    }

    @Test
    public void diaSeguinteJuntoNaoEhSobreposicao() {
        assertTrue(Bimestres.validar(2026, quatro(
                e("02/02/2026", "30/04/2026"), e("01/05/2026", "10/07/2026"), VAZIO, VAZIO)).ok());
    }

    @Test
    public void ordemValeAteComBimestreIntermediarioVazio() {
        Bimestres.Resultado r = Bimestres.validar(2026, quatro(
                e("03/08/2026", "30/09/2026"), VAZIO, e("01/10/2026", "18/12/2026"), e("10/12/2026", "20/12/2026")));

        assertEquals("O 4º bimestre começa antes do fim do 3º. Os bimestres precisam estar em ordem e sem sobreposição.",
                r.erro);
    }
}
