package br.com.example.meufuturoagora;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.ArquivoRepositorio;
import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.AtividadeRepositorio;
import br.com.example.meufuturoagora.dados.Aula;
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.DiarioRepositorio;
import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.DisciplinaRepositorio;
import br.com.example.meufuturoagora.dados.EntradaDiario;
import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.ErroApiDeTeste;
import br.com.example.meufuturoagora.dados.EscolaRepositorio;
import br.com.example.meufuturoagora.dados.Frequencia;
import br.com.example.meufuturoagora.dados.FrequenciaRepositorio;
import br.com.example.meufuturoagora.dados.HistoricoAluno;
import br.com.example.meufuturoagora.dados.HistoricoEntrega;
import br.com.example.meufuturoagora.dados.HistoricoRepositorio;
import br.com.example.meufuturoagora.dados.HistoricoTrilha;
import br.com.example.meufuturoagora.dados.LinhaRanking;
import br.com.example.meufuturoagora.dados.Novidade;
import br.com.example.meufuturoagora.dados.NovidadeRepositorio;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.PerfilRepositorio;
import br.com.example.meufuturoagora.dados.PosicaoRanking;
import br.com.example.meufuturoagora.dados.RankingRepositorio;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.SolicitacaoExclusao;
import br.com.example.meufuturoagora.dados.Turma;

// Todos os repositórios em memória, sem rede, para os testes de tela. Cada
// gravação fica anotada em "chamadas" ("perfis.excluir p2", ...) para o teste
// conferir o que a tela pediu ao servidor.
final class RepositoriosFalsos {

    final List<String> chamadas = new CopyOnWriteArrayList<>();

    Perfil eu;
    final List<Perfil> perfis = new ArrayList<>();
    final List<Turma> turmas = new ArrayList<>();
    final List<Disciplina> disciplinas = new ArrayList<>();
    final List<Atividade> atividades = new ArrayList<>();
    final List<Entrega> entregas = new ArrayList<>();
    final List<Aula> aulas = new ArrayList<>();
    final List<EntradaDiario> diario = new ArrayList<>();
    final List<LinhaRanking> linhasRanking = new ArrayList<>();
    ErroApi erroNoDiario;
    final List<Frequencia> frequencias = new ArrayList<>();
    final List<Participante> participantes = new ArrayList<>();
    final List<SolicitacaoExclusao> pedidos = new ArrayList<>();
    AnoLetivo anoAtual;
    final List<Bimestre> bimestres = new ArrayList<>();
    int pontuacao;
    PosicaoRanking posicao;
    final List<Integer> historicoAnos = new ArrayList<>();
    final List<HistoricoAluno> historicoAlunos = new ArrayList<>();
    final List<HistoricoTrilha> historicoTrilhas = new ArrayList<>();
    final List<HistoricoEntrega> historicoEntregas = new ArrayList<>();

    // Próxima chamada de cadastro devolve este erro (ex.: e-mail repetido)
    ErroApi erroNoCadastro;

    final AuthFalso auth = new AuthFalso();

    void instalar() {
        Servicos.substituir(new Servicos(auth, new Perfis(), new Escola(), new Disciplinas(), new Atividades(),
                new Frequencias(), new Ranking(), new Diario(), new Novidades(), new Arquivos(), new Historico()));
    }

    boolean chamou(String chamada) {
        return chamadas.contains(chamada);
    }

    // =====================================================
    // MONTAGEM DE DADOS
    // =====================================================

    static Perfil perfil(String id, String nome, String tipo, boolean admin, String turmaId) {
        return new Perfil(id, "u-" + id, id + "@escola.test", nome, tipo, admin, true, turmaId, null, null);
    }

    static Disciplina disciplina(String id, String nome, String turmaId, String professorId) {
        return new Disciplina(id, nome, turmaId, null, professorId, 2026, 4, true, OffsetDateTime.now());
    }

    static Entrega entrega(String id, String atividadeId, String alunoId, String arquivoPath, boolean avaliada) {
        return new Entrega(id, atividadeId, alunoId, "ok", arquivoPath, arquivoPath != null ? "x.pdf" : null,
                OffsetDateTime.now(), 0, avaliada ? 8.0 : null, null, avaliada ? OffsetDateTime.now() : null);
    }

    static ErroApi erro(int http, String codigo, String mensagem) {
        return ErroApiDeTeste.criar(http, "{\"code\":\"" + codigo + "\",\"message\":\"" + mensagem + "\"}");
    }

    // =====================================================
    // REPOSITÓRIOS
    // =====================================================

    final class Perfis implements PerfilRepositorio {

        @Override
        public Perfil meu() {
            return eu;
        }

