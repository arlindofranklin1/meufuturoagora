package br.com.example.meufuturoagora.dados;

import java.util.List;

// Disciplinas e matrículas (decisões D1 e D9)
public interface DisciplinaRepositorio {

    // O RLS decide: professor vê as suas, aluno as matriculadas e ativas, admin todas
    List<Disciplina> listar() throws ErroApi;

    Disciplina buscar(String id) throws ErroApi;

    // Professor cria em seu nome; os alunos da turma são matriculados pelo banco
    Disciplina criar(String nome, String turmaId) throws ErroApi;

    void renomear(String id, String nome) throws ErroApi;

    // Exclusão lógica (some para os alunos)
    void desativar(String id) throws ErroApi;

    // Ids dos alunos matriculados
    List<String> alunosMatriculados(String disciplinaId) throws ErroApi;

    // Professor e colegas, com e-mail só para quem pode ver
    List<Participante> participantes(String disciplinaId) throws ErroApi;
}
