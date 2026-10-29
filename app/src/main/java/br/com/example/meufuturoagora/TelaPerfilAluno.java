package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Frequencia;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.PosicaoRanking;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;
import br.com.example.meufuturoagora.dados.Turma;

public class TelaPerfilAluno extends AppCompatActivity {

    private ImageView imgFotoPerfilAluno;
    private Perfil perfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_perfil_aluno);

        // Botão "Área do administrador" (só para contas admin)
        AdminUtil.configurarBotao(this);

        imgFotoPerfilAluno = findViewById(R.id.imgFotoPerfilAluno);

        // Galeria -> tela de recorte quadrado -> envio da foto recortada
        ActivityResultLauncher<Intent> recortarFoto = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> {
                    if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                        PerfilFotoUtil.trocarFoto(this, resultado.getData().getData(), perfil,
                                imgFotoPerfilAluno, atualizado -> perfil = atualizado);
                    }
                }
        );

        ActivityResultLauncher<String> escolherFoto = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        recortarFoto.launch(RecortarFotoActivity.criarIntent(this, uri));
                    }
                }
        );

        // Só troca a foto depois de saber de quem é o perfil
        imgFotoPerfilAluno.setOnClickListener(v -> {
            if (perfil != null) {
                escolherFoto.launch("image/*");
            }
        });

        carregarPerfil();

        // =========================
        // CONFIGURAÇÕES
        // =========================

        ImageView btnConfiguracoesAluno = findViewById(R.id.btnConfiguracoesAluno);

        btnConfiguracoesAluno.setOnClickListener(v ->
                startActivity(new Intent(this, ConfiguracoesActivity.class))
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

        bottomNavigation.setItemIconTintList(null);
        bottomNavigation.setSelectedItemId(R.id.nav_perfil);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                startActivity(new Intent(this, TelaInicialAluno.class));
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

                return true;
            }

            return false;
        });
    }

    // Tudo numa tarefa só: os números do perfil chegam juntos. A pontuação e a
    // posição já vêm calculadas pelo servidor (antes eram recalculadas aqui).
    private void carregarPerfil() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            Perfil meu = servicos.perfis.meu();

            if (meu == null) {
                throw new IllegalStateException("perfil não encontrado");
            }

            // O aluno só enxerga a própria turma
            String turma = null;
            for (Turma t : servicos.escola.turmas()) {
                if (t.id.equals(meu.turmaId)) {
                    turma = t.nome;
                }
            }

            int concluidas = 0;
            for (Entrega entrega : servicos.atividades.minhasEntregas()) {
                if (entrega.avaliada()) {
                    concluidas++;
                }
            }

            int faltas = 0;
            for (Frequencia frequencia : servicos.frequencias.frequencias()) {
                if (Frequencia.FALTA.equals(frequencia.status)) {
                    faltas++;
                }
            }

            return new Dados(meu, turma,
                    servicos.ranking.pontuacao(meu.id, null),
                    servicos.ranking.minhaPosicao(),
                    servicos.disciplinas.listar().size(),
                    concluidas, faltas);

        }, new Tarefas.Retorno<Dados>() {
            @Override
            public void sucesso(Dados dados) {
                mostrar(dados);
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(TelaPerfilAluno.this, "Erro ao carregar o perfil: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private void mostrar(Dados dados) {

        perfil = dados.perfil;

        ((TextView) findViewById(R.id.tvNomePerfilAluno)).setText(perfil.nome);
        ((TextView) findViewById(R.id.tvEmailPerfilAluno)).setText(perfil.email);
        ((TextView) findViewById(R.id.tvTurmaPerfilAluno)).setText(dados.turma != null ? dados.turma : "");
        ((TextView) findViewById(R.id.tvTurmaContaPerfilAluno)).setText(dados.turma != null ? dados.turma : "—");
        ((TextView) findViewById(R.id.tvPontuacaoTotalAluno)).setText(String.valueOf(dados.pontos));
        ((TextView) findViewById(R.id.tvPosicaoRankingAluno)).setText(
                dados.posicao != null ? dados.posicao.posicao + "º" : "—");
        ((TextView) findViewById(R.id.tvQtdDisciplinasPerfil)).setText(String.valueOf(dados.disciplinas));
        ((TextView) findViewById(R.id.tvQtdConcluidasPerfil)).setText(String.valueOf(dados.concluidas));
        ((TextView) findViewById(R.id.tvQtdFaltasPerfil)).setText(String.valueOf(dados.faltas));

        PerfilFotoUtil.mostrar(this, perfil, imgFotoPerfilAluno);
    }

    private static final class Dados {

        final Perfil perfil;
        final String turma;
        final int pontos;
        final PosicaoRanking posicao;
        final int disciplinas;
        final int concluidas;
        final int faltas;

        Dados(Perfil perfil, String turma, int pontos, PosicaoRanking posicao, int disciplinas,
              int concluidas, int faltas) {
            this.perfil = perfil;
            this.turma = turma;
            this.pontos = pontos;
            this.posicao = posicao;
            this.disciplinas = disciplinas;
            this.concluidas = concluidas;
            this.faltas = faltas;
        }
    }
}
