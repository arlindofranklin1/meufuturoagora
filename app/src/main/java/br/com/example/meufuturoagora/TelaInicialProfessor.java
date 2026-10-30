package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class TelaInicialProfessor extends AppCompatActivity {

    private BottomNavigationView bottomNavigation;
    private ImageView imgProfessor;

    private TextView tvNomeProfessor;
    private TextView tvNumeroDisciplinas;
    private CardView cardDisciplinas;
    private CardView btnCriarTarefa;

    // -1 enquanto ainda não carregou
    private int quantidadeDisciplinas = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_inicial_professor);

        imgProfessor = findViewById(R.id.imgProfessor);
        tvNomeProfessor = findViewById(R.id.tvNomeProfessor);
        tvNumeroDisciplinas = findViewById(R.id.tvNumeroDisciplinas);

        // Tocar na foto abre o perfil
        imgProfessor.setOnClickListener(v ->
                startActivity(new Intent(this, TelaPerfilProfessor.class))
        );

        // =========================
        // BIMESTRE ATUAL
        // =========================

        PeriodoAtual.mostrar(
                this,
                findViewById(R.id.tvTituloBimestre),
                findViewById(R.id.tvPeriodoBimestre)
        );

        // Notificações de novidades (respeita o switch das configurações)
        NotificacaoUtil.iniciar(this, NotificacaoUtil.PERFIL_PROFESSOR);

        // Sino: abre a lista de notificações; bolinha laranja = tem notificação não vista
        View bolinhaNotificacao = findViewById(R.id.bolinhaNotificacao);
        NotificacaoHistorico.vincularBolinha(this, bolinhaNotificacao);
        findViewById(R.id.btnNotificacoes).setOnClickListener(v ->
                NotificacoesDialog.mostrar(this, bolinhaNotificacao)
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        bottomNavigation = findViewById(R.id.bottomNavigation);

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

        bottomNavigation.setItemIconTintList(null);

        bottomNavigation.setSelectedItemId(R.id.nav_inicio);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                return true;

            } else if (id == R.id.nav_disciplinas) {

                startActivity(new Intent(TelaInicialProfessor.this, TelaDisciplinaProfessor.class));
                return true;

            } else if (id == R.id.nav_ranking) {

                startActivity(new Intent(TelaInicialProfessor.this, RankingActivity.class)
                        .putExtra(RankingActivity.EXTRA_PERFIL, RankingActivity.PERFIL_PROFESSOR));
                return true;

            } else if (id == R.id.nav_perfil) {

                startActivity(new Intent(TelaInicialProfessor.this, TelaPerfilProfessor.class));
                return true;
            }

            return false;
        });

        cardDisciplinas = findViewById(R.id.cardDisciplinas);

        cardDisciplinas.setOnClickListener(v ->
                startActivity(new Intent(TelaInicialProfessor.this, TelaDisciplinaProfessor.class))
        );

        // =========================
        // AÇÕES RÁPIDAS
        // =========================

        btnCriarTarefa = findViewById(R.id.btnCriarTarefa);

        // Toda trilha pertence a uma disciplina: sem disciplina criada, só avisa
        btnCriarTarefa.setOnClickListener(v -> {

            if (quantidadeDisciplinas == 0) {

                Toast.makeText(
                        this,
                        "Crie uma disciplina primeiro para depois criar uma trilha.",
                        Toast.LENGTH_LONG
                ).show();
                return;
            }

            startActivity(new Intent(TelaInicialProfessor.this, CriarAtividade.class));
        });

        CardView btnVerRanking = findViewById(R.id.btnVerRanking);

        btnVerRanking.setOnClickListener(v -> startActivity(
                new Intent(TelaInicialProfessor.this, RankingActivity.class)
                        .putExtra(
                                RankingActivity.EXTRA_PERFIL,
                                RankingActivity.PERFIL_PROFESSOR
                        )
        ));

        CardView btnDesempenho = findViewById(R.id.btnDesempenho);

        btnDesempenho.setOnClickListener(v -> startActivity(
                new Intent(TelaInicialProfessor.this, DesempenhoActivity.class)
                        .putExtra(
                                DesempenhoActivity.EXTRA_PERFIL,
                                DesempenhoActivity.PERFIL_PROFESSOR
                        )
        ));
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Foto (a escolhida no perfil ou a do Google), nome e disciplinas, atualizados ao voltar
        carregarNomeEDisciplinas();
    }

    // Sem disciplina, o botão fica apagado (o toque só mostra o aviso)
    private void btnCriarTarefaHabilitado(boolean habilitado) {
        btnCriarTarefa.setAlpha(habilitado ? 1f : 0.5f);
    }

    private static final class Carga {

        final Perfil professor;
        final int disciplinas;

        Carga(Perfil professor, int disciplinas) {
            this.professor = professor;
            this.disciplinas = disciplinas;
        }
    }

    // =====================================================
    // NOME, FOTO E QUANTIDADE DE DISCIPLINAS DO PROFESSOR
    // =====================================================

    private void carregarNomeEDisciplinas() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            Perfil eu = servicos.perfis.meu();
            int quantidade = 0;

            // O RLS devolve as disciplinas do professor (o admin vê todas)
            for (Disciplina d : servicos.disciplinas.listar()) {

                if (d.ativo && eu.id.equals(d.professorId)) {
                    quantidade++;
                }
            }

            return new Carga(eu, quantidade);

        }, new Tarefas.Retorno<Carga>() {
            @Override
            public void sucesso(Carga carga) {

                String nome = carga.professor.nome;
                tvNomeProfessor.setText(nome != null && !nome.isEmpty() ? nome : "Professor");

                PerfilFotoUtil.mostrar(TelaInicialProfessor.this, carga.professor, imgProfessor);

                quantidadeDisciplinas = carga.disciplinas;
                btnCriarTarefaHabilitado(carga.disciplinas > 0);

                tvNumeroDisciplinas.setText(String.valueOf(carga.disciplinas));
            }

            @Override
            public void erro(ErroApi erro) {
                tvNumeroDisciplinas.setText("ERRO");
            }
        });
    }
}