        @Override
        public List<Perfil> listar(String tipo) {
            List<Perfil> lista = new ArrayList<>();
            for (Perfil p : perfis) {
                if (p.tipo.equals(tipo)) {
                    lista.add(p);
                }
            }
            return lista;
        }

        @Override
        public Perfil buscarPorEmail(String email) {
            for (Perfil p : perfis) {
                if (p.email.equalsIgnoreCase(email.trim())) {
                    return p;
                }
            }
            return null;
        }

        @Override
        public Perfil cadastrar(String email, String nome, String tipo, String turmaId) throws ErroApi {
            chamadas.add("perfis.cadastrar " + email + " " + nome + " " + tipo + " " + turmaId);
            if (erroNoCadastro != null) {
                ErroApi erro = erroNoCadastro;
                erroNoCadastro = null;
                throw erro;
            }
            Perfil novo = new Perfil("novo", null, email, nome, tipo, false, true, turmaId, null, null);
            perfis.add(novo);
            return novo;
        }

        @Override
        public Perfil atualizar(String id, String email, String nome, String turmaId, boolean ativo, boolean admin) {
            chamadas.add("perfis.atualizar " + id + " " + nome + " " + turmaId);
            return buscarPorEmail(email);
        }

        @Override
        public void definirFoto(String perfilId, String fotoPath) {
            chamadas.add("perfis.definirFoto " + perfilId);
        }

        @Override
        public void excluir(String perfilId) {
            chamadas.add("perfis.excluir " + perfilId);
            perfis.removeIf(p -> p.id.equals(perfilId));
        }

        @Override
        public int transferirDisciplinas(String de, String para) {
            chamadas.add("perfis.transferir " + de + " " + para);
            int total = 0;
            for (int i = 0; i < disciplinas.size(); i++) {
                Disciplina d = disciplinas.get(i);
                if (de.equals(d.professorId)) {
                    disciplinas.set(i, disciplina(d.id, d.nome, d.turmaId, para));
                    total++;
                }
            }
            return total;
        }

        @Override
        public String solicitarExclusao(String motivo) {
            chamadas.add("perfis.solicitarExclusao " + motivo);
            return "pedido-novo";
        }

        @Override
        public List<SolicitacaoExclusao> solicitacoes() {
            return new ArrayList<>(pedidos);
        }

        @Override
        public void decidirExclusao(String solicitacaoId, boolean aprovar) {
            chamadas.add("perfis.decidir " + solicitacaoId + " " + aprovar);
        }
    }

    final class Escola implements EscolaRepositorio {

        @Override
        public List<Turma> turmas() {
            return new ArrayList<>(turmas);
        }

        @Override
        public Turma criarTurma(String nome, Integer serie) {
            chamadas.add("escola.criarTurma " + nome + " " + serie);
            Turma nova = new Turma("turma-nova", nome, serie, true);
            turmas.add(nova);
            return nova;
        }

        @Override
        public Turma atualizarTurma(String id, String nome, Integer serie, boolean ativo) {
            chamadas.add("escola.atualizarTurma " + id);
            return new Turma(id, nome, serie, ativo);
        }

        @Override
        public void excluirTurma(String id) {
            chamadas.add("escola.excluirTurma " + id);
            turmas.removeIf(t -> t.id.equals(id));
        }

        @Override
        public AnoLetivo anoAtual() {
            return anoAtual;
        }

        @Override
        public List<AnoLetivo> anos() {
            return anoAtual != null ? Collections.singletonList(anoAtual) : Collections.emptyList();
        }

        @Override
        public List<Bimestre> bimestres(int ano) {
            return new ArrayList<>(bimestres);
        }

        @Override
        public void definirAnoLetivo(int ano) {
            chamadas.add("escola.definirAnoLetivo " + ano);
        }

        @Override
        public void definirBimestres(int ano, List<Bimestre> lista) {
            chamadas.add("escola.definirBimestres " + ano + " " + lista.size());
        }
    }

    final class Disciplinas implements DisciplinaRepositorio {

        @Override
        public List<Disciplina> listar() {
            return new ArrayList<>(disciplinas);
        }

        @Override
        public Disciplina buscar(String id) {
            for (Disciplina d : disciplinas) {
                if (d.id.equals(id)) {
                    return d;
                }
            }
            return null;
        }

        @Override
        public Disciplina criar(String nome, String turmaId) {
            chamadas.add("disciplinas.criar " + nome + " " + turmaId);
            Disciplina nova = disciplina("disciplina-nova", nome, turmaId, eu != null ? eu.id : null);
            disciplinas.add(nova);
            return nova;
        }

        @Override
        public void renomear(String id, String nome) {
            chamadas.add("disciplinas.renomear " + id + " " + nome);
        }

