package br.com.example.meufuturoagora.dados;

// O "quem é você" vem sempre do servidor (vincular_conta, pelo e-mail
// verificado da conta), nunca do e-mail que o app tem em mãos.
public class AuthRepositorioSupabase implements AuthRepositorio {

    private final AuthSupabase auth;
    private final GerenciadorSessao sessao;
    private final RestSupabase rest;

    public AuthRepositorioSupabase(AuthSupabase auth, GerenciadorSessao sessao, RestSupabase rest) {
        this.auth = auth;
        this.sessao = sessao;
        this.rest = rest;
    }

    @Override
    public Perfil entrarComGoogle(String idToken, String nonce) throws ErroApi {
        sessao.iniciar(auth.entrarComIdTokenGoogle(idToken, nonce));
        return vincularOuSair();
    }

    @Override
    public Perfil restaurarSessao() throws ErroApi {

        if (!sessao.restaurar()) {
            return null;
        }

        return vincularOuSair();
    }

    @Override
    public void sair() {
        sessao.sair();
    }

    // Recusado (e-mail fora do cadastro, perfil inativo): não deixa sessão
    // aberta, como o acessoNaoAutorizado() da TelaEntrar
    private Perfil vincularOuSair() throws ErroApi {

        try {
            return Json.um(rest.rpcObjeto("vincular_conta", null), Perfil::deJson);

        } catch (ErroApi e) {

            if (e.semPermissao()) {
                sessao.sair();
            }

            throw e;
        }
    }
}
