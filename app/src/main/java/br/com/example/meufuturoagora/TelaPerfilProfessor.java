package br.com.example.meufuturoagora;

import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TelaPerfilProfessor extends AppCompatActivity {

    private static final String PREFS_NOME = "preferencias_app";
    private static final String CHAVE_NOTIFICACOES = "notificacoes_ativas";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_tela_perfil_professor);

        // =========================
        // COMPONENTES
        // =========================

        ImageView imgFotoPerfil = findViewById(R.id.imgFotoPerfil);
        TextView tvNomePerfil = findViewById(R.id.tvNomePerfil);
        TextView tvEmailPerfil = findViewById(R.id.tvEmailPerfil);

        // =========================
        // USUÁRIO LOGADO
        // =========================

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario != null) {

            String nome = usuario.getDisplayName();
            tvNomePerfil.setText(nome != null && !nome.isEmpty() ? nome : "Professor(a)");

            String email = usuario.getEmail();
            tvEmailPerfil.setText(email != null ? email : "");

            Uri foto = usuario.getPhotoUrl();

            if (foto != null) {

                Glide.with(this)
                        .load(foto)
                        .placeholder(R.drawable.ic_perfil)
                        .error(R.drawable.ic_perfil)
                        .circleCrop()
                        .into(imgFotoPerfil);
            }
        }

        // =========================
        // PREFERÊNCIA DE NOTIFICAÇÕES
        // =========================

        SharedPreferences preferencias =
                getSharedPreferences(PREFS_NOME, MODE_PRIVATE);

        MaterialSwitch switchNotificacoes = findViewById(R.id.switchNotificacoes);

        switchNotificacoes.setChecked(
                preferencias.getBoolean(CHAVE_NOTIFICACOES, true)
        );

        switchNotificacoes.setOnCheckedChangeListener((buttonView, isChecked) ->
                preferencias.edit()
                        .putBoolean(CHAVE_NOTIFICACOES, isChecked)
                        .apply()
        );

        // =========================
        // SOBRE
        // =========================

        LinearLayout itemSobre = findViewById(R.id.itemSobre);

        itemSobre.setOnClickListener(v ->
                startActivity(new Intent(this, SobreActivity.class))
        );

        // =========================
        // SAIR DA CONTA
        // =========================

        LinearLayout itemSair = findViewById(R.id.itemSair);

        itemSair.setOnClickListener(v ->
                new AlertDialog.Builder(this)
                        .setTitle("Sair da conta")
                        .setMessage("Tem certeza que deseja sair?")
                        .setNegativeButton("Cancelar", null)
                        .setPositiveButton("Sair", (dialog, which) -> sairDaConta())
                        .show()
        );

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

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

            } else if (id == R.id.nav_perfil) {

                return true;
            }

            return false;
        });
    }

    private void sairDaConta() {

        FirebaseAuth.getInstance().signOut();

        Intent intent = new Intent(this, TelaEntrar.class);

        intent.setFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK
        );

        startActivity(intent);
        finish();
    }
}
