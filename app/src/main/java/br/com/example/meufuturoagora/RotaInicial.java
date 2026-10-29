package br.com.example.meufuturoagora;

import android.app.Activity;
import android.content.Intent;

import br.com.example.meufuturoagora.dados.Perfil;

// Para onde vai quem acabou de entrar (antes ficava em TelaEntrar.verificarUsuario).
// Admin é professor (decisão D11): entra pela tela do professor e acha a área
// do administrador no perfil.
final class RotaInicial {

    private RotaInicial() {
    }

    static void abrir(Activity origem, Perfil perfil) {

        Class<?> destino = perfil.ehProfessor() ? TelaInicialProfessor.class : TelaInicialAluno.class;

        origem.startActivity(new Intent(origem, destino));
        origem.finish();
    }
}
