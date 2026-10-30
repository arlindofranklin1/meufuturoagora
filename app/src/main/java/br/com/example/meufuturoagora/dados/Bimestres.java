package br.com.example.meufuturoagora.dados;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Datas dos bimestres como a tela mostra (dd/MM/yyyy) e a validação que o
// servidor também faz (definir_bimestres): a tela avisa na hora, o servidor
// é quem garante.
public final class Bimestres {

    private Bimestres() {
    }

    public static String formatar(LocalDate data) {
        return Datas.formatar(data);
    }

    // Texto vazio = bimestre não definido (null); texto inválido lança IllegalArgumentException
    public static LocalDate ler(String texto) {
        return Datas.ler(texto);
    }

    // Uma linha da tela: início e fim em texto, como digitados
    public static final class Entrada {

        final String inicio;
        final String fim;

        public Entrada(String inicio, String fim) {
            this.inicio = inicio;
            this.fim = fim;
        }
    }

    // Resultado da validação: ou a lista pronta para enviar, ou a mensagem para o usuário
    public static final class Resultado {

        public final List<Bimestre> bimestres;
        public final String erro;

        private Resultado(List<Bimestre> bimestres, String erro) {
            this.bimestres = bimestres;
            this.erro = erro;
        }

        public boolean ok() {
            return erro == null;
        }
    }

    // entradas[0] é o 1º bimestre. Linha com os dois campos vazios fica indefinida.
    // Regras: início e fim juntos, fim não antes do início, em ordem e sem sobreposição.
    public static Resultado validar(int ano, List<Entrada> entradas) {

        List<Bimestre> lista = new ArrayList<>();
        Bimestre anterior = null;

        for (int i = 0; i < entradas.size(); i++) {

            int numero = i + 1;
            Entrada entrada = entradas.get(i);

            LocalDate inicio;
            LocalDate fim;

            try {
                inicio = ler(entrada.inicio);
                fim = ler(entrada.fim);
            } catch (IllegalArgumentException e) {
                return erro("Confira as datas do " + numero + "º bimestre (dia/mês/ano).");
            }

            if (inicio == null && fim == null) {
                continue;
            }

            if (inicio == null || fim == null) {
                return erro("Informe o início e o fim do " + numero + "º bimestre.");
            }

            if (fim.isBefore(inicio)) {
                return erro("No " + numero + "º bimestre, o fim vem antes do início.");
            }

            if (anterior != null && !inicio.isAfter(anterior.fim)) {
                return erro("O " + numero + "º bimestre começa antes do fim do " + anterior.numero
                        + "º. Os bimestres precisam estar em ordem e sem sobreposição.");
            }

            anterior = new Bimestre(ano, numero, inicio, fim);
            lista.add(anterior);
        }

        return new Resultado(lista, null);
    }

    private static Resultado erro(String mensagem) {
        return new Resultado(null, mensagem);
    }
}
