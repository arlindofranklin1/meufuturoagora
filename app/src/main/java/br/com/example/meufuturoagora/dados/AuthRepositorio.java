package br.com.example.meufuturoagora.dados;

public interface AuthRepositorio {

    // Troca o ID token do Google por uma sessão e liga a conta ao cadastro do
    // admin. E-mail não cadastrado: ErroApi (semPermissao) e nenhuma sessão fica.
    Perfil entrarComGoogle(String idToken, String nonce) throws ErroApi;

    // Abertura do app: perfil da sessão salva, ou null se não houver sessão
    Perfil restaurarSessao() throws ErroApi;

    void sair();

    // Id do usuário (Auth) da sessão que já está aberta, ou null; sem rede. Chave do histórico das notificações.
    String usuarioAtualId();
}
