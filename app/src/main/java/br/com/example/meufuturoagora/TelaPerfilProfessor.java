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

import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class TelaPerfilProfessor extends AppCompatActivity {

    private ImageView imgFotoPerfil;
    private Perfil perfil;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_perfil_professor);

        imgFotoPerfil = findViewById(R.id.imgFotoPerfil);

        // Botão "Área do administrador" (só para contas admin)
        AdminUtil.configurarBotao(this);

        // Galeria -> tela de recorte quadrado -> envio da foto recortada
        ActivityResultLauncher<Intent> recortarFoto = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                resultado -> {
                    if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                        PerfilFotoUtil.trocarFoto(this, resultado.getData().getData(), perfil,
                                imgFotoPerfil, atualizado -> perfil = atualizado);
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

        imgFotoPerfil.setOnClickListener(v -> {
            if (perfil != null) {
                escolherFoto.launch("image/*");
            }
        });

        carregarPerfil();

        // =========================
        // CONFIGURAÇÕES
        // =========================

        ImageView btnConfiguracoesProfessor = findViewById(R.id.btnConfiguracoesProfessor);

        btnConfiguracoesProfessor.setOnClickListener(v ->
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

                startActivity(new Intent(this, TelaInicialProfessor.class));
                return true;

            } else if (id == R.id.nav_disciplinas) {

                startActivity(new Intent(this, TelaDisciplinaProfessor.class));
                return true;

            } else if (id == R.id.nav_ranking) {

                startActivity(new Intent(this, RankingActivity.class)
                        .putExtra(RankingActivity.EXTRA_PERFIL, RankingActivity.PERFIL_PROFESSOR));
                return true;

            } else if (id == R.id.nav_perfil) {

                return true;
            }

            return false;
        });
    }

    private void carregarPerfil() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            Perfil meu = servicos.perfis.meu();

            if (meu == null) {
                throw new IllegalStateException("perfil não encontrado");
            }

            // O admin enxerga todas as disciplinas: contam só as do próprio professor
            int disciplinas = 0;
            int trilhas = 0;
            int aulas = 0;

            for (Disciplina disciplina : servicos.disciplinas.listar()) {

                if (!meu.id.equals(disciplina.professorId) || !disciplina.ativo) {
                    continue;
                }

                disciplinas++;
                trilhas += servicos.atividades.daDisciplina(disciplina.id).size();
                aulas += servicos.frequencias.aulas(disciplina.id).size();
            }

            return new Dados(meu, disciplinas, trilhas, aulas);

        }, new Tarefas.Retorno<Dados>() {
            @Override
            public void sucesso(Dados dados) {

                perfil = dados.perfil;

                ((TextView) findViewById(R.id.tvNomePerfil)).setText(perfil.nome);
                ((TextView) findViewById(R.id.tvNomeContaPerfilProfessor)).setText(perfil.nome);
                ((TextView) findViewById(R.id.tvEmailPerfil)).setText(perfil.email);
                ((TextView) findViewById(R.id.tvEmailContaPerfilProfessor)).setText(perfil.email);
                ((TextView) findViewById(R.id.tvQtdDisciplinasPerfilProfessor)).setText(String.valueOf(dados.disciplinas));
                ((TextView) findViewById(R.id.tvQtdTrilhasEnviadasPerfilProfessor)).setText(String.valueOf(dados.trilhas));
                ((TextView) findViewById(R.id.tvQtdAulasRegistradasPerfilProfessor)).setText(String.valueOf(dados.aulas));

                PerfilFotoUtil.mostrar(TelaPerfilProfessor.this, perfil, imgFotoPerfil);
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(TelaPerfilProfessor.this, "Erro ao carregar o perfil: " + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    private static final class Dados {

        final Perfil perfil;
        final int disciplinas;
        final int trilhas;
        final int aulas;

        Dados(Perfil perfil, int disciplinas, int trilhas, int aulas) {
            this.perfil = perfil;
            this.disciplinas = disciplinas;
            this.trilhas = trilhas;
            this.aulas = aulas;
        }
    }
}
