package br.com.example.meufuturoagora;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SobreActivity extends AppCompatActivity {

    // Perfis do LinkedIn (cole aqui o endereço completo de cada perfil)
    private static final String LINKEDIN_ARLINDO = "https://www.linkedin.com/in/arlindo-franklim-62b6a3409?utm_source=share_via&utm_content=profile&utm_medium=member_android";
    private static final String LINKEDIN_MARIA = "";
    private static final String LINKEDIN_ALEXANDRE = "https://www.linkedin.com/in/alexandrecostapb?utm_source=share_via&utm_content=profile&utm_medium=member_android";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_sobre);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        ImageView btnVoltar = findViewById(R.id.btnVoltar);

        btnVoltar.setOnClickListener(v -> finish());

        findViewById(R.id.btnLinkedinArlindo).setOnClickListener(v -> abrirLink(LINKEDIN_ARLINDO));
        findViewById(R.id.btnLinkedinMaria).setOnClickListener(v -> abrirLink(LINKEDIN_MARIA));
        findViewById(R.id.btnLinkedinAlexandre).setOnClickListener(v -> abrirLink(LINKEDIN_ALEXANDRE));
    }

    private void abrirLink(String url) {

        if (url == null || url.isEmpty()) {

            Toast.makeText(this, "LinkedIn ainda não cadastrado.", Toast.LENGTH_SHORT).show();
            return;
        }

        startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
    }
}
