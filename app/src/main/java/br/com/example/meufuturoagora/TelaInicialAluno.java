package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Frequencia;
import br.com.example.meufuturoagora.dados.PosicaoRanking;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class TelaInicialAluno extends AppCompatActivity {

    // Quantas trilhas recentes aparecem na tela
    private static final int QUANTIDADE_PROXIMAS = 2;

    private TextView tvSaudacaoAluno;
    private TextView tvPontuacaoTotalAluno;
    private TextView tvPosicaoRankingAluno;
    private TextView tvFrequenciaPercentual;
    private ProgressBar progressFrequencia;

    private RecyclerView recyclerProximasAtividades;
    private TextView tvSemAtividadesAluno;

    private ProximaAtividadeAdapter adapter;
    private final List<ProximaAtividade> listaProximas = new ArrayList<>();

    @Override
    protected void onResume() {
        super.onResume();

        // Atualiza a foto ao voltar do perfil (o aluno pode ter trocado)
        PerfilFotoUtil.carregarFotoDoUsuario(this, findViewById(R.id.imgAluno));
    }

    @Override
    protected void onStart() {
        super.onStart();

        carregarTela();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_inicial_aluno);

        ImageView imgAluno = findViewById(R.id.imgAluno);
        tvSaudacaoAluno = findViewById(R.id.tvSaudacaoAluno);

        tvPontuacaoTotalAluno = findViewById(R.id.tvPontuacaoTotalAluno);
        tvPosicaoRankingAluno = findViewById(R.id.tvPosicaoRankingAluno);
        tvFrequenciaPercentual = findViewById(R.id.tvFrequenciaPercentual);
        progressFrequencia = findViewById(R.id.progressFrequencia);

        recyclerProximasAtividades = findViewById(R.id.recyclerProximasAtividades);
        tvSemAtividadesAluno = findViewById(R.id.tvSemAtividadesAluno);

        recyclerProximasAtividades.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProximaAtividadeAdapter(listaProximas);
        recyclerProximasAtividades.setAdapter(adapter);

        PeriodoAtual.mostrar(
                this,
                findViewById(R.id.tvTituloBimestre),
                findViewById(R.id.tvPeriodoBimestre)
        );

        // Notificações de novidades (respeita o switch das configurações)
        NotificacaoUtil.iniciar(this, NotificacaoUtil.PERFIL_ALUNO);

        // Sino: abre a lista de notificações; bolinha laranja = tem notificação não vista
        View bolinhaNotificacao = findViewById(R.id.bolinhaNotificacao);
        NotificacaoHistorico.vincularBolinha(this, bolinhaNotificacao);
        findViewById(R.id.btnNotificacoes).setOnClickListener(v ->
                NotificacoesDialog.mostrar(this, bolinhaNotificacao)
        );

        findViewById(R.id.btnMeuDesempenho).setOnClickListener(v -> startActivity(
                new Intent(this, DesempenhoActivity.class)
                        .putExtra(
                                DesempenhoActivity.EXTRA_PERFIL,
                                DesempenhoActivity.PERFIL_ALUNO
                        )
        ));

        // Tocar na foto abre o perfil (a foto é carregada no onResume)
        imgAluno.setOnClickListener(v ->
                startActivity(new Intent(this, TelaPerfilAluno.class))
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

        bottomNavigation.setItemIconTintList(null);
        bottomNavigation.setSelectedItemId(R.id.nav_inicio);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                return true;

            } else if (id == R.id.nav_disciplinas) {

                startActivity(new Intent(this, TelaDisciplinaAluno.class));
                return true;

            } else if (id == R.id.nav_diario) {

                startActivity(new Intent(this, DiarioAlunoActivity.class));
                return true;

            } else if (id == R.id.nav_ranking) {

                startActivity(new Intent(this, RankingActivity.class)
                        .putExtra(RankingActivity.EXTRA_PERFIL, RankingActivity.PERFIL_ALUNO));
                return true;

            } else if (id == R.id.nav_perfil) {

                startActivity(new Intent(this, TelaPerfilAluno.class));
                return true;
            }

            return false;
        });
    }

    private String primeiroNome(String nomeCompleto) {

        int espaco = nomeCompleto.indexOf(' ');
        return espaco > 0 ? nomeCompleto.substring(0, espaco) : nomeCompleto;
    }

    // Tudo o que a tela mostra, lido de uma vez
    private static final class Carga {

        final String nome;
        final int pontos;
        final PosicaoRanking posicao;
        final int faltas;
        final int aulasComRegistro;
        final List<ProximaAtividade> proximas;

        Carga(String nome, int pontos, PosicaoRanking posicao, int faltas, int aulasComRegistro,
              List<ProximaAtividade> proximas) {
            this.nome = nome;
            this.pontos = pontos;
            this.posicao = posicao;
            this.faltas = faltas;
            this.aulasComRegistro = aulasComRegistro;
            this.proximas = proximas;
        }
    }

    private void carregarTela() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            br.com.example.meufuturoagora.dados.Perfil eu = servicos.perfis.meu();

            int faltas = 0;
            List<Frequencia> frequencias = servicos.frequencias.frequencias();

            for (Frequencia f : frequencias) {

                if (Frequencia.FALTA.equals(f.status)) {
                    faltas++;
                }
            }

            // Só as disciplinas ativas em que o aluno está matriculado
            List<Atividade> todas = new ArrayList<>();
            java.util.Map<String, String> nomes = new java.util.HashMap<>();

            for (Disciplina d : servicos.disciplinas.listar()) {

                if (!d.ativo) {
                    continue;
                }

                nomes.put(d.id, d.nome != null ? d.nome : "Disciplina");
                todas.addAll(servicos.atividades.daDisciplina(d.id));
            }

            // As mais recentemente criadas primeiro
            Collections.sort(todas, (a, b) -> {

                if (a.criadoEm == null || b.criadoEm == null) {
                    return 0;
                }

                return b.criadoEm.compareTo(a.criadoEm);
            });

            List<ProximaAtividade> proximas = new ArrayList<>();

            for (Atividade a : todas.subList(0, Math.min(QUANTIDADE_PROXIMAS, todas.size()))) {

                proximas.add(new ProximaAtividade(
                        a.id,
                        a.nome,
                        nomes.get(a.disciplinaId),
                        a.prazo != null ? Datas.formatar(a.prazo) : ""
                ));
            }

            return new Carga(
                    eu != null ? eu.nome : null,
                    servicos.ranking.pontuacao(eu.id, null),
                    servicos.ranking.minhaPosicao(),
                    faltas,
                    frequencias.size(),
                    proximas
            );

        }, new Tarefas.Retorno<Carga>() {
            @Override
            public void sucesso(Carga carga) {

                tvSaudacaoAluno.setText(carga.nome != null && !carga.nome.isEmpty()
                        ? "Olá, " + primeiroNome(carga.nome) + "!"
                        : "Olá, Aluno!");

                tvPontuacaoTotalAluno.setText(String.valueOf(carga.pontos));
                tvPosicaoRankingAluno.setText(carga.posicao != null ? carga.posicao.posicao + "º" : "—");

                int total = carga.aulasComRegistro;
                int percentual = total == 0 ? 100 : Math.round((total - carga.faltas) * 100f / total);

                tvFrequenciaPercentual.setText(percentual + "% | " + carga.faltas + " faltas");
                progressFrequencia.setProgress(percentual);

                listaProximas.clear();
                listaProximas.addAll(carga.proximas);
                adapter.notifyDataSetChanged();
                atualizarListaVazia();
            }

            @Override
            public void erro(ErroApi erro) {
                // Os números ficam como estão no layout
                atualizarListaVazia();
            }
        });
    }

    private void atualizarListaVazia() {

        tvSemAtividadesAluno.setVisibility(listaProximas.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerProximasAtividades.setVisibility(listaProximas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    public static class ProximaAtividade {

        final String id;
        final String nome;
        final String disciplinaNome;
        final String prazo;

        public ProximaAtividade(String id, String nome, String disciplinaNome, String prazo) {
            this.id = id;
            this.nome = nome;
            this.disciplinaNome = disciplinaNome != null ? disciplinaNome : "Disciplina";
            this.prazo = prazo;
        }
    }

    private class ProximaAtividadeAdapter
            extends RecyclerView.Adapter<ProximaAtividadeAdapter.ViewHolder> {

        private final List<ProximaAtividade> atividades;

        public ProximaAtividadeAdapter(List<ProximaAtividade> atividades) {
            this.atividades = atividades;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(
                    R.layout.item_proxima_atividade, parent, false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            ProximaAtividade atividade = atividades.get(position);

            holder.tvNomeProximaAtividade.setText(atividade.nome);
            holder.tvDisciplinaProximaAtividade.setText(atividade.disciplinaNome);
            holder.tvPrazoProximaAtividade.setText(atividade.prazo);

            // Toque abre a trilha
            holder.itemView.setOnClickListener(v -> {

                Intent intent = new Intent(TelaInicialAluno.this, TelaAtividadeAluno.class);
                intent.putExtra("atividadeId", atividade.id);
                intent.putExtra("atividadeNome", atividade.nome);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return atividades.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvNomeProximaAtividade;
            TextView tvDisciplinaProximaAtividade;
            TextView tvPrazoProximaAtividade;

            public ViewHolder(View itemView) {
                super(itemView);

                tvNomeProximaAtividade = itemView.findViewById(R.id.tvNomeProximaAtividade);
                tvDisciplinaProximaAtividade = itemView.findViewById(R.id.tvDisciplinaProximaAtividade);
                tvPrazoProximaAtividade = itemView.findViewById(R.id.tvPrazoProximaAtividade);
            }
        }
    }
}
