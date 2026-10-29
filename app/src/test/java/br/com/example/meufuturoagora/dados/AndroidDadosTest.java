package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;

import org.robolectric.RuntimeEnvironment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.security.GeneralSecurityException;
import java.util.concurrent.atomic.AtomicReference;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

// Partes que dependem do Android: armazenamento da sessão (SharedPreferences),
// Tarefas (thread principal) e Servicos. O Android Keystore não existe no
// Robolectric, então a cifra aqui é AES-GCM de software, com o mesmo formato.
@RunWith(RobolectricTestRunner.class)
public class AndroidDadosTest {

    private SharedPreferences preferencias;
    private CifraSoftware cifra;

    @Before
    public void preparar() throws Exception {
        Context context = RuntimeEnvironment.getApplication();
        preferencias = context.getSharedPreferences("teste_sessao", Context.MODE_PRIVATE);
        preferencias.edit().clear().commit();
        cifra = new CifraSoftware();
        Tarefas.usarExecutores(Runnable::run, Runnable::run);
    }

    @After
    public void restaurar() {
        Servicos.substituir(null);
    }

    // ===== ArmazenamentoSessaoPreferencias =====

    @Test
    public void salvaCifradoELeDeVolta() {
        ArmazenamentoSessaoPreferencias armazenamento = new ArmazenamentoSessaoPreferencias(preferencias, cifra);

        armazenamento.salvar("token-de-renovacao-secreto");

        String gravado = preferencias.getString(ArmazenamentoSessaoPreferencias.CHAVE, null);
        assertNotNull(gravado);
        assertFalse("o token não fica em texto puro no aparelho", gravado.contains("token-de-renovacao-secreto"));
        assertEquals("token-de-renovacao-secreto", armazenamento.ler());
    }

    @Test
    public void semNadaSalvoLeNulo() {
        assertNull(new ArmazenamentoSessaoPreferencias(preferencias, cifra).ler());
    }

    @Test
    public void sessaoCorrompidaEApagada() {
        preferencias.edit().putString(ArmazenamentoSessaoPreferencias.CHAVE, "não é base64!").commit();
        ArmazenamentoSessaoPreferencias armazenamento = new ArmazenamentoSessaoPreferencias(preferencias, cifra);

        assertNull(armazenamento.ler());
        assertFalse(preferencias.contains(ArmazenamentoSessaoPreferencias.CHAVE));
    }

    @Test
    public void sessaoDeOutraChaveEApagada() throws Exception {
        new ArmazenamentoSessaoPreferencias(preferencias, cifra).salvar("token");

        // Outra chave: como se o app tivesse sido reinstalado ou o arquivo copiado
        ArmazenamentoSessaoPreferencias outro = new ArmazenamentoSessaoPreferencias(preferencias, new CifraSoftware());

        assertNull(outro.ler());
        assertFalse(preferencias.contains(ArmazenamentoSessaoPreferencias.CHAVE));
    }

    @Test
    public void semCifraDisponivelNaoGuardaNada() {
        ArmazenamentoSessaoPreferencias armazenamento = new ArmazenamentoSessaoPreferencias(preferencias, new CifraSessao() {
            @Override
            public byte[] cifrar(byte[] texto) throws GeneralSecurityException {
                throw new GeneralSecurityException("sem keystore");
            }

            @Override
            public byte[] decifrar(byte[] cifrado) throws GeneralSecurityException {
                throw new GeneralSecurityException("sem keystore");
            }
        });

        armazenamento.salvar("token");

        assertFalse(preferencias.contains(ArmazenamentoSessaoPreferencias.CHAVE));
    }

    @Test
    public void apagarRemove() {
        ArmazenamentoSessaoPreferencias armazenamento = new ArmazenamentoSessaoPreferencias(preferencias, cifra);
        armazenamento.salvar("token");

        armazenamento.apagar();

        assertNull(armazenamento.ler());
    }

    // ===== Tarefas =====

    @Test
    public void tarefaEntregaOResultado() {
        AtomicReference<String> resultado = new AtomicReference<>();

        Tarefas.executar(null, () -> "ok", retorno(resultado));

        assertEquals("ok", resultado.get());
    }

