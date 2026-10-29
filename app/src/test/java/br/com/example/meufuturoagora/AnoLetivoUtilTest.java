package br.com.example.meufuturoagora;

import static br.com.example.meufuturoagora.DocumentoFalso.mapa;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.Before;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AnoLetivoUtilTest {

    private static final long UM_DIA = 24L * 60 * 60 * 1000;

    @Before
    public void limparEstado() throws Exception {
        EstadoAnoLetivo.limpar();
    }

    private static String texto(Date data) {
        return new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(data);
    }

    // Configuração em que hoje cai no bimestre indicado (os outros ficam no passado)
    private static DocumentSnapshot configuracaoComHojeNoBimestre(int numero) {

        long hoje = System.currentTimeMillis();
        Map<String, Object> dados = new HashMap<>();
        dados.put("anoLetivo", "2026");

        for (int i = 1; i <= 4; i++) {

            long inicio = i == numero ? hoje - 10 * UM_DIA : hoje - (100 - i * 20) * UM_DIA;
            long fim = i == numero ? hoje + 10 * UM_DIA : inicio + 5 * UM_DIA;

            dados.put("bimestre" + i + "Inicio", texto(new Date(inicio)));
            dados.put("bimestre" + i + "Fim", texto(new Date(fim)));
        }

        return DocumentoFalso.de("geral", dados);
    }

    @Test
    public void semConfiguracaoUsaOAnoDoCalendario() {
        assertEquals(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)), AnoLetivoUtil.atual());
        assertEquals(0, AnoLetivoUtil.bimestreAtual());
    }

    @Test
    public void atualizarUsaOAnoLetivoDaConfiguracao() {
        AnoLetivoUtil.atualizar(DocumentoFalso.de("geral", mapa("anoLetivo", "2031")));

        assertEquals("2031", AnoLetivoUtil.atual());
    }

    @Test
    public void atualizarIgnoraDocumentoNuloOuInexistente() {
        AnoLetivoUtil.definirAnoAtual("2030");

        AnoLetivoUtil.atualizar(null);
        AnoLetivoUtil.atualizar(DocumentoFalso.inexistente("geral"));

        assertEquals("2030", AnoLetivoUtil.atual());
    }

    @Test
    public void anoVazioNaConfiguracaoMantemOAnoAnterior() {
        AnoLetivoUtil.definirAnoAtual("2030");

        AnoLetivoUtil.atualizar(DocumentoFalso.de("geral", mapa("anoLetivo", "")));

        assertEquals("2030", AnoLetivoUtil.atual());
    }

    @Test
    public void bimestreAtualVemDaConfiguracao() {
        AnoLetivoUtil.atualizar(configuracaoComHojeNoBimestre(3));

        assertEquals(3, AnoLetivoUtil.bimestreAtual());
    }

    @Test
    public void marcarGravaAnoEBimestre() {
        AnoLetivoUtil.atualizar(configuracaoComHojeNoBimestre(2));
        Map<String, Object> dados = new HashMap<>();

        AnoLetivoUtil.marcar(dados);

        assertEquals("2026", dados.get("anoLetivo"));
        assertEquals(2, dados.get("bimestre"));
    }

    @Test
    public void marcarForaDeBimestreNaoGravaBimestre() {
        AnoLetivoUtil.definirAnoAtual("2026");
        Map<String, Object> dados = new HashMap<>();

        AnoLetivoUtil.marcar(dados);

        assertEquals("2026", dados.get("anoLetivo"));
        assertFalse(dados.containsKey("bimestre"));
    }
}
