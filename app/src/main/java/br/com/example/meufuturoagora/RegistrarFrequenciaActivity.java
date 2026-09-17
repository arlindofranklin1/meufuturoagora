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

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class RegistrarFrequenciaActivity extends AppCompatActivity {

    private FirebaseFirestore db;

    private String disciplinaId;

    private RecyclerView recyclerAulas;
    private TextView tvSemAulas;

    private AulaAdapter adapter;
    private final List<Aula> listaAulas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_registrar_frequencia);

        db = FirebaseFirestore.getInstance();

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

        if (adapter != null) {
            carregarAulas();
        }
    }

    private void carregarAulas() {

        if (disciplinaId == null) {
            return;
        }

        db.collection("aulas")
                .whereEqualTo("disciplinaId", disciplinaId)
                .whereEqualTo("ativo", true)
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    listaAulas.clear();

                    for (QueryDocumentSnapshot documento : querySnapshot) {

                        String descricao = documento.getString("descricao");
                        String data = documento.getString("data");

                        listaAulas.add(new Aula(
                                documento.getId(),
                                descricao != null && !descricao.isEmpty() ? descricao : "Aula",
                                data != null ? data : ""
                        ));
                    }

                    adapter.notifyDataSetChanged();

                    tvSemAulas.setVisibility(listaAulas.isEmpty() ? View.VISIBLE : View.GONE);
                    recyclerAulas.setVisibility(listaAulas.isEmpty() ? View.GONE : View.VISIBLE);
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao carregar aulas.", Toast.LENGTH_SHORT
                ).show());
    }

    private void confirmarExclusao(String aulaId) {

        new AlertDialog.Builder(this)
                .setTitle("Excluir aula")
                .setMessage("Tem certeza que deseja excluir esta aula?")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Excluir", (dialog, which) -> excluirAula(aulaId))
                .show();
    }

    private void excluirAula(String aulaId) {

        db.collection("aulas")
                .document(aulaId)
                .delete()
                .addOnSuccessListener(unused -> {

                    Toast.makeText(this, "Aula excluída!", Toast.LENGTH_SHORT).show();
                    carregarAulas();
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this, "Erro ao excluir aula.", Toast.LENGTH_SHORT
                ).show());
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
