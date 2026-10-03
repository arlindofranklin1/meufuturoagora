package br.com.example.meufuturoagora;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeSet;

// Histórico do administrador: consulta, por ano letivo e bimestre, a pontuação
// dos alunos, as trilhas enviadas pelos professores e as entregas dos alunos.
// Registros novos trazem o ano letivo e o bimestre gravados; os antigos (de
// antes dessa marcação) são encaixados pelas datas dos bimestres.
public class HistoricoAnoLetivoActivity extends AppCompatActivity {

    private static final int ABA_ALUNOS = 0;
    private static final int ABA_TRILHAS = 1;
    private static final int ABA_ENTREGAS = 2;

    private static final String SEM_TURMA = "Sem turma";

    private static final String[] PERIODOS = {
            "Ano inteiro", "1º bimestre", "2º bimestre", "3º bimestre", "4º bimestre"
    };

    private FirebaseFirestore db;

    private Spinner spinnerAno;
    private Spinner spinnerBimestre;
    private Spinner spinnerTurma;

    // Opções do filtro de turma (a primeira é "Todas as turmas")
    private final List<String> turmas = new ArrayList<>();
    // Nomes das turmas que ainda existem na coleção "turmas"
    private final TreeSet<String> turmasExistentes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
    private TextView tvResumo;
    private EditText edtBuscar;
    private ProgressBar progress;
    private final TextView[] abas = new TextView[3];

    private final List<Linha> linhas = new ArrayList<>();
    private HistoricoAdapter adapter;

    private int abaSelecionada = ABA_ALUNOS;
    private boolean dadosCarregados = false;

    // Dados carregados uma única vez e filtrados na tela
    private DocumentSnapshot configuracao;
    private final Map<String, DocumentSnapshot> anosLetivos = new HashMap<>();
    private final Map<String, DocumentSnapshot> alunos = new HashMap<>();
    private final Map<String, DocumentSnapshot> disciplinas = new HashMap<>();
    private final Map<String, DocumentSnapshot> atividades = new HashMap<>();
    private List<DocumentSnapshot> entregas = new ArrayList<>();
    private List<DocumentSnapshot> penalidades = new ArrayList<>();

    private final List<String> anos = new ArrayList<>();

