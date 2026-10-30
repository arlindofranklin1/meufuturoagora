package br.com.example.meufuturoagora;

import android.app.Activity;
import android.widget.TextView;

import java.time.LocalDate;
import java.util.List;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Cartão "Bimestre" das telas iniciais: o bimestre em que hoje está, com o
// período que o administrador definiu
final class PeriodoAtual {

    private PeriodoAtual() {
    }

    static void mostrar(Activity tela, TextView titulo, TextView periodo) {

        Servicos servicos = Servicos.get(tela);

        Tarefas.executar(tela, () -> {

            AnoLetivo ano = servicos.escola.anoAtual();

            if (ano == null) {
                return null;
            }

            List<Bimestre> bimestres = servicos.escola.bimestres(ano.ano);
            LocalDate hoje = LocalDate.now();

            for (Bimestre b : bimestres) {

                if (b.contem(hoje)) {
                    return b;
                }
            }

            return null;

        }, new Tarefas.Retorno<Bimestre>() {
            @Override
            public void sucesso(Bimestre atual) {

                if (atual == null) {
                    titulo.setText("Bimestre");
                    periodo.setText("Bimestre não definido");
                    return;
                }

                titulo.setText(atual.numero + "º Bimestre");
                periodo.setText(Datas.formatar(atual.inicio) + " - " + Datas.formatar(atual.fim));
            }

            @Override
            public void erro(ErroApi erro) {
                // Fica o texto do layout
            }
        });
    }
}