        @Override
        public void desativar(String id) {
            chamadas.add("disciplinas.desativar " + id);
        }

        @Override
        public List<String> alunosMatriculados(String disciplinaId) {
            return new ArrayList<>();
        }

        @Override
        public List<Participante> participantes(String disciplinaId) {
            return new ArrayList<>(participantes);
        }
    }

    final class Atividades implements AtividadeRepositorio {

        @Override
        public List<Atividade> daDisciplina(String disciplinaId) {
            List<Atividade> lista = new ArrayList<>();
            for (Atividade a : atividades) {
                if (a.disciplinaId.equals(disciplinaId)) {
                    lista.add(a);
                }
            }
            return lista;
        }

        @Override
        public Atividade buscar(String id) {
            for (Atividade a : atividades) {
                if (a.id.equals(id)) {
                    return a;
                }
            }
            return null;
        }

        @Override
        public Atividade criar(String id, String disciplinaId, String nome, String descricao, int pontos,
                               LocalDate prazo, String link, String arquivoPath, String arquivoNome) {
            chamadas.add("atividades.criar " + nome);
            Atividade nova = new Atividade(id, disciplinaId, nome, descricao, pontos, prazo, link, arquivoPath,
                    arquivoNome, 2026, 4, OffsetDateTime.now());
            atividades.add(nova);
            return nova;
        }

        @Override
        public Atividade atualizar(String id, String nome, String descricao, int pontos, LocalDate prazo,
                                   String link, String arquivoPath, String arquivoNome) {
            chamadas.add("atividades.atualizar " + id);
            return buscar(id);
        }

        @Override
        public ArquivosDaAtividade arquivosDaAtividade(String atividadeId) {
            List<String> caminhos = new ArrayList<>();
            for (Entrega e : entregas) {
                if (e.atividadeId.equals(atividadeId) && e.arquivoPath != null) {
                    caminhos.add(e.arquivoPath);
                }
            }
            Atividade atividade = buscar(atividadeId);
            return new ArquivosDaAtividade(atividade != null ? atividade.arquivoPath : null, caminhos);
        }

        @Override
        public void excluir(String atividadeId) {
            chamadas.add("atividades.excluir " + atividadeId);
            atividades.removeIf(a -> a.id.equals(atividadeId));
        }

        @Override
        public List<Entrega> entregasDaAtividade(String atividadeId) {
            List<Entrega> lista = new ArrayList<>();
            for (Entrega e : entregas) {
                if (e.atividadeId.equals(atividadeId)) {
                    lista.add(e);
                }
            }
            return lista;
        }

        @Override
        public Entrega minhaEntrega(String atividadeId) {
            for (Entrega e : entregas) {
                if (e.atividadeId.equals(atividadeId) && eu != null && e.alunoId.equals(eu.id)) {
                    return e;
                }
            }
            return null;
        }

        @Override
        public List<Entrega> minhasEntregas() {
            return eu != null ? entregasDoAluno(eu.id) : new ArrayList<>();
        }

        @Override
        public List<Entrega> entregasDoAluno(String alunoId) {
            List<Entrega> lista = new ArrayList<>();
            for (Entrega e : entregas) {
                if (e.alunoId.equals(alunoId)) {
                    lista.add(e);
                }
            }
            return lista;
        }

        @Override
        public Entrega entregar(String atividadeId, String comentario, String arquivoPath, String arquivoNome) {
            chamadas.add("atividades.entregar " + atividadeId);
            Entrega nova = new Entrega("entrega-nova", atividadeId, eu.id, comentario, arquivoPath, arquivoNome,
                    OffsetDateTime.now(), 0, null, null, null);
            entregas.add(nova);
            return nova;
        }

        @Override
        public void avaliar(String entregaId, double nota, String comentario) {
            chamadas.add("atividades.avaliar " + entregaId + " " + nota);
        }

        @Override
        public void refazerAvaliacao(String entregaId) {
            chamadas.add("atividades.refazer " + entregaId);
        }
    }

    final class Frequencias implements FrequenciaRepositorio {

        @Override
        public List<Aula> aulas(String disciplinaId) {
            List<Aula> lista = new ArrayList<>();
            for (Aula a : aulas) {
                if (a.disciplinaId.equals(disciplinaId)) {
                    lista.add(a);
                }
            }
            return lista;
        }

        @Override
        public Aula cadastrarAula(String disciplinaId, LocalDate data, int quantidade, String descricao) {
            chamadas.add("frequencias.cadastrarAula " + disciplinaId + " " + data + " " + quantidade);
            Aula nova = new Aula("aula-nova", disciplinaId, data, quantidade, descricao, 4);
            aulas.add(nova);
            return nova;
        }

