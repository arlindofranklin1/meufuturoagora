package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.PosicaoRanking;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TelaInicialAluno extends AppCompatActivity {

    private FirebaseFirestore db;

    private String alunoId;

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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_inicial_aluno);

        db = FirebaseFirestore.getInstance();

        ImageView imgAluno = findViewById(R.id.imgAluno);
        TextView tvSaudacaoAluno = findViewById(R.id.tvSaudacaoAluno);

        tvPontuacaoTotalAluno = findViewById(R.id.tvPontuacaoTotalAluno);
        tvPosicaoRankingAluno = findViewById(R.id.tvPosicaoRankingAluno);
        tvFrequenciaPercentual = findViewById(R.id.tvFrequenciaPercentual);
        progressFrequencia = findViewById(R.id.progressFrequencia);

        recyclerProximasAtividades = findViewById(R.id.recyclerProximasAtividades);
        tvSemAtividadesAluno = findViewById(R.id.tvSemAtividadesAluno);

        recyclerProximasAtividades.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ProximaAtividadeAdapter(listaProximas);
        recyclerProximasAtividades.setAdapter(adapter);

        BimestreUtil.carregarPeriodoAtual(
                db,
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

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return;
        }

        String nome = usuario.getDisplayName();
        tvSaudacaoAluno.setText(nome != null && !nome.isEmpty() ? "Olá, " + primeiroNome(nome) + "!" : "Olá, Aluno!");

        // Tocar na foto abre o perfil (a foto é carregada no onResume)
        imgAluno.setOnClickListener(v ->
                startActivity(new Intent(this, TelaPerfilAluno.class))
        );

        alunoId = usuario.getUid();

        // Pontuação e posição vêm do servidor (módulo 7). O resto da tela segue
        // no Firestore até o módulo 9.
        carregarPontuacaoEPosicao();
        carregarProximasAtividades();
        carregarFrequencia();

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

    private static final class PontuacaoEPosicao {

        final int pontos;
        final PosicaoRanking posicao;

        PontuacaoEPosicao(int pontos, PosicaoRanking posicao) {
            this.pontos = pontos;
            this.posicao = posicao;
        }
    }

    private void carregarPontuacaoEPosicao() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> new PontuacaoEPosicao(
                        servicos.ranking.pontuacao(servicos.perfis.meu().id, null),
                        servicos.ranking.minhaPosicao()),
                new Tarefas.Retorno<PontuacaoEPosicao>() {
                    @Override
                    public void sucesso(PontuacaoEPosicao dados) {

                        tvPontuacaoTotalAluno.setText(String.valueOf(dados.pontos));
                        tvPosicaoRankingAluno.setText(
                                dados.posicao != null ? dados.posicao.posicao + "º" : "—");
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        // Os números ficam como estão no layout
                    }
                });
    }
    private void carregarFrequencia() {

        db.collection("frequencias")
                .whereEqualTo("alunoId", alunoId)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    int total = querySnapshot.size();
                    int faltas = 0;

                    for (QueryDocumentSnapshot documento : querySnapshot) {

                        if ("Falta".equals(documento.getString("status"))) {
                            faltas++;
                        }
                    }

                    int percentual = total == 0 ? 100 : Math.round((total - faltas) * 100f / total);

                    tvFrequenciaPercentual.setText(percentual + "% | " + faltas + " faltas");
                    progressFrequencia.setProgress(percentual);
                });
    }

    private void carregarProximasAtividades() {

        // Só disciplinas que ainda existem e estão ativas (com o nome atual)
        DisciplinaUtil.carregarDoAluno(db, alunoId, disciplinas -> {

                    List<String> disciplinaIds = new ArrayList<>();
                    List<String> disciplinaNomes = new ArrayList<>();

                    for (java.util.Map.Entry<String, String> item : disciplinas.entrySet()) {
                        disciplinaIds.add(item.getKey());
                        disciplinaNomes.add(item.getValue() != null ? item.getValue() : "Disciplina");
                    }

                    if (disciplinaIds.isEmpty()) {

                        atualizarListaVazia();
                        return;
                    }

                    // Firestore "whereIn" aceita no máximo 10 valores
                    List<String> idsConsulta = disciplinaIds.subList(
                            0, Math.min(10, disciplinaIds.size())
                    );

                    db.collection("atividades")
                            .whereIn("disciplinaId", idsConsulta)
                            .whereEqualTo("ativo", true)
                            .get()
                            .addOnSuccessListener(atividades -> {

                                listaProximas.clear();

                                for (QueryDocumentSnapshot documento : atividades) {

                                    String nome = documento.getString("nome");
                                    String prazo = documento.getString("prazo");
                                    String disciplinaId = documento.getString("disciplinaId");

                                    int indice = disciplinaIds.indexOf(disciplinaId);
                                    String disciplinaNome = indice >= 0
                                            ? disciplinaNomes.get(indice)
                                            : "Disciplina";

                                    if (nome != null) {

                                        ProximaAtividade proxima = new ProximaAtividade(
                                                nome,
                                                disciplinaNome,
                                                prazo != null ? prazo : ""
                                        );

                                        proxima.id = documento.getId();
                                        proxima.criadoEm = documento.getTimestamp("criadoEm");

                                        listaProximas.add(proxima);
                                    }
                                }

                                // Mostra só as 2 trilhas adicionadas mais recentemente
                                ordenarMaisRecentes(listaProximas);

                                if (listaProximas.size() > 2) {

                                    listaProximas.subList(2, listaProximas.size()).clear();
                                }

                                adapter.notifyDataSetChanged();
                                atualizarListaVazia();
                            });
                });
    }

    // Mais recentes primeiro (data de criação). Trilhas antigas, sem essa data,
    // ficam depois, ordenadas pelo prazo mais distante.
    private void ordenarMaisRecentes(List<ProximaAtividade> lista) {

        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        Collections.sort(lista, (a, b) -> {

            if (a.criadoEm != null && b.criadoEm != null) {
                return b.criadoEm.compareTo(a.criadoEm);
            }

            if (a.criadoEm != null) {
                return -1;
            }

            if (b.criadoEm != null) {
                return 1;
            }

            Date dataA = tentarConverter(formato, a.prazo);
            Date dataB = tentarConverter(formato, b.prazo);

            if (dataA == null || dataB == null) {
                return 0;
            }

            return dataB.compareTo(dataA);
        });
    }

    private void ordenarPorPrazo(List<ProximaAtividade> lista) {

        SimpleDateFormat formato = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

        Collections.sort(lista, (a, b) -> {

            Date dataA = tentarConverter(formato, a.prazo);
            Date dataB = tentarConverter(formato, b.prazo);

            if (dataA == null || dataB == null) {
                return 0;
            }

            return dataA.compareTo(dataB);
        });
    }

    private Date tentarConverter(SimpleDateFormat formato, String texto) {

        try {

            return formato.parse(texto);

        } catch (ParseException e) {

            return null;
        }
    }

    private void atualizarListaVazia() {

        tvSemAtividadesAluno.setVisibility(listaProximas.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerProximasAtividades.setVisibility(listaProximas.isEmpty() ? View.GONE : View.VISIBLE);
    }

    public static class ProximaAtividade {

        String id;
        String nome;
        String disciplinaNome;
        String prazo;
        com.google.firebase.Timestamp criadoEm;

        public ProximaAtividade(String nome, String disciplinaNome, String prazo) {
            this.nome = nome;
            this.disciplinaNome = disciplinaNome;
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
