package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import br.com.example.meufuturoagora.dados.Arquivos;
import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.SolicitacaoExclusao;
import br.com.example.meufuturoagora.dados.Tarefas;
import br.com.example.meufuturoagora.dados.Turma;

// Usuários e turmas, para o admin. A exclusão é definitiva (decisão D7) e
// passa pelo servidor (excluir_usuario / decidir_exclusao), que apaga em
// cascata e recusa excluir o último admin (D14).
public class ListaUsuariosActivity extends AppCompatActivity {

    private ExpandableListView expandableUsuarios;
    private TextView tvSemUsuarios;

    private final List<Grupo> grupos = new ArrayList<>();
    private GrupoAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_lista_usuarios);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        expandableUsuarios = findViewById(R.id.expandableUsuarios);
        tvSemUsuarios = findViewById(R.id.tvSemUsuarios);

        adapter = new GrupoAdapter();
        expandableUsuarios.setAdapter(adapter);

        carregarDados();
    }

    private void carregarDados() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> new Dados(
                servicos.perfis.listar(Perfil.PROFESSOR),
                servicos.perfis.listar(Perfil.ALUNO),
                servicos.escola.turmas(),
                servicos.perfis.solicitacoes()
        ), new Tarefas.Retorno<Dados>() {
            @Override
            public void sucesso(Dados dados) {
                montarGrupos(dados);
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(ListaUsuariosActivity.this, "Erro ao carregar usuários: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void montarGrupos(Dados dados) {

        grupos.clear();

        Map<String, Perfil> perfisPorId = new LinkedHashMap<>();

        for (Perfil perfil : dados.professores) {
            perfisPorId.put(perfil.id, perfil);
        }

        for (Perfil perfil : dados.alunos) {
            perfisPorId.put(perfil.id, perfil);
        }

        // Decisão D12: pedidos abertos aparecem primeiro
        List<Pessoa> pedidos = new ArrayList<>();

        for (SolicitacaoExclusao pedido : dados.pedidos) {

            Perfil perfil = perfisPorId.get(pedido.perfilId);

            if (pedido.pendente() && perfil != null) {
                pedidos.add(new Pessoa(perfil, pedido));
            }
        }

        if (!pedidos.isEmpty()) {
            grupos.add(new Grupo("Pedidos de exclusão (" + pedidos.size() + ")", pedidos, null));
        }

        List<Pessoa> professores = new ArrayList<>();

        for (Perfil perfil : dados.professores) {
            professores.add(new Pessoa(perfil, null));
        }

        grupos.add(new Grupo("Professores (" + professores.size() + ")", professores, null));

        Map<String, Grupo> gruposPorTurmaId = new LinkedHashMap<>();

        for (Turma turma : dados.turmas) {
            gruposPorTurmaId.put(turma.id, new Grupo("Turma " + turma.nome, new ArrayList<>(), turma.id));
        }

        List<Pessoa> semTurma = new ArrayList<>();

        for (Perfil perfil : dados.alunos) {

            Grupo grupo = perfil.turmaId != null ? gruposPorTurmaId.get(perfil.turmaId) : null;

            if (grupo != null) {
                grupo.pessoas.add(new Pessoa(perfil, null));
            } else {
                semTurma.add(new Pessoa(perfil, null));
            }
        }

        for (Grupo grupo : gruposPorTurmaId.values()) {
            grupo.titulo = grupo.titulo + " (" + grupo.pessoas.size() + " alunos)";
            grupos.add(grupo);
        }

        if (!semTurma.isEmpty()) {
            grupos.add(new Grupo("Sem turma (" + semTurma.size() + ")", semTurma, null));
        }

        adapter.notifyDataSetChanged();

        boolean semNada = dados.professores.isEmpty() && dados.turmas.isEmpty() && semTurma.isEmpty();
        tvSemUsuarios.setVisibility(semNada ? View.VISIBLE : View.GONE);
        expandableUsuarios.setVisibility(semNada ? View.GONE : View.VISIBLE);
    }

    // =====================================================
    // EXCLUIR PESSOA (direto ou aprovando o pedido)
    // =====================================================

    private void confirmarExclusaoPessoa(Pessoa pessoa) {

        String tipo = pessoa.perfil.ehProfessor() ? "professor" : "aluno";
        String oQueApaga = pessoa.perfil.ehProfessor()
                ? "o cadastro, a conta de login e a foto"
                : "o cadastro, a conta de login, a foto, as entregas, as notas, a frequência e o diário";

        new AlertDialog.Builder(this)
                .setTitle(pessoa.pedido != null ? "Aprovar exclusão" : "Excluir " + tipo)
                .setMessage("Isto apaga de vez " + oQueApaga + " de \"" + pessoa.perfil.nome
                        + "\". Não dá para desfazer.\n\nDeseja continuar?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> verificarDisciplinas(pessoa))
                .show();
    }

    // Decisão D13: as disciplinas do professor passam para outro antes
    private void verificarDisciplinas(Pessoa pessoa) {

        if (!pessoa.perfil.ehProfessor()) {
            excluir(pessoa);
            return;
        }

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            int disciplinas = 0;

            for (Disciplina disciplina : servicos.disciplinas.listar()) {
                if (pessoa.perfil.id.equals(disciplina.professorId)) {
                    disciplinas++;
                }
            }

            List<Perfil> outros = new ArrayList<>();

            if (disciplinas > 0) {
                for (Perfil professor : servicos.perfis.listar(Perfil.PROFESSOR)) {
                    if (!professor.id.equals(pessoa.perfil.id) && professor.ativo) {
                        outros.add(professor);
                    }
                }
            }

            return new Transferencia(disciplinas, outros);

        }, new Tarefas.Retorno<Transferencia>() {
            @Override
            public void sucesso(Transferencia transferencia) {

                if (transferencia.disciplinas == 0) {
                    excluir(pessoa);
                    return;
                }

                if (transferencia.destinos.isEmpty()) {
                    Toast.makeText(ListaUsuariosActivity.this,
                            "Cadastre outro professor para receber as disciplinas antes de excluir.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                escolherDestino(pessoa, transferencia);
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(ListaUsuariosActivity.this, erro.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void escolherDestino(Pessoa pessoa, Transferencia transferencia) {

        String[] nomes = new String[transferencia.destinos.size()];

        for (int i = 0; i < nomes.length; i++) {
            nomes[i] = transferencia.destinos.get(i).nome;
        }

        new AlertDialog.Builder(this)
                .setTitle("Quem recebe as " + transferencia.disciplinas + " disciplina(s)?")
                .setItems(nomes, (dialog, posicao) -> {

                    Perfil destino = transferencia.destinos.get(posicao);

                    Tarefas.executar(this,
                            () -> Servicos.get(this).perfis.transferirDisciplinas(pessoa.perfil.id, destino.id),
                            new Tarefas.Retorno<Integer>() {
                                @Override
                                public void sucesso(Integer total) {
                                    Toast.makeText(ListaUsuariosActivity.this,
                                            total + " disciplina(s) passada(s) para " + destino.nome + ".",
                                            Toast.LENGTH_SHORT).show();
                                    excluir(pessoa);
                                }

                                @Override
                                public void erro(ErroApi erro) {
                                    Toast.makeText(ListaUsuariosActivity.this, erro.getMessage(),
                                            Toast.LENGTH_LONG).show();
                                }
                            });
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    // Os arquivos saem antes, pela API do Storage (o banco não apaga arquivos);
    // depois o servidor apaga o resto em cascata, inclusive a conta de login
    private void excluir(Pessoa pessoa) {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            List<String> entregas = new ArrayList<>();

            for (Entrega entrega : servicos.atividades.entregasDoAluno(pessoa.perfil.id)) {
                if (entrega.arquivoPath != null) {
                    entregas.add(entrega.arquivoPath);
                }
            }

            servicos.arquivos.apagar(Arquivos.BUCKET_ENTREGAS, entregas);

            if (pessoa.perfil.fotoPath != null) {
                servicos.arquivos.apagar(Arquivos.BUCKET_FOTOS, Collections.singletonList(pessoa.perfil.fotoPath));
            }

            if (pessoa.pedido != null) {
                servicos.perfis.decidirExclusao(pessoa.pedido.id, true);
            } else {
                servicos.perfis.excluir(pessoa.perfil.id);
            }

            return null;

        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {
                Toast.makeText(ListaUsuariosActivity.this, "\"" + pessoa.perfil.nome + "\" foi excluído(a).",
                        Toast.LENGTH_SHORT).show();
                carregarDados();
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(ListaUsuariosActivity.this, "Não foi possível excluir: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
                carregarDados();
            }
        });
    }

    private void recusarPedido(Pessoa pessoa) {

        new AlertDialog.Builder(this)
                .setTitle("Recusar pedido")
                .setMessage("Os dados de \"" + pessoa.perfil.nome + "\" continuam no app. Recusar o pedido?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Recusar", (dialog, which) -> Tarefas.executar(this, () -> {
                    Servicos.get(this).perfis.decidirExclusao(pessoa.pedido.id, false);
                    return null;
                }, new Tarefas.Retorno<Object>() {
                    @Override
                    public void sucesso(Object resultado) {
                        Toast.makeText(ListaUsuariosActivity.this, "Pedido recusado.", Toast.LENGTH_SHORT).show();
                        carregarDados();
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Toast.makeText(ListaUsuariosActivity.this, erro.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }))
                .show();
    }

    // =====================================================
    // EXCLUIR TURMA
    // =====================================================

    private void confirmarExclusaoTurma(Grupo grupo) {

        new AlertDialog.Builder(this)
                .setTitle("Excluir turma")
                .setMessage(
                        "Tem certeza que deseja excluir esta turma? "
                                + "Os alunos dela deixarão de ter turma, mas não serão excluídos."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> excluirTurma(grupo.turmaId))
                .show();
    }

    // O banco desliga a turma dos alunos e disciplinas (on delete set null)
    private void excluirTurma(String turmaId) {

        Tarefas.executar(this, () -> {
            Servicos.get(this).escola.excluirTurma(turmaId);
            return null;
        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {
                carregarDados();
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(ListaUsuariosActivity.this, "Erro ao excluir turma: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // =====================================================
    // MODELOS DA TELA
    // =====================================================

    private static final class Dados {

        final List<Perfil> professores;
        final List<Perfil> alunos;
        final List<Turma> turmas;
        final List<SolicitacaoExclusao> pedidos;

        Dados(List<Perfil> professores, List<Perfil> alunos, List<Turma> turmas,
              List<SolicitacaoExclusao> pedidos) {
            this.professores = professores;
            this.alunos = alunos;
            this.turmas = turmas;
            this.pedidos = pedidos;
        }
    }

    private static final class Transferencia {

        final int disciplinas;
        final List<Perfil> destinos;

        Transferencia(int disciplinas, List<Perfil> destinos) {
            this.disciplinas = disciplinas;
            this.destinos = destinos;
        }
    }

    private static class Pessoa {

        final Perfil perfil;
        // Pedido de exclusão aberto, quando a pessoa aparece no grupo de pedidos
        final SolicitacaoExclusao pedido;

        Pessoa(Perfil perfil, SolicitacaoExclusao pedido) {
            this.perfil = perfil;
            this.pedido = pedido;
        }
    }

    private static class Grupo {

        String titulo;
        final List<Pessoa> pessoas;
        final String turmaId;

        Grupo(String titulo, List<Pessoa> pessoas, String turmaId) {
            this.titulo = titulo;
            this.pessoas = pessoas;
            this.turmaId = turmaId;
        }
    }

    private class GrupoAdapter extends BaseExpandableListAdapter {

        @Override
        public int getGroupCount() {
            return grupos.size();
        }

        @Override
        public int getChildrenCount(int groupPosition) {
            return grupos.get(groupPosition).pessoas.size();
        }

        @Override
        public Object getGroup(int groupPosition) {
            return grupos.get(groupPosition);
        }

        @Override
        public Object getChild(int groupPosition, int childPosition) {
            return grupos.get(groupPosition).pessoas.get(childPosition);
        }

        @Override
        public long getGroupId(int groupPosition) {
            return groupPosition;
        }

        @Override
        public long getChildId(int groupPosition, int childPosition) {
            return childPosition;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(
                int groupPosition, boolean isExpanded, View convertView, ViewGroup parent
        ) {

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_grupo_usuario, parent, false);
            }

            Grupo grupo = grupos.get(groupPosition);

            TextView tvTituloGrupo = convertView.findViewById(R.id.tvTituloGrupo);
            ImageView imgSetaGrupo = convertView.findViewById(R.id.imgSetaGrupo);
            ImageView btnMenuGrupo = convertView.findViewById(R.id.btnMenuGrupo);

            tvTituloGrupo.setText(grupo.titulo);
            imgSetaGrupo.setRotation(isExpanded ? 270f : 180f);
            imgSetaGrupo.setVisibility(grupo.pessoas.isEmpty() ? View.INVISIBLE : View.VISIBLE);

            if (grupo.turmaId != null) {

                btnMenuGrupo.setVisibility(View.VISIBLE);
                btnMenuGrupo.setOnClickListener(v -> {

                    PopupMenu popupMenu = new PopupMenu(ListaUsuariosActivity.this, btnMenuGrupo);
                    popupMenu.getMenu().add("Excluir");
                    popupMenu.setOnMenuItemClickListener(item -> {
                        confirmarExclusaoTurma(grupo);
                        return true;
                    });
                    popupMenu.show();
                });

            } else {

                btnMenuGrupo.setVisibility(View.GONE);
                btnMenuGrupo.setOnClickListener(null);
            }

            return convertView;
        }

        @Override
        public View getChildView(
                int groupPosition, int childPosition, boolean isLastChild,
                View convertView, ViewGroup parent
        ) {

            if (convertView == null) {
                convertView = getLayoutInflater().inflate(R.layout.item_pessoa_usuario, parent, false);
            }

            Pessoa pessoa = grupos.get(groupPosition).pessoas.get(childPosition);

            TextView tvNomePessoa = convertView.findViewById(R.id.tvNomePessoa);
            TextView tvEmailPessoa = convertView.findViewById(R.id.tvEmailPessoa);
            ImageView btnMenuPessoa = convertView.findViewById(R.id.btnMenuPessoa);
            ImageView imgFotoPessoa = convertView.findViewById(R.id.imgFotoPessoa);

            // Quem ainda não entrou no app aparece marcado
            String nome = pessoa.perfil.nome + (pessoa.perfil.semConta() ? " (ainda não entrou)" : "")
                    + (pessoa.perfil.ativo ? "" : " (inativo)");
            String detalhe = pessoa.pedido != null && pessoa.pedido.motivo != null
                    ? pessoa.perfil.email + "\nMotivo: " + pessoa.pedido.motivo
                    : pessoa.perfil.email;

            tvNomePessoa.setText(nome);
            tvEmailPessoa.setText(detalhe);
            imgFotoPessoa.setImageResource(R.drawable.ic_pessoa);
            PerfilFotoUtil.mostrar(ListaUsuariosActivity.this, pessoa.perfil, imgFotoPessoa);

            btnMenuPessoa.setOnClickListener(v -> {

                PopupMenu popupMenu = new PopupMenu(ListaUsuariosActivity.this, btnMenuPessoa);

                if (pessoa.pedido != null) {
                    popupMenu.getMenu().add("Aprovar exclusão");
                    popupMenu.getMenu().add("Recusar pedido");
                } else {
                    popupMenu.getMenu().add("Excluir");
                }

                popupMenu.setOnMenuItemClickListener(item -> {

                    if ("Recusar pedido".contentEquals(item.getTitle())) {
                        recusarPedido(pessoa);
                    } else {
                        confirmarExclusaoPessoa(pessoa);
                    }

                    return true;
                });
                popupMenu.show();
            });

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
