package br.com.example.meufuturoagora;

// As telas buscam os dados em segundo plano (Tarefas), e o Espresso só espera
// a thread principal ficar livre. Isto repete a verificação até passar ou até
// o tempo acabar, e então mostra a última falha.
final class Espera {

    private static final long LIMITE_MS = 5000;

    private Espera() {
    }

    static void ate(Runnable verificacao) {

        long fim = System.currentTimeMillis() + LIMITE_MS;
        Throwable ultima;

        do {
            try {
                verificacao.run();
                return;
            } catch (Throwable t) {
                ultima = t;
            }

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }

        } while (System.currentTimeMillis() < fim);

        if (ultima instanceof RuntimeException) {
            throw (RuntimeException) ultima;
        }

        throw new AssertionError(ultima);
    }
}
