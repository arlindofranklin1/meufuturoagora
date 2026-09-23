package br.com.example.meufuturoagora;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.WriteBatch;

public class TelaAdminHome extends AppCompatActivity {

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_admin_home);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.itemAnoLetivo).setOnClickListener(v ->
                startActivity(new Intent(this, DefinirAnoLetivoActivity.class))
        );

        findViewById(R.id.itemBimestres).setOnClickListener(v ->
                startActivity(new Intent(this, DefinirBimestresActivity.class))
        );

        findViewById(R.id.itemCadastrarUsuario).setOnClickListener(v ->
                startActivity(new Intent(this, CadastrarUsuarioActivity.class))
        );

        findViewById(R.id.itemListaUsuarios).setOnClickListener(v ->
                startActivity(new Intent(this, ListaUsuariosActivity.class))
        );

        findViewById(R.id.itemEncerrarBimestre).setOnClickListener(v -> confirmarEncerramento());

        findViewById(R.id.itemSairAdmin).setOnClickListener(v -> {

            Intent intent = new Intent(this, TelaEntrar.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });
    }

    private void confirmarEncerramento() {

        new AlertDialog.Builder(this)
                .setTitle("Encerrar bimestre atual")
                .setMessage(
                        "Isso vai zerar a pontuação e as faltas de todos os alunos. " +
                                "Essa ação não pode ser desfeita. Deseja continuar?"
                )
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Encerrar", (dialog, which) -> encerrarBimestre())
                .show();
    }

    private void encerrarBimestre() {

        Toast.makeText(this, "Encerrando bimestre...", Toast.LENGTH_SHORT).show();

        db.collection("alunos")
                .get()
                .addOnSuccessListener(alunos -> {

                    db.collection("frequencias")
                            .get()
                            .addOnSuccessListener(frequencias -> {

                                WriteBatch batch = db.batch();

                                for (QueryDocumentSnapshot aluno : alunos) {

                                    DocumentReference referencia = aluno.getReference();
                                    batch.update(referencia, "pontuacao", 0);
                                }

                                for (QueryDocumentSnapshot frequencia : frequencias) {
                                    batch.delete(frequencia.getReference());
                                }

                                batch.commit()
                                        .addOnSuccessListener(unused -> Toast.makeText(
                                                this,
                                                "Bimestre encerrado! Pontuação e faltas reiniciadas.",
                                                Toast.LENGTH_LONG
                                        ).show())
                                        .addOnFailureListener(e -> Toast.makeText(
                                                this,
                                                "Erro ao encerrar bimestre: " + e.getMessage(),
                                                Toast.LENGTH_LONG
                                        ).show());
                            });
                })
                .addOnFailureListener(e -> Toast.makeText(
                        this,
                        "Erro ao encerrar bimestre: " + e.getMessage(),
                        Toast.LENGTH_LONG
                ).show());
    }
}
