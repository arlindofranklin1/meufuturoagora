package br.com.example.meufuturoagora.dados;

// Onde o token de renovação fica entre uma abertura do app e outra
public interface ArmazenamentoSessao {

    void salvar(String tokenDeRenovacao);

    // null se não houver sessão salva ou se ela estiver ilegível
    String ler();

    void apagar();
}
