package br.com.example.meufuturoagora.dados;

import java.util.List;

// Perfis, cadastro pelo admin e pedidos de exclusão (decisões D7, D10, D12, D13)
public interface PerfilRepositorio {

    // O próprio perfil (o RLS só devolve o dele para quem não é admin)
    Perfil meu() throws ErroApi;

    // Admin: todos; professor: os alunos das suas disciplinas
    List<Perfil> listar(String tipo) throws ErroApi;

    // Admin. E-mail repetido: ErroApi com jaExiste()
    Perfil cadastrar(String email, String nome, String tipo, String turmaId) throws ErroApi;

    // Admin
    Perfil atualizar(String id, String email, String nome, String turmaId, boolean ativo, boolean admin)
            throws ErroApi;

    // O próprio usuário; null volta para a foto do Google
    void definirFoto(String perfilId, String fotoPath) throws ErroApi;

    // Admin, exclusão definitiva. Os arquivos do usuário são apagados antes pelo app.
    void excluir(String perfilId) throws ErroApi;

    // Admin: passa as disciplinas antes de excluir um professor
    int transferirDisciplinas(String deProfessorId, String paraProfessorId) throws ErroApi;

    String solicitarExclusao(String motivo) throws ErroApi;

    // Os do próprio usuário, ou todos para o admin
    List<SolicitacaoExclusao> solicitacoes() throws ErroApi;

    void decidirExclusao(String solicitacaoId, boolean aprovar) throws ErroApi;
}