    @Test
    public void tarefaEntregaOErro() {
        AtomicReference<String> resultado = new AtomicReference<>();

        Tarefas.executar(null, () -> {
            throw ErroApi.sessaoExpirada();
        }, retorno(resultado));

        assertEquals("erro: Sua sessão expirou. Entre de novo.", resultado.get());
    }

    @Test
    public void excecaoInesperadaViraErroApi() {
        AtomicReference<String> resultado = new AtomicReference<>();

        Tarefas.executar(null, () -> {
            throw new IllegalStateException("bug");
        }, retorno(resultado));

        assertEquals("erro: Algo deu errado. Tente de novo.", resultado.get());
    }

    @Test
    public void telaFechadaNaoRecebeOResultado() {
        AtomicReference<String> resultado = new AtomicReference<>();
        Activity fechando = mock(Activity.class);
        when(fechando.isFinishing()).thenReturn(true);
        Activity destruida = mock(Activity.class);
        when(destruida.isDestroyed()).thenReturn(true);
        Activity aberta = mock(Activity.class);

        Tarefas.executar(fechando, () -> "não chega", retorno(resultado));
        Tarefas.executar(destruida, () -> "não chega", retorno(resultado));
        assertNull(resultado.get());

        Tarefas.executar(aberta, () -> "chega", retorno(resultado));
        assertEquals("chega", resultado.get());
    }

    // ===== Servicos =====

    @Test
    public void servicosMontaTudoEPodeSerSubstituido() {
        Context context = RuntimeEnvironment.getApplication();

        Servicos reais = Servicos.get(context);

        assertNotNull(reais.auth);
        assertNotNull(reais.perfis);
        assertNotNull(reais.escola);
        assertNotNull(reais.disciplinas);
        assertNotNull(reais.atividades);
        assertNotNull(reais.frequencias);
        assertNotNull(reais.ranking);
        assertNotNull(reais.diario);
        assertNotNull(reais.novidades);
        assertNotNull(reais.arquivos);
        assertSame("é sempre a mesma instância", reais, Servicos.get(context));

        Servicos falsos = new Servicos(null, null, null, null, null, null, null, null, null, null);
        Servicos.substituir(falsos);
        assertSame(falsos, Servicos.get(context));
    }

    private static Tarefas.Retorno<String> retorno(AtomicReference<String> destino) {
        return new Tarefas.Retorno<String>() {
            @Override
            public void sucesso(String resultado) {
                destino.set(resultado);
            }

            @Override
            public void erro(ErroApi erro) {
                destino.set("erro: " + erro.getMessage());
            }
        };
    }

    // AES-GCM de software no mesmo formato da CifraKeystore (tamanho do IV + IV + dados)
    private static final class CifraSoftware implements CifraSessao {

        private final SecretKey chave;

        CifraSoftware() throws GeneralSecurityException {
            KeyGenerator gerador = KeyGenerator.getInstance("AES");
            gerador.init(256);
            chave = gerador.generateKey();
        }

        @Override
        public byte[] cifrar(byte[] texto) throws GeneralSecurityException {
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.ENCRYPT_MODE, chave);
            byte[] iv = c.getIV();
            byte[] dados = c.doFinal(texto);
            byte[] saida = new byte[1 + iv.length + dados.length];
            saida[0] = (byte) iv.length;
            System.arraycopy(iv, 0, saida, 1, iv.length);
            System.arraycopy(dados, 0, saida, 1 + iv.length, dados.length);
            return saida;
        }

        @Override
        public byte[] decifrar(byte[] cifrado) throws GeneralSecurityException {
            int tamanhoIv = cifrado[0];
            Cipher c = Cipher.getInstance("AES/GCM/NoPadding");
            c.init(Cipher.DECRYPT_MODE, chave, new GCMParameterSpec(128, cifrado, 1, tamanhoIv));
            return c.doFinal(cifrado, 1 + tamanhoIv, cifrado.length - 1 - tamanhoIv);
        }
    }

}
