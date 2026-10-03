package br.com.example.meufuturoagora;

import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;

// "Meu Futuro Agora": os três cursos do IFSertãoPB – Campus Santa Luzia
public class MeuFuturoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_meu_futuro);

        InsetsUtil.aplicarInsetsSistema(this);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        configurarCard(R.id.cardInformatica, CursosIfUtil.INFORMATICA);
        configurarCard(R.id.cardEnergia, CursosIfUtil.ENERGIA);
        configurarCard(R.id.cardEletronica, CursosIfUtil.ELETRONICA);
    }

    private void configurarCard(int idCard, String idCurso) {

        CursosIfUtil.Curso curso = CursosIfUtil.porId(idCurso);
        View card = findViewById(idCard);

        preencherCard(card, curso);

        card.setOnClickListener(v -> startActivity(
                new Intent(this, CursoDetalheActivity.class)
                        .putExtra(CursoDetalheActivity.EXTRA_CURSO, idCurso)
        ));
    }

    // Pinta o card (layout item_card_curso) com as cores e textos do curso
    static void preencherCard(View card, CursosIfUtil.Curso curso) {

        int forte = ContextCompat.getColor(card.getContext(), curso.corForte);
        int texto = ContextCompat.getColor(card.getContext(), curso.corTexto);
        int suave = ContextCompat.getColor(card.getContext(), curso.corSuave);
        int fundo = ContextCompat.getColor(card.getContext(), curso.corFundo);

        ((CardView) card).setCardBackgroundColor(fundo);

        ((ImageView) card.findViewById(R.id.imgCurvaCurso))
                .setImageTintList(ColorStateList.valueOf(suave));

        card.findViewById(R.id.circuloCurso)
                .setBackgroundTintList(ColorStateList.valueOf(suave));

        ImageView icone = card.findViewById(R.id.imgIconeCurso);
        icone.setImageResource(curso.icone);
        icone.setBackgroundTintList(ColorStateList.valueOf(forte));

        TextView nome = card.findViewById(R.id.tvNomeCurso);
        nome.setText(curso.nome);
        nome.setTextColor(texto);

        // Nome comprido (Energia Renovável) fica um pouco menor para caber em duas linhas
        if (curso.nome.length() > 16) {
            nome.setTextSize(18);
        }

        ((TextView) card.findViewById(R.id.tvResumoCurso)).setText(curso.resumo);
        ((TextView) card.findViewById(R.id.tvInfoCurso)).setText(curso.infoCard);

        card.setContentDescription(curso.nome + ". " + curso.resumo);
    }
}
