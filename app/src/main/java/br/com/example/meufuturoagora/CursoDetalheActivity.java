package br.com.example.meufuturoagora;

import android.content.res.ColorStateList;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

// Detalhes de um curso: sobre, descrição, onde trabalhar e disciplinas por ano
public class CursoDetalheActivity extends AppCompatActivity {

    static final String EXTRA_CURSO = "curso";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_curso_detalhe);

        InsetsUtil.aplicarInsetsSistema(this);

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        CursosIfUtil.Curso curso = CursosIfUtil.porId(getIntent().getStringExtra(EXTRA_CURSO));

        if (curso == null) {
            finish();
            return;
        }

        int forte = ContextCompat.getColor(this, curso.corForte);
        int texto = ContextCompat.getColor(this, curso.corTexto);
        int suave = ContextCompat.getColor(this, curso.corSuave);

        LayoutInflater inflater = LayoutInflater.from(this);

        // =========================
        // TOPO E CARD DESTAQUE
        // =========================

        TextView tvTituloTela = findViewById(R.id.tvTituloTela);
        tvTituloTela.setText(curso.nome);
        tvTituloTela.setTextColor(texto);

        View cardDestaque = findViewById(R.id.cardDestaque);
        MeuFuturoActivity.preencherCard(cardDestaque, curso);
        ((TextView) cardDestaque.findViewById(R.id.tvNomeCurso)).setText(curso.nomeCompleto);
        ((TextView) cardDestaque.findViewById(R.id.tvInfoCurso))
                .setText("IFSertãoPB – Campus Santa Luzia");
        cardDestaque.setClickable(false);
        cardDestaque.setForeground(null);

        int[] secoes = {
                R.id.tvSecaoSobre, R.id.tvSecaoDescricao,
                R.id.tvSecaoTrabalho, R.id.tvSecaoDisciplinas
        };

        for (int id : secoes) {
            ((TextView) findViewById(id)).setTextColor(texto);
        }

        // =========================
        // SOBRE O CURSO
        // =========================

        LinearLayout containerInfos = findViewById(R.id.containerInfos);

        for (int i = 0; i < curso.infos.length; i++) {

            View linha = inflater.inflate(R.layout.item_info_curso, containerInfos, false);

            ((TextView) linha.findViewById(R.id.tvRotuloInfo)).setText(curso.infos[i][0]);
            ((TextView) linha.findViewById(R.id.tvValorInfo)).setText(curso.infos[i][1]);

            containerInfos.addView(linha);

            if (i < curso.infos.length - 1) {
                View divisor = new View(this);
                divisor.setBackgroundColor(0xFFEFEFEF);
                containerInfos.addView(divisor, new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dp(1)
                ));
            }
        }

        // =========================
        // DESCRIÇÃO
        // =========================

        ((TextView) findViewById(R.id.tvDescricaoCurso)).setText(curso.descricao);

        // =========================
        // ONDE PODE TRABALHAR
        // =========================

        ((TextView) findViewById(R.id.tvIntroTrabalho)).setText(curso.introTrabalho);

        LinearLayout containerTrabalho = findViewById(R.id.containerTrabalho);

        for (String item : curso.trabalho) {

            View linha = inflater.inflate(R.layout.item_trabalho_curso, containerTrabalho, false);

            ((ImageView) linha.findViewById(R.id.imgCheckTrabalho))
                    .setImageTintList(ColorStateList.valueOf(forte));
            ((TextView) linha.findViewById(R.id.tvTextoTrabalho)).setText(item);

            containerTrabalho.addView(linha);
        }

        // =========================
        // DISCIPLINAS POR ANO
        // =========================

        if (curso.obsDisciplinas != null) {
            TextView tvObs = findViewById(R.id.tvObsDisciplinas);
            tvObs.setText(curso.obsDisciplinas);
            tvObs.setVisibility(View.VISIBLE);
        }

        LinearLayout containerDisciplinas = findViewById(R.id.containerDisciplinas);

        for (CursosIfUtil.Ano ano : curso.anos) {

            View cabecalho = inflater.inflate(R.layout.item_ano_curso, containerDisciplinas, false);

            TextView tvAno = cabecalho.findViewById(R.id.tvAnoCurso);
            tvAno.setText(ano.titulo);
            tvAno.setBackgroundTintList(ColorStateList.valueOf(forte));

            int qtd = ano.disciplinas.size();
            ((TextView) cabecalho.findViewById(R.id.tvQtdDisciplinasAno))
                    .setText(qtd + (qtd == 1 ? " disciplina" : " disciplinas"));

            containerDisciplinas.addView(cabecalho);

            for (CursosIfUtil.Disciplina disciplina : ano.disciplinas) {

                View item = inflater.inflate(R.layout.item_disciplina_curso, containerDisciplinas, false);

                item.findViewById(R.id.faixaDisciplina).setBackgroundColor(forte);

                ((TextView) item.findViewById(R.id.tvNomeDisciplinaCurso)).setText(disciplina.nome);
                ((TextView) item.findViewById(R.id.tvDescricaoDisciplinaCurso))
                        .setText(disciplina.descricao);

                TextView tvCarga = item.findViewById(R.id.tvCargaDisciplinaCurso);
                tvCarga.setText(disciplina.carga);
                tvCarga.setTextColor(texto);
                tvCarga.setBackgroundTintList(ColorStateList.valueOf(suave));

                containerDisciplinas.addView(item);
            }
        }
    }

    private int dp(int valor) {
        return Math.round(valor * getResources().getDisplayMetrics().density);
    }
}
