package br.com.example.meufuturoagora;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.File;
import java.io.FileOutputStream;

// Tela para ajustar a foto de perfil escolhida em um recorte quadrado antes de enviar.
// Recebe a imagem em getData() e devolve (RESULT_OK) o arquivo recortado em getData().
public class RecortarFotoActivity extends AppCompatActivity {

    // Lado da foto final, em pixels
    private static final int TAMANHO_FINAL = 512;

    // Lado máximo da imagem carregada para o recorte (economiza memória)
    private static final int TAMANHO_EDICAO = 2048;

    private RecorteQuadradoView recorteView;
    private MaterialButton btnSalvar;

    static Intent criarIntent(Context context, Uri imagem) {
        return new Intent(context, RecortarFotoActivity.class).setData(imagem);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_recortar_foto);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        recorteView = findViewById(R.id.recorteView);
        btnSalvar = findViewById(R.id.btnSalvarRecorte);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());
        findViewById(R.id.btnCancelarRecorte).setOnClickListener(v -> finish());
        btnSalvar.setOnClickListener(v -> salvar());

        carregarImagem(getIntent().getData());
    }

    private void carregarImagem(Uri uri) {

        if (uri == null) {
            finish();
            return;
        }

        Handler principal = new Handler(Looper.getMainLooper());

        new Thread(() -> {

            try {

                Bitmap bitmap = ImageDecoder.decodeBitmap(
                        ImageDecoder.createSource(getContentResolver(), uri),
                        (decoder, info, origem) -> {

                            int largura = info.getSize().getWidth();
                            int altura = info.getSize().getHeight();
                            int maior = Math.max(largura, altura);

                            if (maior > TAMANHO_EDICAO) {

                                float escala = (float) TAMANHO_EDICAO / maior;
                                decoder.setTargetSize(
                                        Math.max(1, Math.round(largura * escala)),
                                        Math.max(1, Math.round(altura * escala))
                                );
                            }

                            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                        }
                );

                principal.post(() -> {

                    if (isDestroyed()) {
                        return;
                    }

                    findViewById(R.id.progressRecorte).setVisibility(View.GONE);
                    recorteView.setImagem(bitmap);
                    btnSalvar.setEnabled(true);
                });

            } catch (Exception e) {

                principal.post(() -> {
                    Toast.makeText(this, "Não foi possível abrir a imagem selecionada.", Toast.LENGTH_LONG).show();
                    finish();
                });
            }
        }).start();
    }

    private void salvar() {

        Bitmap recorte = recorteView.recortar(TAMANHO_FINAL);

        if (recorte == null) {
            return;
        }

        File arquivo = new File(getCacheDir(), "foto_perfil_recortada.jpg");

        try (FileOutputStream saida = new FileOutputStream(arquivo)) {

            recorte.compress(Bitmap.CompressFormat.JPEG, 90, saida);

        } catch (Exception e) {

            Toast.makeText(this, "Erro ao salvar o recorte.", Toast.LENGTH_LONG).show();
            return;
        }

        setResult(RESULT_OK, new Intent().setData(Uri.fromFile(arquivo)));
        finish();
    }
}