    private final SimpleDateFormat formatoData =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_historico_ano_letivo);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        db = FirebaseFirestore.getInstance();

        findViewById(R.id.btnVoltar).setOnClickListener(v -> finish());

        spinnerAno = findViewById(R.id.spinnerAnoHistorico);
        spinnerBimestre = findViewById(R.id.spinnerBimestreHistorico);
        spinnerTurma = findViewById(R.id.spinnerTurmaHistorico);
        tvResumo = findViewById(R.id.tvResumoHistorico);
        progress = findViewById(R.id.progressHistorico);

        // Pesquisa: filtra a lista enquanto digita
        edtBuscar = findViewById(R.id.edtBuscarHistorico);
        edtBuscar.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                mostrar();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        abas[ABA_ALUNOS] = findViewById(R.id.abaAlunos);
        abas[ABA_TRILHAS] = findViewById(R.id.abaTrilhas);
        abas[ABA_ENTREGAS] = findViewById(R.id.abaEntregas);

        for (int i = 0; i < abas.length; i++) {

            int aba = i;

            abas[i].setOnClickListener(v -> {
                abaSelecionada = aba;
                mostrar();
            });
        }

        ArrayAdapter<String> adapterPeriodos =
                new ArrayAdapter<>(this, R.layout.item_spinner, PERIODOS);
        adapterPeriodos.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerBimestre.setAdapter(adapterPeriodos);
        spinnerBimestre.setOnItemSelectedListener(new AoSelecionar());

        RecyclerView recycler = findViewById(R.id.recyclerHistorico);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoricoAdapter();
        recycler.setAdapter(adapter);

        mostrar();
        carregarDados();
    }

    // =====================================================
    // CARREGAMENTO
    // =====================================================

    private void carregarDados() {

        Task<DocumentSnapshot> tConfig = db.collection("configuracoes").document("geral").get();
        Task<QuerySnapshot> tAnos = db.collection("anosLetivos").get();
        Task<QuerySnapshot> tTurmas = db.collection("turmas").get();
        Task<QuerySnapshot> tAlunos = db.collection("alunos").get();
        Task<QuerySnapshot> tDisciplinas = db.collection("disciplinas").get();
        Task<QuerySnapshot> tAtividades = db.collection("atividades").get();
        Task<QuerySnapshot> tEntregas = db.collection("entregas").get();
        Task<QuerySnapshot> tPenalidades = db.collection("penalidades").get();

        Tasks.whenAllSuccess(Arrays.asList(
                        tConfig, tAnos, tTurmas, tAlunos, tDisciplinas, tAtividades, tEntregas, tPenalidades))
                .addOnSuccessListener(resultados -> {

                    configuracao = tConfig.getResult();

                    for (DocumentSnapshot d : tAnos.getResult()) anosLetivos.put(d.getId(), d);
                    for (DocumentSnapshot d : tAlunos.getResult()) alunos.put(d.getId(), d);
                    for (DocumentSnapshot d : tDisciplinas.getResult()) disciplinas.put(d.getId(), d);
                    for (DocumentSnapshot d : tAtividades.getResult()) atividades.put(d.getId(), d);

                    entregas = tEntregas.getResult().getDocuments();
                    penalidades = tPenalidades.getResult().getDocuments();

                    montarListaDeAnos();
                    montarListaDeTurmas(tTurmas.getResult());

                    dadosCarregados = true;
                    mostrar();
                })
                .addOnFailureListener(e -> {

                    progress.setVisibility(View.GONE);
                    Toast.makeText(
                            this,
                            "Erro ao carregar o histórico: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    private void montarListaDeAnos() {

        // Do mais recente para o mais antigo
        TreeSet<String> conjunto = new TreeSet<>((a, b) -> b.compareTo(a));

        String anoAtual = configuracao.getString(AnoLetivoUtil.CAMPO_ANO);

        if (anoAtual != null && !anoAtual.isEmpty()) {
            conjunto.add(anoAtual);
        }

        conjunto.addAll(anosLetivos.keySet());

        List<DocumentSnapshot> todos = new ArrayList<>(atividades.values());
        todos.addAll(entregas);
        todos.addAll(penalidades);

        for (DocumentSnapshot documento : todos) {

            String ano = documento.getString(AnoLetivoUtil.CAMPO_ANO);

            if (ano != null && !ano.isEmpty()) {
                conjunto.add(ano);
            }
        }

        if (conjunto.isEmpty()) {
            conjunto.add(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)));
        }

        anos.clear();
        anos.addAll(conjunto);

        List<String> rotulos = new ArrayList<>();

        for (String ano : anos) {
            rotulos.add(ano.equals(anoAtual) ? ano + " (atual)" : ano);
        }

        ArrayAdapter<String> adapterAnos = new ArrayAdapter<>(this, R.layout.item_spinner, rotulos);
        adapterAnos.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerAno.setAdapter(adapterAnos);
        spinnerAno.setSelection(Math.max(0, anos.indexOf(anoAtual)));
        spinnerAno.setOnItemSelectedListener(new AoSelecionar());
    }

    // Somente as turmas cadastradas (turma excluída não aparece no filtro; os
    // registros dela ficam em "Sem turma")
    private void montarListaDeTurmas(QuerySnapshot turmasCadastradas) {

        turmasExistentes.clear();
        boolean temSemTurma = false;

        for (DocumentSnapshot turma : turmasCadastradas) {
            String nome = turma.getString("nome");
            if (nome != null && !nome.isEmpty()) turmasExistentes.add(nome);
        }

        List<DocumentSnapshot> comTurma = new ArrayList<>(alunos.values());
        comTurma.addAll(disciplinas.values());

        for (DocumentSnapshot documento : comTurma) {

            String nome = documento.getString("turmaNome");

            if (nome == null || !turmasExistentes.contains(nome)) {
                temSemTurma = true;
            }
        }

        turmas.clear();
        turmas.add("Todas as turmas");
        turmas.addAll(turmasExistentes);

        if (temSemTurma) {
            turmas.add(SEM_TURMA);
        }

        ArrayAdapter<String> adapterTurmas = new ArrayAdapter<>(this, R.layout.item_spinner, turmas);
        adapterTurmas.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerTurma.setAdapter(adapterTurmas);
        spinnerTurma.setOnItemSelectedListener(new AoSelecionar());
    }

    // =====================================================
    // FILTRO POR ANO LETIVO E BIMESTRE
    // =====================================================

    // Documento com as datas dos bimestres do ano (configuração atual ou arquivo do ano)
    private DocumentSnapshot datasDoAno(String ano) {

        String anoAtual = configuracao.getString(AnoLetivoUtil.CAMPO_ANO);

        if (ano.equals(anoAtual) && temBimestres(configuracao)) {
            return configuracao;
        }

        return anosLetivos.get(ano);
    }

    private boolean temBimestres(DocumentSnapshot documento) {

        return documento != null
                && BimestreUtil.converter(documento.getString("bimestre1Inicio")) != null;
    }

    private boolean pertence(DocumentSnapshot documento, Date data, String ano, int bimestre) {

        DocumentSnapshot datas = datasDoAno(ano);
        String anoDoRegistro = documento.getString(AnoLetivoUtil.CAMPO_ANO);

        if (anoDoRegistro != null) {

            if (!anoDoRegistro.equals(ano)) {
                return false;
            }

        } else if (!dataNoAno(data, ano, datas)) {

            // Registro antigo, sem ano letivo gravado: vale a data
            return false;
        }

        if (bimestre == 0) {
            return true;
        }

        Long bimestreDoRegistro = documento.getLong(AnoLetivoUtil.CAMPO_BIMESTRE);

        if (bimestreDoRegistro != null) {
            return bimestreDoRegistro == bimestre;
        }

        return BimestreUtil.numeroDaData(datas, data) == bimestre;
    }

    // Dentro do período dos bimestres do ano; sem bimestres definidos, vale o ano do calendário
    private boolean dataNoAno(Date data, String ano, DocumentSnapshot datas) {
        return BimestreUtil.dataNoAno(data, ano, datas);
    }

    private Date data(DocumentSnapshot documento, String... campos) {

        for (String campo : campos) {

            Object valor = documento.get(campo);

            if (valor instanceof Timestamp) {
                return ((Timestamp) valor).toDate();
            }

            if (valor instanceof String) {

                Date convertida = BimestreUtil.converter((String) valor);

                if (convertida != null) {
                    return convertida;
                }
            }
        }

        return null;
    }

    // =====================================================
    // EXIBIÇÃO
    // =====================================================

    private void mostrar() {

        for (int i = 0; i < abas.length; i++) {

            boolean selecionada = i == abaSelecionada;

            GradientDrawable fundo = new GradientDrawable();
            fundo.setColor(Color.parseColor(selecionada ? "#521BB3" : "#EFEFEF"));
            fundo.setCornerRadius(14 * getResources().getDisplayMetrics().density);

            abas[i].setBackground(fundo);
            abas[i].setTextColor(Color.parseColor(selecionada ? "#FFFFFF" : "#521BB3"));
        }

        edtBuscar.setHint(abaSelecionada == ABA_ALUNOS
                ? "Buscar alunos"
                : abaSelecionada == ABA_TRILHAS
                ? "Buscar trilhas, disciplinas ou professores"
                : "Buscar alunos ou trilhas");

        if (!dadosCarregados || anos.isEmpty()) {
            return;
        }

        progress.setVisibility(View.GONE);

        int posicaoAno = Math.max(0, spinnerAno.getSelectedItemPosition());
        String ano = anos.get(Math.min(posicaoAno, anos.size() - 1));
        int bimestre = Math.max(0, spinnerBimestre.getSelectedItemPosition());

        linhas.clear();

        if (abaSelecionada == ABA_ALUNOS) {
            montarAlunos(ano, bimestre);
        } else if (abaSelecionada == ABA_TRILHAS) {
            montarTrilhas(ano, bimestre);
        } else {
            montarEntregas(ano, bimestre);
        }

        String periodo = PERIODOS[bimestre].toLowerCase();
        DocumentSnapshot datas = datasDoAno(ano);

        if (bimestre > 0 && datas != null) {

            String inicio = datas.getString("bimestre" + bimestre + "Inicio");
            String fim = datas.getString("bimestre" + bimestre + "Fim");

            if (inicio != null && !inicio.isEmpty() && fim != null && !fim.isEmpty()) {
                periodo += " (" + inicio + " a " + fim + ")";
            }
        }

        agruparPorTurma();

        int total = 0;

        for (Linha linha : linhas) {
            if (!linha.cabecalho) total++;
        }

        String prefixo = total == 0
                ? "Nenhum registro"
                : total + (total == 1 ? " registro" : " registros");

        tvResumo.setText(prefixo + " em " + ano + " — " + periodo);

        adapter.notifyDataSetChanged();
    }

    private void montarAlunos(String ano, int bimestre) {

        Map<String, double[]> porAluno = new HashMap<>(); // {pontos, entregas, faltas}

        for (DocumentSnapshot entrega : entregas) {

            if (!pertence(entrega, data(entrega, "enviadoEm", "avaliadoEm"), ano, bimestre)) {
                continue;
            }

            String alunoId = entrega.getString("alunoId");

            if (alunoId == null) {
                continue;
            }

            double[] valores = porAluno.computeIfAbsent(alunoId, k -> new double[3]);
            valores[1]++;

            Double nota = entrega.getDouble("nota");

            if (Boolean.TRUE.equals(entrega.getBoolean("avaliado")) && nota != null) {
                valores[0] += nota;
            }
        }

        for (DocumentSnapshot penalidade : penalidades) {

            if (!pertence(penalidade, data(penalidade, "criadoEm", "data"), ano, bimestre)) {
                continue;
            }

            String alunoId = penalidade.getString("alunoId");

            if (alunoId == null) {
                continue;
            }

            double[] valores = porAluno.computeIfAbsent(alunoId, k -> new double[3]);
            valores[0] -= PenalidadeFaltaUtil.PONTOS_POR_DIA;
            valores[2]++;
        }

        for (Map.Entry<String, double[]> item : porAluno.entrySet()) {

            DocumentSnapshot aluno = alunos.get(item.getKey());
            double[] v = item.getValue();

            String nome = aluno != null && aluno.getString("nome") != null
                    ? aluno.getString("nome") : "Aluno removido";
            String turma = aluno != null ? aluno.getString("turmaNome") : null;

            linhas.add(new Linha(
                    nome,
                    (int) v[1] + " entrega(s) • " + (int) v[2] + " dia(s) com falta",
                    Math.round(v[0]) + " pts",
                    v[0],
                    turma,
                    nome
            ));
        }

        // Ranking do período: maior pontuação primeiro
        linhas.sort((a, b) -> Double.compare(b.ordem, a.ordem));
    }

    private void montarTrilhas(String ano, int bimestre) {

        Map<String, Integer> entregasPorTrilha = new HashMap<>();

        for (DocumentSnapshot entrega : entregas) {

            String atividadeId = entrega.getString("atividadeId");

            if (atividadeId != null) {
                entregasPorTrilha.merge(atividadeId, 1, Integer::sum);
            }
        }

        for (DocumentSnapshot atividade : atividades.values()) {

            Date criadaEm = data(atividade, "criadoEm", "prazo");

            if (!pertence(atividade, criadaEm, ano, bimestre)) {
                continue;
            }

            DocumentSnapshot disciplina = disciplinas.get(atividade.getString("disciplinaId"));

            String disciplinaNome = disciplina != null && disciplina.getString("nome") != null
                    ? disciplina.getString("nome") : "Disciplina removida";
            String professorNome = disciplina != null && disciplina.getString("professorNome") != null
                    ? disciplina.getString("professorNome") : "Professor";
            String prazo = atividade.getString("prazo");
            Long pontos = atividade.getLong("pontos");
            int qtdEntregas = entregasPorTrilha.getOrDefault(atividade.getId(), 0);

            String nome = atividade.getString("nome") != null ? atividade.getString("nome") : "Trilha";

            if (Boolean.FALSE.equals(atividade.getBoolean("ativo"))) {
                nome += " (excluída)";
            }

            linhas.add(new Linha(
                    nome,
                    disciplinaNome + " • Prof. " + professorNome
                            + "\nPrazo: " + (prazo != null ? prazo : "—")
                            + " • " + qtdEntregas + " entrega(s)",
                    (pontos != null ? pontos : 0) + " pts",
                    criadaEm != null ? criadaEm.getTime() : 0,
                    disciplina != null ? disciplina.getString("turmaNome") : null,
                    nome + " " + disciplinaNome + " " + professorNome
            ));
        }

        // Mais recentes primeiro
        linhas.sort((a, b) -> Double.compare(b.ordem, a.ordem));
    }

    private void montarEntregas(String ano, int bimestre) {

        for (DocumentSnapshot entrega : entregas) {

            Date enviadaEm = data(entrega, "enviadoEm", "avaliadoEm");

            if (!pertence(entrega, enviadaEm, ano, bimestre)) {
                continue;
            }

            DocumentSnapshot aluno = alunos.get(entrega.getString("alunoId"));
            DocumentSnapshot atividade = atividades.get(entrega.getString("atividadeId"));
            DocumentSnapshot disciplina = atividade != null
                    ? disciplinas.get(atividade.getString("disciplinaId"))
                    : null;

            String alunoNome = aluno != null && aluno.getString("nome") != null
                    ? aluno.getString("nome") : "Aluno removido";
            String trilhaNome = atividade != null && atividade.getString("nome") != null
                    ? atividade.getString("nome") : "Trilha removida";
            String disciplinaNome = disciplina != null && disciplina.getString("nome") != null
                    ? disciplina.getString("nome") : "";

            Double nota = entrega.getDouble("nota");
            boolean avaliada = Boolean.TRUE.equals(entrega.getBoolean("avaliado"));

            String valor = avaliada && nota != null ? formatarNota(nota) + " pts" : "Pendente";

            String turma = aluno != null && aluno.getString("turmaNome") != null
                    ? aluno.getString("turmaNome")
                    : disciplina != null ? disciplina.getString("turmaNome") : null;

            linhas.add(new Linha(
                    alunoNome,
                    trilhaNome + (disciplinaNome.isEmpty() ? "" : " • " + disciplinaNome)
                            + "\nEnviada em " + (enviadaEm != null ? formatoData.format(enviadaEm) : "—"),
                    valor,
                    enviadaEm != null ? enviadaEm.getTime() : 0,
                    turma,
                    alunoNome + " " + trilhaNome + " " + disciplinaNome
            ));
        }

        linhas.sort((a, b) -> Double.compare(b.ordem, a.ordem));
    }

    private String formatarNota(double nota) {
        return nota == Math.floor(nota) ? String.valueOf((long) nota) : String.valueOf(nota);
    }

    private class AoSelecionar implements AdapterView.OnItemSelectedListener {

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            mostrar();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    // =====================================================
    // LISTA
    // =====================================================

    private static class Linha {

        final String titulo;
        final String subtitulo;
        final String valor;
        final double ordem;
        final String turma;
        final String busca;

        // Cabeçalho de turma: titulo = nome da turma, valor = quantidade
        final boolean cabecalho;

        Linha(String titulo, String subtitulo, String valor, double ordem, String turma, String busca) {
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.valor = valor;
            this.ordem = ordem;
            this.turma = turma != null && !turma.isEmpty() ? turma : SEM_TURMA;
            this.busca = busca != null ? busca.toLowerCase() : "";
            this.cabecalho = false;
        }

        Linha(String turma, int quantidade) {
            this.titulo = turma;
            this.subtitulo = null;
            this.valor = String.valueOf(quantidade);
            this.ordem = 0;
            this.turma = turma;
            this.busca = "";
            this.cabecalho = true;
        }
    }

    // Aplica a pesquisa e separa a lista por turma (cada turma com seu cabeçalho)
    private void agruparPorTurma() {

        String busca = edtBuscar.getText().toString().trim().toLowerCase();

        // Turma escolhida no spinner (posição 0 = todas)
        int posicaoTurma = spinnerTurma.getSelectedItemPosition();
        String turmaFiltro = posicaoTurma > 0 && posicaoTurma < turmas.size()
                ? turmas.get(posicaoTurma)
                : null;

        // Turmas em ordem alfabética, "Sem turma" no final; a ordem dentro da turma é mantida
        Map<String, List<Linha>> porTurma = new java.util.TreeMap<>((a, b) -> {
            if (a.equals(b)) return 0;
            if (a.equals(SEM_TURMA)) return 1;
            if (b.equals(SEM_TURMA)) return -1;
            return a.compareToIgnoreCase(b);
        });

        for (Linha linha : linhas) {

            // Registro de uma turma que foi excluída entra em "Sem turma"
            String turmaDaLinha = turmasExistentes.contains(linha.turma) ? linha.turma : SEM_TURMA;

            if (turmaFiltro != null && !turmaFiltro.equalsIgnoreCase(turmaDaLinha)) {
                continue;
            }

            if (busca.isEmpty() || linha.busca.contains(busca)) {
                porTurma.computeIfAbsent(turmaDaLinha, k -> new ArrayList<>()).add(linha);
            }
        }

        linhas.clear();

        for (Map.Entry<String, List<Linha>> grupo : porTurma.entrySet()) {
            linhas.add(new Linha(grupo.getKey(), grupo.getValue().size()));
            linhas.addAll(grupo.getValue());
        }
    }

    private class HistoricoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

        private static final int TIPO_CABECALHO = 0;
        private static final int TIPO_ITEM = 1;

        @Override
        public int getItemViewType(int position) {
            return linhas.get(position).cabecalho ? TIPO_CABECALHO : TIPO_ITEM;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            if (viewType == TIPO_CABECALHO) {
                return new CabecalhoHolder(getLayoutInflater()
                        .inflate(R.layout.item_historico_turma, parent, false));
            }

            return new ViewHolder(getLayoutInflater().inflate(R.layout.item_historico, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {

            Linha linha = linhas.get(position);

            if (holder instanceof CabecalhoHolder) {

                ((CabecalhoHolder) holder).tvTurma.setText(linha.titulo);
                ((CabecalhoHolder) holder).tvQuantidade.setText(linha.valor);
                return;
            }

            ViewHolder item = (ViewHolder) holder;
            item.tvTitulo.setText(linha.titulo);
            item.tvSubtitulo.setText(linha.subtitulo);
            item.tvValor.setText(linha.valor);
        }

        @Override
        public int getItemCount() {
            return linhas.size();
        }

        class CabecalhoHolder extends RecyclerView.ViewHolder {

            final TextView tvTurma;
            final TextView tvQuantidade;

            CabecalhoHolder(View itemView) {
                super(itemView);
                tvTurma = itemView.findViewById(R.id.tvNomeTurmaHistorico);
                tvQuantidade = itemView.findViewById(R.id.tvQtdTurmaHistorico);
            }
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            final TextView tvTitulo;
            final TextView tvSubtitulo;
            final TextView tvValor;

            ViewHolder(View itemView) {
                super(itemView);
                tvTitulo = itemView.findViewById(R.id.tvTituloHistorico);
                tvSubtitulo = itemView.findViewById(R.id.tvSubtituloHistorico);
                tvValor = itemView.findViewById(R.id.tvValorHistorico);
            }
        }
    }
}
