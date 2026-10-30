package br.com.example.meufuturoagora;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.Aula;
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.Desempenho;
import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.Entrega;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Frequencia;
import br.com.example.meufuturoagora.dados.Participante;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

/**
 * Desempenho por bimestre.
 *
 * Professor: pontos e faltas de cada aluno matriculado nas suas disciplinas.
 * Aluno: seus pontos, faltas e faltas justificadas, e os pontos por disciplina.
 *
 * Os pontos vêm das entregas avaliadas (nota) e entram no bimestre do prazo
 * da atividade. As faltas vêm das frequências e entram no bimestre da data
 * da aula. Os períodos são os definidos pelo administrador. A conta é da
 * classe Desempenho; o RLS já limita o que cada perfil lê.
 */
public class DesempenhoActivity extends AppCompatActivity {

    public static final String EXTRA_PERFIL = "perfil";
    public static final String PERFIL_PROFESSOR = "professor";
    public static final String PERFIL_ALUNO = "aluno";

    private static final String[] CORES = {
            "#521BB3", "#E45832", "#22BEEA", "#8E44AD",
            "#27AE60", "#F39C12", "#E84393", "#16A085"
    };

    private boolean modoProfessor;

    // Índices 1 a 4
    private final String[] periodosTexto = new String[5];

    private int bimestreSelecionado = 1;
    private Dados dados;

    private final TextView[] abas = new TextView[5];

    private TextView tvTituloBimestre;
    private TextView tvPeriodoBimestre;
    private TextView tvPontosBimestre;
    private TextView tvFaltasBimestre;
    private TextView tvJustificadasBimestre;
    private TextView tvSemDados;
    private View progressDesempenho;
    private RecyclerView recyclerDesempenho;