        @Override
        public void excluirAula(String aulaId) {
            chamadas.add("frequencias.excluirAula " + aulaId);
            aulas.removeIf(a -> a.id.equals(aulaId));
        }

        @Override
        public List<Frequencia> frequenciaDaAula(String aulaId) {
            List<Frequencia> lista = new ArrayList<>();
            for (Frequencia f : frequencias) {
                if (f.aulaId.equals(aulaId)) {
                    lista.add(f);
                }
            }
            return lista;
        }

        @Override
        public List<Frequencia> frequencias() {
            return new ArrayList<>(frequencias);
        }

        @Override
        public int registrar(String aulaId, List<Frequencia> registros) {
            StringBuilder resumo = new StringBuilder("frequencias.registrar " + aulaId);
            for (Frequencia f : registros) {
                resumo.append(' ').append(f.alunoId).append('=').append(f.status);
            }
            chamadas.add(resumo.toString());
            return registros.size();
        }
    }

    final class Ranking implements RankingRepositorio {

        @Override
        public List<LinhaRanking> ranking(String turmaId) {
            chamadas.add("ranking.ranking " + turmaId);
            return new ArrayList<>(linhasRanking);
        }

        @Override
        public List<Turma> turmasDoRanking() {
            return new ArrayList<>(turmas);
        }

        @Override
        public PosicaoRanking minhaPosicao() {
            return posicao;
        }

        @Override
        public int pontuacao(String alunoId, Integer ano) {
            return pontuacao;
        }
    }

    final class Diario implements DiarioRepositorio {

        @Override
        public List<EntradaDiario> entradas() {
            List<EntradaDiario> lista = new ArrayList<>(diario);
            java.util.Collections.reverse(lista);
            return lista;
        }

        @Override
        public EntradaDiario escrever(String texto) throws ErroApi {
            chamadas.add("diario.escrever " + texto);
            falharSeNecessario();
            EntradaDiario nova = new EntradaDiario("entrada-" + (diario.size() + 1), texto,
                    OffsetDateTime.now(), OffsetDateTime.now());
            diario.add(nova);
            return nova;
        }

        @Override
        public void editar(String id, String texto) throws ErroApi {
            chamadas.add("diario.editar " + id + " " + texto);
            falharSeNecessario();
            for (int i = 0; i < diario.size(); i++) {
                if (diario.get(i).id.equals(id)) {
                    EntradaDiario antiga = diario.get(i);
                    diario.set(i, new EntradaDiario(id, texto, antiga.criadoEm, OffsetDateTime.now()));
                }
            }
        }

        @Override
        public void apagar(String id) throws ErroApi {
            chamadas.add("diario.apagar " + id);
            falharSeNecessario();
            diario.removeIf(e -> e.id.equals(id));
        }

        private void falharSeNecessario() throws ErroApi {
            if (erroNoDiario != null) {
                ErroApi erro = erroNoDiario;
                erroNoDiario = null;
                throw erro;
            }
        }
    }

    final class Novidades implements NovidadeRepositorio {

        @Override
        public List<Novidade> desde(OffsetDateTime momento) {
            return new ArrayList<>();
        }
    }

    // Devolve as listas montadas pelo teste, sem filtrar por período, e anota o período pedido
    final class Historico implements HistoricoRepositorio {

        @Override
        public List<Integer> anos() {
            return new ArrayList<>(historicoAnos);
        }

        @Override
        public List<HistoricoAluno> alunos(int ano, int bimestre) {
            chamadas.add("historico.alunos " + ano + " " + bimestre);
            return new ArrayList<>(historicoAlunos);
        }

        @Override
        public List<HistoricoTrilha> trilhas(int ano, int bimestre) {
            chamadas.add("historico.trilhas " + ano + " " + bimestre);
            return new ArrayList<>(historicoTrilhas);
        }

        @Override
        public List<HistoricoEntrega> entregas(int ano, int bimestre) {
            chamadas.add("historico.entregas " + ano + " " + bimestre);
            return new ArrayList<>(historicoEntregas);
        }
    }

    final class Arquivos implements ArquivoRepositorio {

        @Override
        public void enviar(String bucket, String caminho, byte[] conteudo, String tipoMime) {
            chamadas.add("arquivos.enviar " + bucket);
        }

        @Override
        public String urlAssinada(String bucket, String caminho, int segundos) {
            return "https://exemplo.test/" + caminho;
        }

        @Override
        public byte[] baixar(String bucket, String caminho) {
            return new byte[0];
        }

        @Override
        public void apagar(String bucket, List<String> caminhos) {
            if (!caminhos.isEmpty()) {
                chamadas.add("arquivos.apagar " + bucket + " " + String.join(",", caminhos));
            }
        }
    }
}
