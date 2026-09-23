package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class TelaPerfilProfessor extends AppCompatActivity {

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
                        .override(200, 200)
                        .circleCrop()
                        .into(imgFotoPerfil);
            }
        }

        // =========================
        // CONFIGURAÇÕES
        // =========================

        LinearLayout itemConfiguracoes = findViewById(R.id.itemConfiguracoes);

        itemConfiguracoes.setOnClickListener(v ->
                startActivity(new Intent(this, ConfiguracoesActivity.class))
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
}