    private DesempenhoAdapter adapter;
    private final List<Linha> linhas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_desempenho);
        InsetsUtil.aplicarInsetsSistema(this);

        modoProfessor = PERFIL_PROFESSOR.equals(
                getIntent().getStringExtra(EXTRA_PERFIL)
        );

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        tvTituloBimestre = findViewById(R.id.tvTituloBimestre);
        tvPeriodoBimestre = findViewById(R.id.tvPeriodoBimestreDesempenho);
        tvPontosBimestre = findViewById(R.id.tvPontosBimestre);
        tvFaltasBimestre = findViewById(R.id.tvFaltasBimestre);
        tvJustificadasBimestre = findViewById(R.id.tvJustificadasBimestre);
        tvSemDados = findViewById(R.id.tvSemDadosDesempenho);
        progressDesempenho = findViewById(R.id.progressDesempenho);
        recyclerDesempenho = findViewById(R.id.recyclerDesempenho);

        ((TextView) findViewById(R.id.tvResumoDesempenho)).setText(
                modoProfessor
                        ? "Pontos e faltas de cada aluno"
                        : "Seus pontos e faltas neste bimestre"
        );

        findViewById(R.id.layoutCabecalhoProfessor).setVisibility(
                modoProfessor ? View.VISIBLE : View.GONE
        );

        findViewById(R.id.layoutResumoAluno).setVisibility(
                modoProfessor ? View.GONE : View.VISIBLE
        );

        aplicarFundos();

        abas[1] = findViewById(R.id.abaBimestre1);
        abas[2] = findViewById(R.id.abaBimestre2);
        abas[3] = findViewById(R.id.abaBimestre3);
        abas[4] = findViewById(R.id.abaBimestre4);

        for (int i = 1; i <= 4; i++) {

            int bimestre = i;

            abas[i].setOnClickListener(v -> {
                bimestreSelecionado = bimestre;
                mostrarBimestre();
            });
        }

        recyclerDesempenho.setLayoutManager(new LinearLayoutManager(this));
        adapter = new DesempenhoAdapter();
        recyclerDesempenho.setAdapter(adapter);

        mostrarBimestre();
        carregarDados();
    }

    // =====================================================
    // CARREGAMENTO
    // =====================================================

    // Tudo o que a tela mostra, já lido do servidor
    private static final class Dados {

        final Perfil eu;
        final List<Bimestre> bimestres;
        final Desempenho desempenho;
        // Professor: os alunos das disciplinas dele, por nome
        final List<Participante> alunos;
        // Aluno: as disciplinas ativas em que está matriculado
        final Map<String, String> disciplinas;

        Dados(Perfil eu, List<Bimestre> bimestres, Desempenho desempenho, List<Participante> alunos,
              Map<String, String> disciplinas) {
            this.eu = eu;
            this.bimestres = bimestres;
            this.desempenho = desempenho;
            this.alunos = alunos;
            this.disciplinas = disciplinas;
        }
    }

    private void carregarDados() {

        Servicos servicos = Servicos.get(this);
        boolean professor = modoProfessor;

        Tarefas.executar(this, () -> {

            Perfil eu = servicos.perfis.meu();
            AnoLetivo ano = servicos.escola.anoAtual();
            List<Bimestre> bimestres = ano != null
                    ? servicos.escola.bimestres(ano.ano)
                    : new ArrayList<>();

            // As disciplinas ativas que a tela cobre: as que o professor dá ou
            // em que o aluno está matriculado (o RLS já filtra por perfil)
            Map<String, String> disciplinas = new LinkedHashMap<>();

            for (Disciplina d : servicos.disciplinas.listar()) {

                if (d.ativo && (!professor || eu.id.equals(d.professorId))) {
                    disciplinas.put(d.id, d.nome != null ? d.nome : "Disciplina");
                }
            }

            List<Atividade> atividades = new ArrayList<>();
            List<Aula> aulas = new ArrayList<>();
            List<Entrega> entregas = new ArrayList<>();
            Map<String, Participante> alunos = new LinkedHashMap<>();

            for (String disciplinaId : disciplinas.keySet()) {

                for (Atividade a : servicos.atividades.daDisciplina(disciplinaId)) {

                    atividades.add(a);

                    if (professor) {
                        entregas.addAll(servicos.atividades.entregasDaAtividade(a.id));
                    }
                }

                aulas.addAll(servicos.frequencias.aulas(disciplinaId));

                if (professor) {

                    for (Participante p : servicos.disciplinas.participantes(disciplinaId)) {

                        if ("aluno".equals(p.tipo) && !alunos.containsKey(p.perfilId)) {
                            alunos.put(p.perfilId, p);
                        }
                    }
                }
            }

            if (!professor) {
                entregas.addAll(servicos.atividades.minhasEntregas());
            }

            List<Participante> listaAlunos = new ArrayList<>(alunos.values());
            Collections.sort(listaAlunos, (a, b) -> a.nome.compareToIgnoreCase(b.nome));

            Desempenho desempenho = new Desempenho(bimestres, atividades, entregas, aulas,
                    servicos.frequencias.frequencias());

            return new Dados(eu, bimestres, desempenho, listaAlunos, disciplinas);

        }, new Tarefas.Retorno<Dados>() {
            @Override
            public void sucesso(Dados carregados) {

                dados = carregados;
                lerPeriodos(carregados.bimestres, carregados.desempenho);
                mostrarBimestre();
            }

            @Override
            public void erro(ErroApi erro) {

                progressDesempenho.setVisibility(View.GONE);

                Toast.makeText(
                        DesempenhoActivity.this,
                        "Erro ao carregar desempenho.",
                        Toast.LENGTH_SHORT
                ).show();
            }
        });
    }

    // Texto do período de cada aba e seleção do bimestre em curso
    private void lerPeriodos(List<Bimestre> bimestres, Desempenho desempenho) {

        Set<Integer> definidos = new HashSet<>();

        for (Bimestre b : bimestres) {

            if (b.numero >= 1 && b.numero <= 4 && b.inicio != null && b.fim != null) {
                definidos.add(b.numero);
                periodosTexto[b.numero] = Datas.formatar(b.inicio) + " - " + Datas.formatar(b.fim);
            }
        }

        int atual = desempenho.bimestreDa(LocalDate.now());

        if (definidos.contains(atual)) {
            bimestreSelecionado = atual;
        }
    }

    // =====================================================
    // EXIBIÇÃO
    // =====================================================

    private void mostrarBimestre() {

        int n = bimestreSelecionado;

        for (int i = 1; i <= 4; i++) {

            boolean selecionada = i == n;

            abas[i].setBackground(fundoArredondado(
                    selecionada ? "#521BB3" : "#EFEFEF", 14
            ));
            abas[i].setTextColor(Color.parseColor(
                    selecionada ? "#FFFFFF" : "#521BB3"
            ));
        }

        tvTituloBimestre.setText(n + "º bimestre");
        tvPeriodoBimestre.setText(
                periodosTexto[n] != null ? periodosTexto[n] : "Período não definido"
        );

        if (dados == null) {
            return;
        }

        progressDesempenho.setVisibility(View.GONE);
        linhas.clear();

        if (modoProfessor) {
            montarLinhasProfessor(n);
        } else {
            montarLinhasAluno(n);
        }

        adapter.notifyDataSetChanged();

        tvSemDados.setVisibility(linhas.isEmpty() ? View.VISIBLE : View.GONE);
    }

    private void montarLinhasProfessor(int n) {

        tvSemDados.setText("Nenhum aluno matriculado nas suas disciplinas.");

        for (Participante aluno : dados.alunos) {

            Desempenho.Resumo resumo = dados.desempenho.doAluno(aluno.perfilId, n);

            linhas.add(new Linha(
                    aluno.nome,
                    corPara(aluno.perfilId),
                    null,
                    formatarPontos(resumo.pontos),
                    resumo.faltas + (resumo.faltas == 1 ? " falta" : " faltas")
                            + " · " + resumo.justificadas + " justificada(s)",
                    true,
                    aluno.fotoPath,
                    aluno.fotoGoogleUrl
            ));
        }
    }

    private void montarLinhasAluno(int n) {

        tvSemDados.setText("Você ainda não está matriculado em disciplinas.");

        String meuId = dados.eu.id;
        Desempenho.Resumo total = dados.desempenho.doAluno(meuId, n);

        for (Map.Entry<String, String> disciplina : dados.disciplinas.entrySet()) {

            Desempenho.Resumo resumo = dados.desempenho.daDisciplina(meuId, disciplina.getKey(), n);

            linhas.add(new Linha(
                    disciplina.getValue(),
                    corPara(disciplina.getKey()),
                    resumo.avaliadas + " de " + resumo.atividades + " atividade(s) avaliada(s)",
                    formatarPontos(resumo.pontos) + " pts",
                    null,
                    false,
                    null,
                    null
            ));
        }

        tvPontosBimestre.setText(formatarPontos(total.pontos) + " pts");
        tvFaltasBimestre.setText(String.valueOf(total.faltas));
        tvJustificadasBimestre.setText(String.valueOf(total.justificadas));
    }

    private void aplicarFundos() {

        GradientDrawable decoracao = new GradientDrawable();
        decoracao.setShape(GradientDrawable.OVAL);
        decoracao.setColor(Color.parseColor("#C9A9FB"));
        findViewById(R.id.decoracaoCard).setBackground(decoracao);

        GradientDrawable circulo = new GradientDrawable();
        circulo.setShape(GradientDrawable.OVAL);
        circulo.setColor(Color.parseColor("#C9A9FB"));
        findViewById(R.id.circuloIconeDesempenho).setBackground(circulo);

        findViewById(R.id.cardPontosBimestre).setBackground(fundoArredondado("#D9C6FF", 14));
        findViewById(R.id.cardFaltasBimestre).setBackground(fundoArredondado("#FFC0A8", 14));
        findViewById(R.id.cardJustificadasBimestre).setBackground(fundoArredondado("#F7A8DC", 14));
    }

    // =====================================================
    // UTILITÁRIOS
    // =====================================================

    private String formatarPontos(double pontos) {

        if (pontos == Math.rint(pontos)) {
            return String.valueOf((long) pontos);
        }

        return String.format(Locale.getDefault(), "%.1f", pontos);
    }

    private String corPara(String id) {
        return CORES[Math.abs(id.hashCode()) % CORES.length];
    }

    private GradientDrawable fundoArredondado(String cor, int raioDp) {

        GradientDrawable fundo = new GradientDrawable();
        fundo.setColor(Color.parseColor(cor));
        fundo.setCornerRadius(raioDp * getResources().getDisplayMetrics().density);
        return fundo;
    }

    // =====================================================
    // MODELOS
    // =====================================================

    private static class Linha {

        final String nome;
        final String cor;
        final String detalhe;
        final String pontos;
        final String faltas;
        final boolean mostrarIconePessoa;
        final String fotoPath;
        final String fotoGoogleUrl;

        Linha(
                String nome,
                String cor,
                String detalhe,
                String pontos,
                String faltas,
                boolean mostrarIconePessoa,
                String fotoPath,
                String fotoGoogleUrl
        ) {
            this.nome = nome;
            this.cor = cor;
            this.detalhe = detalhe;
            this.pontos = pontos;
            this.faltas = faltas;
            this.mostrarIconePessoa = mostrarIconePessoa;
            this.fotoPath = fotoPath;
            this.fotoGoogleUrl = fotoGoogleUrl;
        }

        boolean temFoto() {
            return (fotoPath != null && !fotoPath.isEmpty())
                    || (fotoGoogleUrl != null && !fotoGoogleUrl.isEmpty());
        }
    }

    // =====================================================
    // ADAPTER
    // =====================================================

    private class DesempenhoAdapter
            extends RecyclerView.Adapter<DesempenhoAdapter.ViewHolder> {

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view = getLayoutInflater().inflate(
                    R.layout.item_desempenho, parent, false
            );

            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            Linha linha = linhas.get(position);

            holder.imgPessoa.setVisibility(
                    linha.mostrarIconePessoa ? View.VISIBLE : View.GONE
            );

            if (linha.mostrarIconePessoa && linha.temFoto()) {

                holder.circulo.setBackground(null);

                ViewGroup.LayoutParams lp = holder.imgPessoa.getLayoutParams();
                int tamanho = (int) (36 * getResources().getDisplayMetrics().density);
                lp.width = tamanho;
                lp.height = tamanho;
                holder.imgPessoa.setLayoutParams(lp);

                holder.imgPessoa.setImageTintList(null);
                holder.imgPessoa.setScaleType(ImageView.ScaleType.CENTER_CROP);

                PerfilFotoUtil.mostrar(DesempenhoActivity.this, linha.fotoPath, linha.fotoGoogleUrl,
                        holder.imgPessoa);

            } else {

                GradientDrawable circulo = new GradientDrawable();
                circulo.setShape(GradientDrawable.OVAL);
                circulo.setColor(Color.parseColor(linha.cor));
                holder.circulo.setBackground(circulo);

                ViewGroup.LayoutParams lp = holder.imgPessoa.getLayoutParams();
                int tamanho = (int) (22 * getResources().getDisplayMetrics().density);
                lp.width = tamanho;
                lp.height = tamanho;
                holder.imgPessoa.setLayoutParams(lp);

                holder.imgPessoa.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                holder.imgPessoa.setImageResource(R.drawable.ic_pessoa);
                holder.imgPessoa.setImageTintList(
                        android.content.res.ColorStateList.valueOf(Color.WHITE)
                );
            }

            holder.tvNome.setText(linha.nome);
            holder.tvPontos.setText(linha.pontos);

            holder.tvDetalhe.setVisibility(linha.detalhe != null ? View.VISIBLE : View.GONE);
            holder.tvDetalhe.setText(linha.detalhe);

            holder.tvFaltas.setVisibility(linha.faltas != null ? View.VISIBLE : View.GONE);
            holder.tvFaltas.setText(linha.faltas);
        }

        @Override
        public int getItemCount() {
            return linhas.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            View circulo;
            ImageView imgPessoa;
            TextView tvNome;
            TextView tvDetalhe;
            TextView tvPontos;
            TextView tvFaltas;

            ViewHolder(View itemView) {
                super(itemView);

                circulo = itemView.findViewById(R.id.circuloDesempenho);
                imgPessoa = itemView.findViewById(R.id.imgPessoaDesempenho);
                tvNome = itemView.findViewById(R.id.tvNomeDesempenho);
                tvDetalhe = itemView.findViewById(R.id.tvDetalheDesempenho);
                tvPontos = itemView.findViewById(R.id.tvPontosDesempenho);
                tvFaltas = itemView.findViewById(R.id.tvFaltasDesempenho);
            }
        }
    }
}
