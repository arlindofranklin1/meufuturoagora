package br.com.example.meufuturoagora;

import android.app.Activity;
import android.view.View;

import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

class InsetsUtil {

    private InsetsUtil() {
    }

    // Afasta o conteúdo das barras do sistema (status e navegação),
    // assim a margem do topo conta a partir de baixo da barra de status
    static void aplicarInsetsSistema(Activity activity) {

        View raiz = activity.findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {

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
        });
    }

    // Igual ao aplicarInsetsSistema, mas só afasta do topo (e laterais).
    // Usado nas telas com bottom bar, que já trata a parte de baixo sozinha.
    static void aplicarInsetsTopo(Activity activity) {

        View raiz = activity.findViewById(R.id.main);

        ViewCompat.setOnApplyWindowInsetsListener(raiz, (v, insets) -> {

            Insets systemBars = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            );

            v.setPadding(
                    systemBars.left,
                    systemBars.top,
                    systemBars.right,
                    v.getPaddingBottom()
            );

            return insets;
        });
    }

    // Empurra o conteúdo da bottom bar para cima da barra de navegação do
    // sistema, mas deixa o fundo da view esticar até a borda da tela —
    // assim não sobra tira em branco/preta do sistema embaixo da barra.
    static void aplicarInsetsBottomNav(View bottomNavigation) {

        int paddingInicial = bottomNavigation.getPaddingBottom();

        ViewCompat.setOnApplyWindowInsetsListener(bottomNavigation, (v, insets) -> {

            int inferior = insets.getInsets(
                    WindowInsetsCompat.Type.systemBars()
            ).bottom;

            v.setPadding(
                    v.getPaddingLeft(),
                    v.getPaddingTop(),
                    v.getPaddingRight(),
                    paddingInicial + inferior
            );

            return insets;
        });
    }
}
