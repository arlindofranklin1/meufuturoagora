package br.com.example.meufuturoagora;

import android.app.Activity;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

// Diálogo do (i) ao lado do nome da disciplina: foto e nome do professor
// e a lista de alunos participantes (matriculados) da disciplina.
class InfoDisciplinaDialog {

    private InfoDisciplinaDialog() {
    }

    static void mostrar(Activity activity, String disciplinaId) {

        if (disciplinaId == null) {
            return;
        }

        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_info_professor);

        Window janela = dialog.getWindow();

        if (janela != null) {

            // Fundo transparente para as bordas arredondadas aparecerem
            janela.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            int largura = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f);
            janela.setLayout(largura, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        ImageView imgProfessor = dialog.findViewById(R.id.imgInfoProfessor);
        TextView tvNome = dialog.findViewById(R.id.tvNomeInfoProfessor);
        TextView tvEmail = dialog.findViewById(R.id.tvEmailInfoProfessor);
        TextView tvDisciplina = dialog.findViewById(R.id.tvDisciplinaInfoProfessor);
        TextView tvTurma = dialog.findViewById(R.id.tvTurmaInfoProfessor);

        dialog.findViewById(R.id.btnFecharInfoProfessor).setOnClickListener(v -> dialog.dismiss());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("disciplinas")
                .document(disciplinaId)
                .get()
                .addOnSuccessListener(disciplina -> {

                    if (!disciplina.exists()) {
                        return;
                    }

                    String professorNome = disciplina.getString("professorNome");
                    String turma = disciplina.getString("turmaNome");

                    tvNome.setText(professorNome != null ? professorNome : "Professor");
                    tvDisciplina.setText(disciplina.getString("nome"));
                    tvTurma.setText(turma != null && !turma.isEmpty() ? turma : "—");

                    carregarProfessor(activity, db, disciplina, imgProfessor, tvNome, tvEmail);
                });

        carregarParticipantes(activity, dialog, db, disciplinaId);

        dialog.show();
    }

    // Foto e e-mail vêm do cadastro do professor (pelo uid da conta ou pelo e-mail)
    private static void carregarProfessor(Activity activity, FirebaseFirestore db,
                                          DocumentSnapshot disciplina, ImageView imgProfessor,
                                          TextView tvNome, TextView tvEmail) {

        String professorId = disciplina.getString("professorId");
        String professorEmail = disciplina.getString("professorEmail");

        if (professorId == null) {
            return;
        }

        db.collection("professores")
                .whereEqualTo("uid", professorId)
                .limit(1)
                .get()
                .addOnSuccessListener(porUid -> {

                    if (!porUid.isEmpty()) {
                        preencherProfessor(activity, porUid.getDocuments().get(0), imgProfessor, tvNome, tvEmail);
                        return;
                    }

                    if (professorEmail == null || professorEmail.isEmpty()) {
                        return;
                    }

                    db.collection("professores")
                            .whereEqualTo("email", professorEmail)
                            .limit(1)
                            .get()
                            .addOnSuccessListener(porEmail -> {

                                if (!porEmail.isEmpty()) {
                                    preencherProfessor(activity, porEmail.getDocuments().get(0),
                                            imgProfessor, tvNome, tvEmail);
                                }
                            });
                });
    }

    private static void preencherProfessor(Activity activity, DocumentSnapshot professor,
                                           ImageView imgProfessor, TextView tvNome, TextView tvEmail) {

        String nome = professor.getString("nome");
        String email = professor.getString("email");

        if (nome != null && !nome.isEmpty()) {
            tvNome.setText(nome);
        }

        if (email != null && !email.isEmpty()) {
            tvEmail.setText(email);
            tvEmail.setVisibility(View.VISIBLE);
        }

        PerfilFotoUtil.mostrar(activity, PerfilFotoUtil.fotoDoDocumento(professor), imgProfessor);
    }

