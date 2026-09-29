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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ListaUsuariosActivity extends AppCompatActivity {

    private FirebaseFirestore db;
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

        db = FirebaseFirestore.getInstance();

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        expandableUsuarios = findViewById(R.id.expandableUsuarios);
        tvSemUsuarios = findViewById(R.id.tvSemUsuarios);

        adapter = new GrupoAdapter();
        expandableUsuarios.setAdapter(adapter);

        carregarDados();
    }

    private void carregarDados() {

        db.collection("professores")
                .get()
                .addOnSuccessListener(professores -> {

                    List<Pessoa> listaProfessores = new ArrayList<>();

                    for (QueryDocumentSnapshot documento : professores) {
                        listaProfessores.add(pessoaDoDocumento(documento, "professores"));
                    }

                    carregarTurmasEAlunos(listaProfessores);
                });
    }

    private void carregarTurmasEAlunos(List<Pessoa> listaProfessores) {

        db.collection("turmas")
                .get()
                .addOnSuccessListener(turmas -> {

                    Map<String, Grupo> gruposPorTurmaId = new LinkedHashMap<>();

                    for (QueryDocumentSnapshot documento : turmas) {

                        String nome = documento.getString("nome");

                        if (nome == null) {
                            continue;
                        }

                        gruposPorTurmaId.put(
                                documento.getId(),
                                new Grupo("Turma " + nome, new ArrayList<>(), documento.getId())
                        );
                    }

                    db.collection("alunos")
                            .get()
                            .addOnSuccessListener(alunos -> {

                                List<Pessoa> semTurma = new ArrayList<>();

                                // E-mails de alunos que já entraram no app (a conta tem pontuação).
                                // O cadastro antigo desses alunos é escondido até ser juntado no próximo login.
                                Set<String> emailsComConta = new HashSet<>();

                                for (QueryDocumentSnapshot documento : alunos) {

                                    String email = documento.getString("email");

                                    if (email != null && documento.getLong("pontuacao") != null) {
                                        emailsComConta.add(email.toLowerCase());
                                    }
                                }

                                for (QueryDocumentSnapshot documento : alunos) {

                                    String email = documento.getString("email");

                                    if (email != null && documento.getLong("pontuacao") == null
                                            && emailsComConta.contains(email.toLowerCase())) {
                                        continue;
                                    }

                                    Pessoa pessoa = pessoaDoDocumento(documento, "alunos");
                                    String turmaId = documento.getString("turmaId");

                                    Grupo grupo = turmaId != null ? gruposPorTurmaId.get(turmaId) : null;

                                    if (grupo != null) {
                                        grupo.pessoas.add(pessoa);
                                    } else {
                                        semTurma.add(pessoa);
                                    }
                                }

                                montarGrupos(listaProfessores, gruposPorTurmaId, semTurma);
                            });
                });
    }

    private void montarGrupos(
            List<Pessoa> listaProfessores,
            Map<String, Grupo> gruposPorTurmaId,
            List<Pessoa> semTurma
    ) {

        grupos.clear();

        grupos.add(new Grupo("Professores (" + listaProfessores.size() + ")", listaProfessores, null));

        for (Grupo grupo : gruposPorTurmaId.values()) {

            grupo.titulo = grupo.titulo + " (" + grupo.pessoas.size() + " alunos)";
            grupos.add(grupo);
        }

        if (!semTurma.isEmpty()) {
            grupos.add(new Grupo("Sem turma (" + semTurma.size() + ")", semTurma, null));
        }

        adapter.notifyDataSetChanged();

        boolean semNada = listaProfessores.isEmpty() && gruposPorTurmaId.isEmpty() && semTurma.isEmpty();
        tvSemUsuarios.setVisibility(semNada ? View.VISIBLE : View.GONE);
        expandableUsuarios.setVisibility(semNada ? View.GONE : View.VISIBLE);
    }

    private Pessoa pessoaDoDocumento(DocumentSnapshot documento, String colecao) {

        String nome = documento.getString("nome");
        String email = documento.getString("email");
        String fotoUrl = documento.getString("fotoUrl");

        return new Pessoa(
                documento.getId(),
                colecao,
                nome != null ? nome : "(sem nome)",
                email != null ? email : "",
                fotoUrl
        );
    }

    // =====================================================
    // EXCLUIR
    // =====================================================

    private void confirmarExclusaoPessoa(Pessoa pessoa) {

        String tipo = "professores".equals(pessoa.colecao) ? "professor" : "aluno";

        new AlertDialog.Builder(this)
                .setTitle("Excluir " + tipo)
                .setMessage("Tem certeza que deseja excluir \"" + pessoa.nome + "\"?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> {

                    if ("alunos".equals(pessoa.colecao) && !pessoa.email.isEmpty()) {
                        excluirRegistrosDoAluno(pessoa);
                        return;
                    }

                    db.collection(pessoa.colecao)
                            .document(pessoa.id)
                            .delete()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(this, "Excluído!", Toast.LENGTH_SHORT).show();
                                carregarDados();
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_LONG
                            ).show());
                })
                .show();
    }

    // Apaga a conta e o cadastro do aluno (mesmo e-mail), para ele não conseguir mais entrar
    private void excluirRegistrosDoAluno(Pessoa pessoa) {

        db.collection("alunos")
                .whereEqualTo("email", pessoa.email)
                .get()
                .addOnSuccessListener(registros -> {

                    WriteBatch lote = db.batch();

                    lote.delete(db.collection("alunos").document(pessoa.id));

                    for (QueryDocumentSnapshot documento : registros) {
                        lote.delete(documento.getReference());
                    }

                    lote.commit()
                            .addOnSuccessListener(unused -> {

                                Toast.makeText(this, "Excluído!", Toast.LENGTH_SHORT).show();
                                carregarDados();
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_LONG
                            ).show());
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao excluir: " + e.getMessage(), Toast.LENGTH_LONG
                ).show());
    }

    private void confirmarExclusaoTurma(Grupo grupo) {

        new AlertDialog.Builder(this)
                .setTitle("Excluir turma")
                .setMessage(
                        "Tem certeza que deseja excluir esta turma? "
                                + "Os alunos dela deixarão de ter turma, mas não serão excluídos."
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) ->
                        db.collection("turmas")
                                .document(grupo.turmaId)
                                .delete()
                                .addOnSuccessListener(unused -> {

                                    Toast.makeText(this, "Turma excluída!", Toast.LENGTH_SHORT).show();
                                    carregarDados();
                                })
                                .addOnFailureListener(e -> Toast.makeText(
                                        this, "Erro ao excluir turma: " + e.getMessage(), Toast.LENGTH_LONG
                                ).show())
                )
                .show();
    }

    private static class Pessoa {

        final String id;
        final String colecao;
        final String nome;
        final String email;
        final String fotoUrl;

        Pessoa(String id, String colecao, String nome, String email, String fotoUrl) {
            this.id = id;
            this.colecao = colecao;
            this.nome = nome;
            this.email = email;
            this.fotoUrl = fotoUrl;
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

            tvNomePessoa.setText(pessoa.nome);
            tvEmailPessoa.setText(pessoa.email);
            imgFotoPessoa.setImageResource(R.drawable.ic_pessoa);
            FotoUtil.carregar(ListaUsuariosActivity.this, pessoa.fotoUrl, imgFotoPessoa);

            btnMenuPessoa.setOnClickListener(v -> {

                PopupMenu popupMenu = new PopupMenu(ListaUsuariosActivity.this, btnMenuPessoa);
                popupMenu.getMenu().add("Excluir");
                popupMenu.setOnMenuItemClickListener(item -> {
                    confirmarExclusaoPessoa(pessoa);
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
