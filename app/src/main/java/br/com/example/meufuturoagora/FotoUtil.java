package br.com.example.meufuturoagora;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;

class FotoUtil {

    private FotoUtil() {
    }

    // Carrega a foto da conta Google do usuário (aluno ou professor) no
    // ImageView, mantendo o ícone padrão do layout quando não há foto salva.
    static void carregar(Context context, String fotoUrl, ImageView imageView) {

        if (fotoUrl == null || fotoUrl.isEmpty()) {
            return;
        }

        Glide.with(context)
                .load(fotoUrl)
                .placeholder(imageView.getDrawable())
                .error(imageView.getDrawable())
                .circleCrop()
                .into(imageView);
    }
}
