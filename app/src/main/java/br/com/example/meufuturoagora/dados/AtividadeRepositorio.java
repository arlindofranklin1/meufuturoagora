package br.com.example.meufuturoagora.dados;

import java.time.LocalDate;
import java.util.List;

// Atividades ("trilhas"), entregas e avaliação (decisões D3, D4, D6)
public interface AtividadeRepositorio {

    List<Atividade> daDisciplina(String disciplinaId) throws ErroApi;

    Atividade buscar(String id) throws ErroApi;

    // O id vem do app para o anexo ir antes para a pasta certa (Arquivos.caminhoAtividade)
    Atividade criar(String id, String disciplinaId, String nome, String descricao, int pontos,
                    LocalDate prazo, String link, String arquivoPath, String arquivoNome) throws ErroApi;

    Atividade atualizar(String id, String nome, String descricao, int pontos, LocalDate prazo,
                        String link, String arquivoPath, String arquivoNome) throws ErroApi;

    // Caminhos dos arquivos da trilha e das entregas dela, para o app apagar
    // do Storage antes de excluir (o Storage não apaga por SQL)
    ArquivosDaAtividade arquivosDaAtividade(String atividadeId) throws ErroApi;

    // Apaga a trilha e, em cascata, as entregas
    void excluir(String atividadeId) throws ErroApi;

    // Professor: todas da trilha; aluno: só a própria
    List<Entrega> entregasDaAtividade(String atividadeId) throws ErroApi;

    // Entrega do aluno logado, ou null se ainda não entregou
    Entrega minhaEntrega(String atividadeId) throws ErroApi;

    List<Entrega> minhasEntregas() throws ErroApi;

    // Admin: para apagar os arquivos do aluno antes da exclusão definitiva
    List<Entrega> entregasDoAluno(String alunoId) throws ErroApi;

    // Uma vez só; o servidor marca horário e dias de atraso
    Entrega entregar(String atividadeId, String comentario, String arquivoPath, String arquivoNome)
            throws ErroApi;

    void avaliar(String entregaId, double nota, String comentario) throws ErroApi;

    void refazerAvaliacao(String entregaId) throws ErroApi;

    final class ArquivosDaAtividade {

        public final String anexo;
        public final List<String> entregas;

        public ArquivosDaAtividade(String anexo, List<String> entregas) {
            this.anexo = anexo;
            this.entregas = entregas;
        }
    }
}
