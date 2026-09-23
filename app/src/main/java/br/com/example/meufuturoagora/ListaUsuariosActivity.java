package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                        listaProfessores.add(pessoaDoDocumento(documento));
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

                        gruposPorTurmaId.put(documento.getId(), new Grupo("Turma " + nome, new ArrayList<>()));
                    }

                    db.collection("alunos")
                            .get()
                            .addOnSuccessListener(alunos -> {

                                List<Pessoa> semTurma = new ArrayList<>();

                                for (QueryDocumentSnapshot documento : alunos) {

                                    Pessoa pessoa = pessoaDoDocumento(documento);
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

        grupos.add(new Grupo("Professores (" + listaProfessores.size() + ")", listaProfessores));

        for (Grupo grupo : gruposPorTurmaId.values()) {

            grupo.titulo = grupo.titulo + " (" + grupo.pessoas.size() + " alunos)";
            grupos.add(grupo);
        }

        if (!semTurma.isEmpty()) {
            grupos.add(new Grupo("Sem turma (" + semTurma.size() + ")", semTurma));
        }

        adapter.notifyDataSetChanged();

        boolean semNada = listaProfessores.isEmpty() && gruposPorTurmaId.isEmpty() && semTurma.isEmpty();
        tvSemUsuarios.setVisibility(semNada ? View.VISIBLE : View.GONE);
        expandableUsuarios.setVisibility(semNada ? View.GONE : View.VISIBLE);
    }

    private Pessoa pessoaDoDocumento(DocumentSnapshot documento) {

        String nome = documento.getString("nome");
        String email = documento.getString("email");

        return new Pessoa(
                nome != null ? nome : "(sem nome)",
                email != null ? email : ""
        );
    }

    private static class Pessoa {

        final String nome;
        final String email;

        Pessoa(String nome, String email) {
            this.nome = nome;
            this.email = email;
        }
    }

    private static class Grupo {

        String titulo;
        final List<Pessoa> pessoas;

        Grupo(String titulo, List<Pessoa> pessoas) {
            this.titulo = titulo;
            this.pessoas = pessoas;
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

            tvTituloGrupo.setText(grupo.titulo);
            imgSetaGrupo.setRotation(isExpanded ? 270f : 180f);
            imgSetaGrupo.setVisibility(grupo.pessoas.isEmpty() ? View.INVISIBLE : View.VISIBLE);

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

            tvNomePessoa.setText(pessoa.nome);
            tvEmailPessoa.setText(pessoa.email);

            return convertView;
        }

        @Override
        public boolean isChildSelectable(int groupPosition, int childPosition) {
            return false;
        }
    }
}
