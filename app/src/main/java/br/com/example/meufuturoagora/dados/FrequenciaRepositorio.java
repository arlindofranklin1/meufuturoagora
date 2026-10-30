package br.com.example.meufuturoagora.dados;

import java.time.LocalDate;
import java.util.List;

// Aulas e frequência. O desconto de 5 pontos por dia de falta é do servidor.
public interface FrequenciaRepositorio {

    List<Aula> aulas(String disciplinaId) throws ErroApi;

    Aula cadastrarAula(String disciplinaId, LocalDate data, int quantidade, String descricao)
            throws ErroApi;

    // Professor da disciplina ou admin. A frequência da aula vai junto, e o
    // desconto de pontos por falta daquele dia some (é calculado da frequência).
    void excluirAula(String aulaId) throws ErroApi;

    // Professor: a da aula inteira; aluno: só a própria
    List<Frequencia> frequenciaDaAula(String aulaId) throws ErroApi;

    // Todas as que o usuário pode ver (aluno: as próprias)
    List<Frequencia> frequencias() throws ErroApi;

    // Tudo ou nada: um aluno fora da disciplina recusa a lista inteira
    int registrar(String aulaId, List<Frequencia> registros) throws ErroApi;
}
