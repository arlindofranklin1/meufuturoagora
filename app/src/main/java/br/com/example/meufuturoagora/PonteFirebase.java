package br.com.example.meufuturoagora;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

// TEMPORÁRIO (Fase 5 da migração): o login de verdade é o do Supabase, mas as
// telas ainda não migradas leem FirebaseAuth/Firestore. Entrar também no
// Firebase com o mesmo token do Google mantém essas telas funcionando até o
// módulo delas. Sai na Fase 6, junto com o Firebase.
// Nunca decide acesso: quem autoriza é vincular_conta() no Supabase.
final class PonteFirebase {

    private PonteFirebase() {
    }

    // depois roda com ou sem sucesso: a ponte não pode travar o login
    static void entrar(String idTokenGoogle, Runnable depois) {

        FirebaseAuth.getInstance()
                .signInWithCredential(GoogleAuthProvider.getCredential(idTokenGoogle, null))
                .addOnCompleteListener(tarefa -> depois.run());
    }

    static void sair() {
        FirebaseAuth.getInstance().signOut();
    }
}
