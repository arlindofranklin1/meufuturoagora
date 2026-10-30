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

import java.util.ArrayList;
import java.util.List;

import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Diálogo do (i) ao lado do nome da disciplina: foto e nome do professor
// e a lista de alunos participantes (matriculados) da disciplina.
class InfoDisciplinaDialog {

    private InfoDisciplinaDialog() {
    }

    private static final class Carga {

        final Disciplina disciplina;
        final List<Participante> participantes;

        Carga(Disciplina disciplina, List<Participante> participantes) {
            this.disciplina = disciplina;
            this.participantes = participantes;
        }
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

        TextView tvTitulo = dialog.findViewById(R.id.tvTituloParticipantes);
        TextView tvVazio = dialog.findViewById(R.id.tvSemParticipantes);
        RecyclerView recycler = dialog.findViewById(R.id.recyclerParticipantes);

        List<Participante> alunos = new ArrayList<>();
        ParticipantesAdapter adapter = new ParticipantesAdapter(activity, alunos);

        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setAdapter(adapter);

        dialog.findViewById(R.id.btnFecharInfoProfessor).setOnClickListener(v -> dialog.dismiss());

        Servicos servicos = Servicos.get(activity);

        // Fecha o diálogo à toa se a tela sumir: Tarefas já descarta o resultado
        Tarefas.executar(activity, () -> new Carga(
                servicos.disciplinas.buscar(disciplinaId),
                servicos.disciplinas.participantes(disciplinaId)
        ), new Tarefas.Retorno<Carga>() {
            @Override
            public void sucesso(Carga carga) {

                if (carga.disciplina != null) {

                    tvDisciplina.setText(carga.disciplina.nome);
                    tvTurma.setText(carga.disciplina.turmaNome != null && !carga.disciplina.turmaNome.isEmpty()
                            ? carga.disciplina.turmaNome
                            : "—");
                }

                tvNome.setText("Professor");

                for (Participante p : carga.participantes) {

                    if ("professor".equals(p.tipo)) {

                        tvNome.setText(p.nome != null && !p.nome.isEmpty() ? p.nome : "Professor");

                        if (p.email != null && !p.email.isEmpty()) {
                            tvEmail.setText(p.email);
                            tvEmail.setVisibility(View.VISIBLE);
                        }

                        PerfilFotoUtil.mostrar(activity, p.fotoPath, p.fotoGoogleUrl, imgProfessor);

                    } else if ("aluno".equals(p.tipo)) {

                        alunos.add(p);
                    }
                }

                alunos.sort((a, b) -> a.nome.compareToIgnoreCase(b.nome));

                tvTitulo.setText("Participantes (" + alunos.size() + ")");
                tvVazio.setText("Nenhum aluno participando ainda.");
                tvVazio.setVisibility(alunos.isEmpty() ? View.VISIBLE : View.GONE);
                adapter.notifyDataSetChanged();
            }

            @Override
            public void erro(ErroApi erro) {
                tvVazio.setVisibility(View.VISIBLE);
                tvVazio.setText("Não foi possível carregar os participantes.");
            }
        });

        dialog.show();
    }

    private static class ParticipantesAdapter extends RecyclerView.Adapter<ParticipantesAdapter.ViewHolder> {

        private final Activity activity;
        private final List<Participante> participantes;

        ParticipantesAdapter(Activity activity, List<Participante> participantes) {
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

            Participante participante = participantes.get(position);

            holder.tvNome.setText(participante.nome);
            com.bumptech.glide.Glide.with(activity).clear(holder.imgFoto);
            holder.imgFoto.setImageResource(R.drawable.ic_perfil);
            PerfilFotoUtil.mostrar(activity, participante.fotoPath, participante.fotoGoogleUrl, holder.imgFoto);
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