    private static void carregarParticipantes(Activity activity, Dialog dialog,
                                              FirebaseFirestore db, String disciplinaId) {

        TextView tvTitulo = dialog.findViewById(R.id.tvTituloParticipantes);
        TextView tvVazio = dialog.findViewById(R.id.tvSemParticipantes);
        RecyclerView recycler = dialog.findViewById(R.id.recyclerParticipantes);

        List<String[]> participantes = new ArrayList<>(); // {alunoId, nome, fotoUrl}
        ParticipantesAdapter adapter = new ParticipantesAdapter(activity, participantes);

        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setAdapter(adapter);

        db.collection("matriculas")
                .whereEqualTo("disciplinaId", disciplinaId)
                .get()
                .addOnSuccessListener(matriculas -> {

                    List<String> ids = new ArrayList<>();

                    for (DocumentSnapshot matricula : matriculas) {

                        String alunoId = matricula.getString("alunoId");
                        String nome = matricula.getString("alunoNome");

                        if (alunoId != null && !ids.contains(alunoId)) {
                            ids.add(alunoId);
                            participantes.add(new String[]{alunoId, nome != null ? nome : "Aluno", null});
                        }
                    }

                    participantes.sort((a, b) -> a[1].compareToIgnoreCase(b[1]));

                    tvTitulo.setText("Participantes (" + participantes.size() + ")");
                    tvVazio.setText("Nenhum aluno participando ainda.");
                    tvVazio.setVisibility(participantes.isEmpty() ? View.VISIBLE : View.GONE);
                    adapter.notifyDataSetChanged();

                    carregarFotos(db, ids, participantes, adapter);
                })
                .addOnFailureListener(e -> tvVazio.setText("Não foi possível carregar os participantes."));
    }

    // Fotos (e nomes atualizados) dos alunos, em lotes de 30 (limite do whereIn)
    private static void carregarFotos(FirebaseFirestore db, List<String> ids,
                                      List<String[]> participantes, ParticipantesAdapter adapter) {

        for (int i = 0; i < ids.size(); i += 30) {

            List<String> lote = ids.subList(i, Math.min(i + 30, ids.size()));

            db.collection("alunos")
                    .whereIn(FieldPath.documentId(), new ArrayList<>(lote))
                    .get()
                    .addOnSuccessListener(alunos -> {

                        for (DocumentSnapshot aluno : alunos) {

                            for (String[] participante : participantes) {

                                if (participante[0].equals(aluno.getId())) {

                                    String nome = aluno.getString("nome");

                                    if (nome != null && !nome.isEmpty()) {
                                        participante[1] = nome;
                                    }

                                    participante[2] = aluno.getString("fotoUrl");
                                }
                            }
                        }

                        adapter.notifyDataSetChanged();
                    });
        }
    }

    private static class ParticipantesAdapter extends RecyclerView.Adapter<ParticipantesAdapter.ViewHolder> {

        private final Activity activity;
        private final List<String[]> participantes;

        ParticipantesAdapter(Activity activity, List<String[]> participantes) {
            this.activity = activity;
            this.participantes = participantes;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return new ViewHolder(activity.getLayoutInflater()
                    .inflate(R.layout.item_participante, parent, false));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            String[] participante = participantes.get(position);

            holder.tvNome.setText(participante[1]);
            com.bumptech.glide.Glide.with(activity).clear(holder.imgFoto);
            holder.imgFoto.setImageResource(R.drawable.ic_perfil);
            PerfilFotoUtil.mostrar(activity, participante[2], holder.imgFoto);
        }

        @Override
        public int getItemCount() {
            return participantes.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            final ImageView imgFoto;
            final TextView tvNome;

            ViewHolder(View itemView) {
                super(itemView);
                imgFoto = itemView.findViewById(R.id.imgParticipante);
                tvNome = itemView.findViewById(R.id.tvNomeParticipante);
            }
        }
    }
}
