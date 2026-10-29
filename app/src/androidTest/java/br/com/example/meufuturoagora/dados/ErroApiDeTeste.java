package br.com.example.meufuturoagora.dados;

// Os testes de tela simulam respostas do servidor; o ErroApi é montado pelo
// mesmo caminho do app (daResposta), não por um construtor só para teste
public final class ErroApiDeTeste {

    private ErroApiDeTeste() {
    }

    public static ErroApi criar(int codigoHttp, String corpo) {
        return ErroApi.daResposta(codigoHttp, corpo);
    }
}
