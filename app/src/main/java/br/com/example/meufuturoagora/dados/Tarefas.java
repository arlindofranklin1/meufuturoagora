package br.com.example.meufuturoagora.dados;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

// Rede fora da thread principal, resultado de volta nela (como o CloudinaryUtil
// já fazia). Se a tela fechou enquanto a requisição corria, o resultado é
// descartado em vez de mexer numa Activity destruída.
public final class Tarefas {

    public interface Trabalho<T> {
        T executar() throws ErroApi;
    }

    public interface Retorno<T> {
        void sucesso(T resultado);

        void erro(ErroApi erro);
    }

    private static Executor fundo = Executors.newFixedThreadPool(4);
    private static Executor principal = new Executor() {
        private final Handler handler = new Handler(Looper.getMainLooper());

        @Override
        public void execute(Runnable tarefa) {
            handler.post(tarefa);
        }
    };

    private Tarefas() {
    }

    public static <T> void executar(Activity dona, Trabalho<T> trabalho, Retorno<T> retorno) {

        fundo.execute(() -> {

            T resultado;

            try {
                resultado = trabalho.executar();
            } catch (ErroApi e) {
                entregar(dona, () -> retorno.erro(e));
                return;
            } catch (RuntimeException e) {
                entregar(dona, () -> retorno.erro(ErroApi.inesperado(e)));
                return;
            }

            entregar(dona, () -> retorno.sucesso(resultado));
        });
    }

    // Testes: executores síncronos para não depender de threads
    public static void usarExecutores(Executor novoFundo, Executor novoPrincipal) {
        fundo = novoFundo;
        principal = novoPrincipal;
    }

    private static void entregar(Activity dona, Runnable acao) {
        principal.execute(() -> {
            if (dona == null || (!dona.isFinishing() && !dona.isDestroyed())) {
                acao.run();
            }
        });
    }
}
