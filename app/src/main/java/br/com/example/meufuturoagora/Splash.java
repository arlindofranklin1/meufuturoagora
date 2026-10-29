package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class Splash extends AppCompatActivity {

    // Tempo mínimo da abertura, como antes; a sessão é conferida enquanto isso
    private static final long DURACAO_MINIMA_MS = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        long inicio = SystemClock.elapsedRealtime();

        // Sessão salva: renova e confere no servidor se o cadastro continua ativo.
        // Sem sessão, ou recusada, vai para a TelaEntrar.
        Tarefas.executar(this, () -> Servicos.get(this).auth.restaurarSessao(),
                new Tarefas.Retorno<Perfil>() {
                    @Override
                    public void sucesso(Perfil perfil) {
                        depoisDoTempoMinimo(inicio, () -> {
                            if (perfil != null) {
                                RotaInicial.abrir(Splash.this, perfil);
                            } else {
                                // Sem sessão no Supabase, a da ponte não vale
                                PonteFirebase.sair();
                                abrirTelaEntrar(null);
                            }
                        });
                    }

                    @Override
                    public void erro(ErroApi erro) {

                        if (erro.semPermissao()) {
                            PonteFirebase.sair();
                        }

                        depoisDoTempoMinimo(inicio, () -> abrirTelaEntrar(erro.getMessage()));
                    }
                });
    }

    private void depoisDoTempoMinimo(long inicio, Runnable acao) {

        long restante = DURACAO_MINIMA_MS - (SystemClock.elapsedRealtime() - inicio);

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            if (!isFinishing() && !isDestroyed()) {
                acao.run();
            }
        }, Math.max(0, restante));
    }

    private void abrirTelaEntrar(String mensagem) {

        Intent intent = new Intent(
                Splash.this,
                TelaEntrar.class
        );

        if (mensagem != null) {
            intent.putExtra(TelaEntrar.EXTRA_MENSAGEM, mensagem);
        }

        startActivity(intent);
        finish();
    }
}
