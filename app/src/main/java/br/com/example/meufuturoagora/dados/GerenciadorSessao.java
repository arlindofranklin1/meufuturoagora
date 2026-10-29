package br.com.example.meufuturoagora.dados;

import java.util.function.LongSupplier;

// Dono da sessão do usuário: guarda o token de acesso em memória, o de
// renovação no ArmazenamentoSessao, e renova antes de expirar.
// Sincronizado para duas requisições simultâneas não renovarem duas vezes.
public class GerenciadorSessao {

    // Renova um pouco antes do fim, para o token não vencer no meio da requisição
    static final long MARGEM_SEGUNDOS = 60;

    private final AuthSupabase auth;
    private final ArmazenamentoSessao armazenamento;
    private final LongSupplier agoraEmSegundos;

    private Sessao sessao;

    public GerenciadorSessao(AuthSupabase auth, ArmazenamentoSessao armazenamento,
                             LongSupplier agoraEmSegundos) {
        this.auth = auth;
        this.armazenamento = armazenamento;
        this.agoraEmSegundos = agoraEmSegundos;
    }

    public synchronized void iniciar(Sessao nova) {
        sessao = nova;
        armazenamento.salvar(nova.tokenDeRenovacao);
    }

    public synchronized boolean temSessao() {
        return sessao != null;
    }

    public synchronized Sessao atual() {
        return sessao;
    }

    // Token para a próxima requisição, ou null sem login (a requisição vai só com a chave pública)
    public synchronized String tokenDeAcesso() throws ErroApi {

        if (sessao == null) {
            return null;
        }

        if (sessao.expiraEm - agoraEmSegundos.getAsLong() <= MARGEM_SEGUNDOS) {
            renovar();
        }

        return sessao.tokenDeAcesso;
    }

    // Token de renovação recusado pelo servidor: a sessão acabou de vez.
    // Sem internet: mantém o que está salvo para tentar de novo depois.
    public synchronized void renovar() throws ErroApi {

        String tokenDeRenovacao = sessao != null ? sessao.tokenDeRenovacao : armazenamento.ler();

        if (tokenDeRenovacao == null) {
            throw ErroApi.sessaoExpirada();
        }

        try {
            iniciar(auth.renovar(tokenDeRenovacao));

        } catch (ErroApi e) {

            if (e.codigoHttp() >= 400 && e.codigoHttp() < 500) {
                limparLocal();
                throw ErroApi.sessaoExpirada();
            }

            throw e;
        }
    }

    // Abertura do app (Splash): volta a sessão salva, se houver
    public synchronized boolean restaurar() throws ErroApi {

        if (sessao != null) {
            return true;
        }

        if (armazenamento.ler() == null) {
            return false;
        }

        renovar();
        return true;
    }

    // Revoga no servidor e apaga do aparelho. Se o servidor não responder, o
    // aparelho é limpo do mesmo jeito; o token de acesso vence sozinho.
    public synchronized void sair() {

        Sessao anterior = sessao;
        limparLocal();

        if (anterior != null) {
            try {
                auth.sair(anterior.tokenDeAcesso);
            } catch (ErroApi ignorado) {
                // Já saiu do aparelho, que é o que importa para o usuário
            }
        }
    }

    private void limparLocal() {
        sessao = null;
        armazenamento.apagar();
    }
}
