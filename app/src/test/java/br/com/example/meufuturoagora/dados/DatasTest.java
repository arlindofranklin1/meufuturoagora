package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Test;

import java.time.LocalDate;

public class DatasTest {

    @Test
    public void leEFormataNoPadraoBrasileiro() {
        assertEquals(LocalDate.of(2026, 10, 29), Datas.ler("29/10/2026"));
        assertEquals(LocalDate.of(2026, 10, 29), Datas.ler("  29/10/2026 "));
        assertEquals("05/02/2026", Datas.formatar(LocalDate.of(2026, 2, 5)));
        assertEquals("", Datas.formatar(null));
    }

    @Test
    public void vazioNaoEData() {
        assertNull(Datas.ler(""));
        assertNull(Datas.ler("  "));
        assertNull(Datas.ler(null));
    }

    @Test
    public void dataImpossivelOuForaDoPadraoELancaErro() {
        for (String invalida : new String[]{"31/02/2026", "29/02/2026", "00/10/2026", "29/13/2026", "29-10-2026", "2026-10-29"}) {
            try {
                Datas.ler(invalida);
                fail("Devia recusar: " + invalida);
            } catch (IllegalArgumentException esperado) {
                // ok
            }
        }
    }

    @Test
    public void idaEVolta() {
        LocalDate data = LocalDate.of(2028, 2, 29);

        assertEquals(data, Datas.ler(Datas.formatar(data)));
    }
}
