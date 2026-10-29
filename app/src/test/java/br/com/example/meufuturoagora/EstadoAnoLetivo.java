package br.com.example.meufuturoagora;

import java.lang.reflect.Field;

// AnoLetivoUtil guarda o ano e a configuração em campos estáticos. Os testes
// limpam esse estado por reflexão para não depender da ordem de execução, sem
// precisar mudar a classe só para isso.
final class EstadoAnoLetivo {

    private EstadoAnoLetivo() {
    }

    static void limpar() throws Exception {

        for (String nome : new String[]{"anoAtual", "configuracao"}) {

            Field campo = AnoLetivoUtil.class.getDeclaredField(nome);
            campo.setAccessible(true);
            campo.set(null, null);
        }
    }
}
