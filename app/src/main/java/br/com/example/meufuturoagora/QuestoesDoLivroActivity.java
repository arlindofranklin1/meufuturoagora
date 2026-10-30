package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import br.com.example.meufuturoagora.dados.Arquivos;
import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class QuestoesDoLivroActivity extends AppCompatActivity {

    private String atividadeId;
    private String disciplinaId;

    private String arquivoPath;

    private TextView tabDescricao, tabEntregas;
    private View linhaAbaDescricao, linhaAbaEntregas;

    private View scrollDescricao;
    private RecyclerView recyclerEntregas;
    private TextView tvSemEntregas;

    private EntregaAdapter adapter;
    private final List<Entrega> listaEntregas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_questoes_do_livro);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        atividadeId = getIntent().getStringExtra("atividadeId");
        disciplinaId = getIntent().getStringExtra("disciplinaId");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        TextView tvTituloAtividade = findViewById(R.id.tvTituloAtividade);

        String nomeAtividade = getIntent().getStringExtra("atividadeNome");

        if (nomeAtividade != null && !nomeAtividade.isEmpty()) {
            tvTituloAtividade.setText(nomeAtividade);
        }

        tabDescricao = findViewById(R.id.tabDescricao);
        tabEntregas = findViewById(R.id.tabEntregas);
        linhaAbaDescricao = findViewById(R.id.linhaAbaDescricao);
        linhaAbaEntregas = findViewById(R.id.linhaAbaEntregas);

        scrollDescricao = findViewById(R.id.scrollDescricao);
        recyclerEntregas = findViewById(R.id.recyclerEntregas);
        tvSemEntregas = findViewById(R.id.tvSemEntregas);

        recyclerEntregas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EntregaAdapter(listaEntregas);
        recyclerEntregas.setAdapter(adapter);

        tabDescricao.setOnClickListener(v -> mostrarAba(true));
        tabEntregas.setOnClickListener(v -> mostrarAba(false));

        findViewById(R.id.itemAnexo).setOnClickListener(v -> abrirAnexo());

        carregarTela();
    }

    private void mostrarAba(boolean descricao) {

        scrollDescricao.setVisibility(descricao ? View.VISIBLE : View.GONE);
        recyclerEntregas.setVisibility(
                !descricao && !listaEntregas.isEmpty() ? View.VISIBLE : View.GONE
        );
        tvSemEntregas.setVisibility(
                !descricao && listaEntregas.isEmpty() ? View.VISIBLE : View.GONE
        );

        tabDescricao.setTextColor(getColor(
                descricao ? R.color.roxo_primario : R.color.texto_escuro
        ));

        tabEntregas.setTextColor(getColor(
                !descricao ? R.color.roxo_primario : R.color.texto_escuro
        ));

        linhaAbaDescricao.setVisibility(descricao ? View.VISIBLE : View.GONE);
        linhaAbaEntregas.setVisibility(!descricao ? View.VISIBLE : View.GONE);
    }

    private void abrirAnexo() {

        if (arquivoPath == null || arquivoPath.isEmpty()) {

            Toast.makeText(this, "Nenhum anexo disponível.", Toast.LENGTH_SHORT).show();
            return;
        }

        ArquivoUtil.abrir(this, Arquivos.BUCKET_ATIVIDADES, arquivoPath);
    }

    private void preencherAtividade(Atividade atividade) {

        arquivoPath = atividade.arquivoPath;

        TextView tvTituloAtividade = findViewById(R.id.tvTituloAtividade);
        if (atividade.nome != null) {
            tvTituloAtividade.setText(atividade.nome);
        }

        TextView tvDescricaoAtividade = findViewById(R.id.tvDescricaoAtividade);
        tvDescricaoAtividade.setText(
                atividade.descricao != null && !atividade.descricao.isEmpty()
                        ? atividade.descricao
                        : "Sem descrição."
        );

        TextView tvNomeAnexo = findViewById(R.id.tvNomeAnexo);
        tvNomeAnexo.setText(
                atividade.arquivoNome != null && !atividade.arquivoNome.isEmpty()
                        ? atividade.arquivoNome
                        : "Nenhum anexo"
        );

        TextView tvPontuacaoMaxima = findViewById(R.id.tvPontuacaoMaxima);
        tvPontuacaoMaxima.setText(atividade.pontos + " pts");

        TextView tvPrazoFinal = findViewById(R.id.tvPrazoFinal);
        tvPrazoFinal.setText(atividade.prazo != null ? Datas.formatar(atividade.prazo) : "");
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Volta da tela de avaliação: o status do aluno mudou
        if (adapter != null) {
            carregarTela();
        }
    }

    private static final class Carga {

        final Atividade atividade;
        final List<Entrega> entregas;

        Carga(Atividade atividade, List<Entrega> entregas) {
            this.atividade = atividade;
            this.entregas = entregas;
        }
    }

    // A trilha, os alunos da disciplina (com foto) e quem já entregou
    private void carregarTela() {

        if (atividadeId == null) {
            return;
        }

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            Atividade atividade = servicos.atividades.buscar(atividadeId);
            Map<String, br.com.example.meufuturoagora.dados.Entrega> porAluno = new HashMap<>();

            for (br.com.example.meufuturoagora.dados.Entrega e : servicos.atividades.entregasDaAtividade(atividadeId)) {
                porAluno.put(e.alunoId, e);
            }

            List<Entrega> lista = new ArrayList<>();

            if (disciplinaId != null) {

                for (Participante p : servicos.disciplinas.participantes(disciplinaId)) {

                    if (!"aluno".equals(p.tipo)) {
                        continue;
                    }

                    br.com.example.meufuturoagora.dados.Entrega entrega = porAluno.get(p.perfilId);

                    lista.add(new Entrega(p.perfilId, p.nome, p.fotoPath, p.fotoGoogleUrl,
                            entrega != null, entrega != null && entrega.avaliada()));
                }
            }

            return new Carga(atividade, lista);

        }, new Tarefas.Retorno<Carga>() {
            @Override
            public void sucesso(Carga carga) {

                if (carga.atividade != null) {
                    preencherAtividade(carga.atividade);
                }

                listaEntregas.clear();
                listaEntregas.addAll(carga.entregas);
                adapter.notifyDataSetChanged();

                boolean vazio = listaEntregas.isEmpty();
                boolean abaEntregasAtiva =
                        recyclerEntregas.getVisibility() == View.VISIBLE
                                || tvSemEntregas.getVisibility() == View.VISIBLE;

                if (abaEntregasAtiva) {

                    recyclerEntregas.setVisibility(vazio ? View.GONE : View.VISIBLE);
                    tvSemEntregas.setVisibility(vazio ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(QuestoesDoLivroActivity.this, "Erro ao carregar trilha: " + erro.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    // =====================================================
    // REFAZER AVALIAÇÃO
    // =====================================================

    private void confirmarRefazerAvaliacao(Entrega entrega) {

        new AlertDialog.Builder(this)
                .setTitle("Refazer avaliação")
                .setMessage("A nota e o comentário dados para " + entrega.alunoNome
                        + " serão apagados e a entrega volta a ficar aguardando avaliação. "
                        + "Deseja refazer a avaliação?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Refazer", (dialog, which) -> refazerAvaliacao(entrega))
                .show();
    }

    private void refazerAvaliacao(Entrega entrega) {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            // A RPC pede o id da entrega, que o app acha pela trilha e pelo aluno
            for (br.com.example.meufuturoagora.dados.Entrega e : servicos.atividades.entregasDaAtividade(atividadeId)) {

                if (entrega.alunoId.equals(e.alunoId)) {

                    // A nota antiga sai da pontuação do aluno no próprio servidor
                    servicos.atividades.refazerAvaliacao(e.id);
                    return true;
                }
            }

            return false;

        }, new Tarefas.Retorno<Boolean>() {
            @Override
            public void sucesso(Boolean refeita) {

                if (!refeita) {

                    Toast.makeText(QuestoesDoLivroActivity.this, "Entrega não encontrada.", Toast.LENGTH_SHORT).show();
                    return;
                }

                // Abre direto a tela de avaliação para dar a nova nota
                startActivity(new Intent(QuestoesDoLivroActivity.this, AvaliarEntregaActivity.class)
                        .putExtra("atividadeId", atividadeId)
                        .putExtra("alunoId", entrega.alunoId)
                        .putExtra("alunoNome", entrega.alunoNome));
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(QuestoesDoLivroActivity.this,
                        "Erro ao refazer avaliação: " + erro.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    public static class Entrega {

        String alunoId;
        String alunoNome;
        String fotoPath;
        String fotoGoogleUrl;
        boolean entregue;
        boolean avaliado;

        public Entrega(String alunoId, String alunoNome, String fotoPath, String fotoGoogleUrl,
                       boolean entregue, boolean avaliado) {
            this.alunoId = alunoId;
            this.alunoNome = alunoNome;
            this.fotoPath = fotoPath;
            this.fotoGoogleUrl = fotoGoogleUrl;
            this.entregue = entregue;
            this.avaliado = avaliado;
        }
    }

    private class EntregaAdapter extends RecyclerView.Adapter<EntregaAdapter.ViewHolder> {

        private final List<Entrega> entregas;

        public EntregaAdapter(List<Entrega> entregas) {
            this.entregas = entregas;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(R.layout.item_entrega, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            Entrega entrega = entregas.get(position);

            holder.tvNomeAlunoEntrega.setText(entrega.alunoNome);
            holder.imgAlunoEntrega.setImageResource(R.drawable.ic_perfil);
            PerfilFotoUtil.mostrar(QuestoesDoLivroActivity.this, entrega.fotoPath, entrega.fotoGoogleUrl,
                    holder.imgAlunoEntrega);

            if (entrega.entregue) {

                holder.tvStatusEntrega.setText(entrega.avaliado ? "Avaliado" : "Entregue");
                holder.tvStatusEntrega.setBackgroundResource(R.drawable.bg_pill_verde);
                holder.tvStatusEntrega.setTextColor(getColor(R.color.verde_sucesso));

            } else {

                holder.tvStatusEntrega.setText("Não entregue");
                holder.tvStatusEntrega.setBackgroundResource(R.drawable.bg_pill_vermelho);
                holder.tvStatusEntrega.setTextColor(getColor(R.color.vermelho_erro));
            }

            // Entrega avaliada: três pontinhos com a opção de refazer a avaliação
            holder.btnMenuEntrega.setVisibility(
                    entrega.entregue && entrega.avaliado ? View.VISIBLE : View.GONE
            );

            holder.btnMenuEntrega.setOnClickListener(v -> {

                PopupMenu menu = new PopupMenu(QuestoesDoLivroActivity.this, holder.btnMenuEntrega);
                menu.getMenu().add("Refazer avaliação");

                menu.setOnMenuItemClickListener(item -> {
                    confirmarRefazerAvaliacao(entrega);
                    return true;
                });

                menu.show();
            });

            holder.itemView.setOnClickListener(v -> {

                if (!entrega.entregue) {

                    Toast.makeText(
                            QuestoesDoLivroActivity.this,
                            "Este aluno ainda não entregou a trilha.",
                            Toast.LENGTH_SHORT
                    ).show();

                    return;
                }

                Intent intent = new Intent(
                        QuestoesDoLivroActivity.this,
                        AvaliarEntregaActivity.class
                );

                intent.putExtra("atividadeId", atividadeId);
                intent.putExtra("alunoId", entrega.alunoId);
                intent.putExtra("alunoNome", entrega.alunoNome);

                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return entregas.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvNomeAlunoEntrega;
            TextView tvStatusEntrega;
            ImageView imgAlunoEntrega;
            ImageView btnMenuEntrega;

            public ViewHolder(View itemView) {
                super(itemView);

                btnMenuEntrega = itemView.findViewById(R.id.btnMenuEntrega);

                tvNomeAlunoEntrega = itemView.findViewById(R.id.tvNomeAlunoEntrega);
                tvStatusEntrega = itemView.findViewById(R.id.tvStatusEntrega);
                imgAlunoEntrega = itemView.findViewById(R.id.imgAlunoEntrega);
            }
        }
    }
}
