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
import java.util.List;

import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Lista as aulas da disciplina (mais recentes primeiro) para abrir a frequência
// de cada uma ou excluir a aula.
public class RegistrarFrequenciaActivity extends AppCompatActivity {

    private String disciplinaId;

    private RecyclerView recyclerAulas;
    private TextView tvSemAulas;

    private AulaAdapter adapter;
    private final List<Aula> listaAulas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registrar_frequencia);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        disciplinaId = getIntent().getStringExtra("disciplinaId");

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        recyclerAulas = findViewById(R.id.recyclerAulas);
        tvSemAulas = findViewById(R.id.tvSemAulas);

        recyclerAulas.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AulaAdapter(listaAulas);
        recyclerAulas.setAdapter(adapter);

        carregarAulas();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Volta da tela de cadastro de aula: recarrega
        if (adapter != null) {
            carregarAulas();
        }
    }

    private void carregarAulas() {

        if (disciplinaId == null) {
            return;
        }

        Tarefas.executar(this, () -> Servicos.get(this).frequencias.aulas(disciplinaId),
                new Tarefas.Retorno<List<br.com.example.meufuturoagora.dados.Aula>>() {
                    @Override
                    public void sucesso(List<br.com.example.meufuturoagora.dados.Aula> aulas) {

                        listaAulas.clear();

                        for (br.com.example.meufuturoagora.dados.Aula aula : aulas) {

                            listaAulas.add(new Aula(
                                    aula.id,
                                    aula.descricao != null && !aula.descricao.isEmpty() ? aula.descricao : "Aula",
                                    Datas.formatar(aula.data)
                            ));
                        }

                        adapter.notifyDataSetChanged();

                        tvSemAulas.setVisibility(listaAulas.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerAulas.setVisibility(listaAulas.isEmpty() ? View.GONE : View.VISIBLE);
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Toast.makeText(RegistrarFrequenciaActivity.this, "Erro ao carregar aulas: " + erro.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // A frequência da aula também é apagada, e com ela o desconto de pontos por falta daquele dia
    private void confirmarExclusao(String aulaId) {

        new AlertDialog.Builder(this)
                .setTitle("Excluir aula")
                .setMessage("Tem certeza que deseja excluir esta aula? A frequência registrada nela também "
                        + "será apagada, e os alunos que faltaram nesse dia recuperam os pontos descontados.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> excluirAula(aulaId))
                .show();
    }

    private void excluirAula(String aulaId) {

        Tarefas.executar(this, () -> {
            Servicos.get(this).frequencias.excluirAula(aulaId);
            return null;
        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {

                Toast.makeText(RegistrarFrequenciaActivity.this, "Aula excluída!", Toast.LENGTH_SHORT).show();
                carregarAulas();
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(RegistrarFrequenciaActivity.this, "Erro ao excluir aula: " + erro.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static class Aula {

        String id;
        String descricao;
        String data;

        public Aula(String id, String descricao, String data) {
            this.id = id;
            this.descricao = descricao;
            this.data = data;
        }
    }

    private class AulaAdapter extends RecyclerView.Adapter<AulaAdapter.ViewHolder> {

        private final List<Aula> aulas;

        public AulaAdapter(List<Aula> aulas) {
            this.aulas = aulas;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(R.layout.item_aula, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            Aula aula = aulas.get(position);

            holder.tvDescricaoAula.setText(aula.descricao);
            holder.tvDataAula.setText(aula.data);

            holder.itemView.setOnClickListener(v -> {

                Intent intent = new Intent(
                        RegistrarFrequenciaActivity.this,
                        FrequenciaAulaActivity.class
                );

                intent.putExtra("aulaId", aula.id);
                intent.putExtra("aulaDescricao", aula.descricao);
                intent.putExtra("disciplinaId", disciplinaId);

                startActivity(intent);
            });

            holder.btnMenuAula.setOnClickListener(v -> {

                PopupMenu popupMenu = new PopupMenu(
                        RegistrarFrequenciaActivity.this, holder.btnMenuAula
                );

                popupMenu.getMenu().add("Excluir");

                popupMenu.setOnMenuItemClickListener(item -> {

                    if ("Excluir".equals(item.getTitle().toString())) {

                        confirmarExclusao(aula.id);
                        return true;
                    }

                    return false;
                });

                popupMenu.show();
            });
        }

        @Override
        public int getItemCount() {
            return aulas.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            TextView tvDescricaoAula;
            TextView tvDataAula;
            ImageView btnMenuAula;

            public ViewHolder(View itemView) {
                super(itemView);

                tvDescricaoAula = itemView.findViewById(R.id.tvDescricaoAula);
                tvDataAula = itemView.findViewById(R.id.tvDataAula);
                btnMenuAula = itemView.findViewById(R.id.btnMenuAula);
            }
        }
    }
}
