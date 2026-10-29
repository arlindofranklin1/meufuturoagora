package br.com.example.meufuturoagora.dados;

final class ArmazenamentoMemoria implements ArmazenamentoSessao {

    String salvo;

    @Override
    public void salvar(String tokenDeRenovacao) {
        salvo = tokenDeRenovacao;
    }

    @Override
    public String ler() {
        return salvo;
    }

    @Override
    public void apagar() {
        salvo = null;
    }
}
