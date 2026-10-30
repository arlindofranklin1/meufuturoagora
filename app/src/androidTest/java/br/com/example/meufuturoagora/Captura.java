package br.com.example.meufuturoagora;

import android.graphics.Bitmap;

import androidx.test.platform.app.InstrumentationRegistry;

import java.io.File;
import java.io.FileOutputStream;

// Captura da tela durante os testes de ponta a ponta, para o QA visual.
// Fica em /sdcard/Android/data/<pacote>/files/qa/ (adb pull para ver).
final class Captura {

    private Captura() {
    }

    static void salvar(String nome) {

        // Dá tempo de a tela terminar de desenhar
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        Bitmap tela = InstrumentationRegistry.getInstrumentation().getUiAutomation().takeScreenshot();

        if (tela == null) {
            return;
        }

        File pasta = new File(InstrumentationRegistry.getInstrumentation().getTargetContext()
                .getExternalFilesDir(null), "qa");

        if (!pasta.exists() && !pasta.mkdirs()) {
            return;
        }

        try (FileOutputStream saida = new FileOutputStream(new File(pasta, nome + ".png"))) {
            tela.compress(Bitmap.CompressFormat.PNG, 100, saida);
        } catch (Exception ignorado) {
            // Sem captura o teste continua valendo
        }
    }
}
