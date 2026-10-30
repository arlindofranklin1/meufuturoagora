package br.com.example.meufuturoagora;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class TelaDisciplinaAluno extends AppCompatActivity {


    private RecyclerView recyclerDisciplinas;
    private TextView tvSemDisciplinasAluno;

    private DisciplinaAlunoAdapter adapter;

    private final List<Disciplina> listaDisciplinas = new ArrayList<>();
    private final List<Disciplina> listaDisciplinasOriginal = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_disciplina_aluno);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsTopo(this);


        recyclerDisciplinas = findViewById(R.id.recyclerDisciplinas);
        tvSemDisciplinasAluno = findViewById(R.id.tvSemDisciplinasAluno);

        recyclerDisciplinas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DisciplinaAlunoAdapter(listaDisciplinas);
        recyclerDisciplinas.setAdapter(adapter);

        EditText edtBuscar = findViewById(R.id.edtBuscar);

        edtBuscar.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filtrarDisciplinas(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        carregarDisciplinas();

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

        bottomNavigation.setItemIconTintList(null);
        bottomNavigation.setSelectedItemId(R.id.nav_disciplinas);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                startActivity(new Intent(this, TelaInicialAluno.class));
                return true;

            } else if (id == R.id.nav_disciplinas) {

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

    private void carregarDisciplinas() {

        // O RLS devolve ao aluno só as disciplinas em que está matriculado; só as ativas
        Tarefas.executar(this, () -> Servicos.get(this).disciplinas.listar(),
                new Tarefas.Retorno<List<br.com.example.meufuturoagora.dados.Disciplina>>() {
                    @Override
                    public void sucesso(List<br.com.example.meufuturoagora.dados.Disciplina> disciplinas) {

                        listaDisciplinas.clear();
                        listaDisciplinasOriginal.clear();

                        for (br.com.example.meufuturoagora.dados.Disciplina d : disciplinas) {

                            if (!d.ativo) {
                                continue;
                            }

                            Disciplina disciplina = new Disciplina(
                                    d.id,
                                    d.nome != null ? d.nome : "Disciplina",
                                    gerarCorDisciplina(d.id)
                            );

                            listaDisciplinas.add(disciplina);
                            listaDisciplinasOriginal.add(disciplina);
                        }

                        adapter.notifyDataSetChanged();
                        atualizarVazio();
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Toast.makeText(TelaDisciplinaAluno.this,
                                "Erro ao carregar disciplinas.", Toast.LENGTH_SHORT).show();
                        atualizarVazio();
                    }
                });
    }

    private void atualizarVazio() {

        tvSemDisciplinasAluno.setVisibility(
                listaDisciplinas.isEmpty() ? View.VISIBLE : View.GONE
        );

        recyclerDisciplinas.setVisibility(
                listaDisciplinas.isEmpty() ? View.GONE : View.VISIBLE
        );
    }

    private void filtrarDisciplinas(String texto) {

        String busca = texto.trim().toLowerCase();

        listaDisciplinas.clear();

        if (busca.isEmpty()) {

            listaDisciplinas.addAll(listaDisciplinasOriginal);

        } else {

            for (Disciplina disciplina : listaDisciplinasOriginal) {

                if (disciplina.nome.toLowerCase().contains(busca)) {
                    listaDisciplinas.add(disciplina);
                }
            }
        }

        adapter.notifyDataSetChanged();
    }

    private String gerarCorDisciplina(String id) {

        String[] cores = {
                "#521BB3", "#E45832", "#22BEEA", "#8E44AD",
                "#27AE60", "#F39C12", "#E84393", "#16A085"
        };

        int indice = Math.abs(id.hashCode()) % cores.length;
        return cores[indice];
    }

    public static class Disciplina {

        String id;
        String nome;
        String cor;

        public Disciplina(String id, String nome, String cor) {
            this.id = id;
            this.nome = nome;
            this.cor = cor;
        }
    }

    private class DisciplinaAlunoAdapter
            extends RecyclerView.Adapter<DisciplinaAlunoAdapter.ViewHolder> {

        private final List<Disciplina> disciplinas;

        public DisciplinaAlunoAdapter(List<Disciplina> disciplinas) {
            this.disciplinas = disciplinas;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(
                    R.layout.item_disciplina_aluno, parent, false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            Disciplina disciplina = disciplinas.get(position);

            holder.tvNomeDisciplina.setText(disciplina.nome);

            int cor = Color.parseColor(disciplina.cor);

            GradientDrawable fundoCirculo = new GradientDrawable();
            fundoCirculo.setShape(GradientDrawable.OVAL);
            fundoCirculo.setColor(cor);

            holder.circuloCor.setBackground(fundoCirculo);

            holder.itemView.setOnClickListener(v -> {

                Intent intent = new Intent(
                        TelaDisciplinaAluno.this,
                        TelaDetalhesDisciplinaAluno.class
                );

                intent.putExtra("disciplinaId", disciplina.id);
                intent.putExtra("disciplinaNome", disciplina.nome);

                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return disciplinas.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            View circuloCor;
            TextView tvNomeDisciplina;

            public ViewHolder(View itemView) {
                super(itemView);

                circuloCor = itemView.findViewById(R.id.circuloCor);
                tvNomeDisciplina = itemView.findViewById(R.id.tvNomeDisciplina);
            }
        }
    }
}
