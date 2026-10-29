package br.com.example.meufuturoagora;

import br.com.example.meufuturoagora.dados.AuthRepositorio;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;

// AuthRepositorio sem rede, para os testes de tela: devolve o perfil ou o erro configurado
final class AuthFalso implements AuthRepositorio {

    Perfil perfil;
    ErroApi erro;
    volatile boolean saiu;

    @Override
    public Perfil entrarComGoogle(String idToken, String nonce) throws ErroApi {
        return restaurarSessao();
    }

    @Override
    public Perfil restaurarSessao() throws ErroApi {

        if (erro != null) {
            throw erro;
        }

        return perfil;
    }

    @Override
    public void sair() {
        saiu = true;
    }

    static Perfil perfil(String tipo, boolean admin) {
        return new Perfil("p1", "u1", "teste@escola.test", "Teste", tipo, admin, true,
                Perfil.ALUNO.equals(tipo) ? "t1" : null, null, null);
    }
}
