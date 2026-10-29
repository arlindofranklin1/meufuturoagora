package br.com.example.meufuturoagora;

import android.app.Activity;
import android.content.Intent;
import android.view.View;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Mostra ou esconde a área do administrador. É só conveniência de tela: quem
// barra de verdade é o servidor (RLS e as funções com exigir_admin), então
// uma tela aberta à força não consegue gravar nada.
class AdminUtil {

    interface Callback {
        void aoVerificar(boolean ehAdmin);
    }

    private AdminUtil() {
    }

    static void verificar(Activity activity, Callback callback) {

        Tarefas.executar(activity, () -> Servicos.get(activity).perfis.meu(), new Tarefas.Retorno<Perfil>() {
            @Override
            public void sucesso(Perfil perfil) {
                callback.aoVerificar(perfil != null && perfil.admin && perfil.ativo);
            }

            @Override
            public void erro(ErroApi erro) {
                callback.aoVerificar(false);
            }
        });
    }

    // Mostra o botão do perfil só para administradores
    static void configurarBotao(Activity activity) {

        View botao = activity.findViewById(R.id.btnAreaAdministrador);

        botao.setOnClickListener(v ->
                activity.startActivity(new Intent(activity, TelaAdminHome.class))
        );

        verificar(activity, ehAdmin -> botao.setVisibility(ehAdmin ? View.VISIBLE : View.GONE));
    }
}
