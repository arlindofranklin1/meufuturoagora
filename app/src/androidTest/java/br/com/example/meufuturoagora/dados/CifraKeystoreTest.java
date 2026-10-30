package br.com.example.meufuturoagora.dados;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.Arrays;

// O Android Keystore só existe no aparelho (ou emulador): por isso este teste
// é instrumentado, e não JVM como os outros da camada de dados
@RunWith(AndroidJUnit4.class)
public class CifraKeystoreTest {

    private final CifraKeystore cifra = new CifraKeystore();

    @Test
    public void cifraEDecifraDeVolta() throws Exception {
        byte[] texto = "token-de-renovacao".getBytes(StandardCharsets.UTF_8);

        byte[] cifrado = cifra.cifrar(texto);

        assertFalse(Arrays.equals(texto, cifrado));
        assertArrayEquals(texto, cifra.decifrar(cifrado));
    }

    @Test
    public void cadaCifragemUsaUmIvNovo() throws Exception {
        byte[] texto = "mesmo texto".getBytes(StandardCharsets.UTF_8);

        assertFalse(Arrays.equals(cifra.cifrar(texto), cifra.cifrar(texto)));
    }

    @Test
    public void dadoAlteradoNaoDecifra() throws Exception {
        byte[] cifrado = cifra.cifrar("token".getBytes(StandardCharsets.UTF_8));
        cifrado[cifrado.length - 1] ^= 1;

        try {
            cifra.decifrar(cifrado);
            fail("O GCM precisa recusar dados alterados");
        } catch (GeneralSecurityException esperado) {
            // ok
        }
    }

    @Test
    public void dadosIncompletosSaoRecusados() {
        for (byte[] invalido : new byte[][]{new byte[0], new byte[]{0, 1}, new byte[]{(byte) 200, 1, 2}}) {
            try {
                cifra.decifrar(invalido);
                fail("Dados inválidos precisam ser recusados");
            } catch (GeneralSecurityException esperado) {
                // ok
            }
        }
    }
}
