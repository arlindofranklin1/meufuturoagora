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

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Desempenho por bimestre.
 *
 * Professor: pontos e faltas de cada aluno matriculado nas suas disciplinas.
 * Aluno: seus pontos, faltas e faltas justificadas, e os pontos por disciplina.
 *
 * Os pontos vêm das entregas avaliadas (nota) e entram no bimestre do prazo
 * da atividade. As faltas vêm das frequências e entram no bimestre da data
 * da aula. Os períodos são os definidos pelo administrador.
 */
public class DesempenhoActivity extends AppCompatActivity {

    public static final String EXTRA_PERFIL = "perfil";
    public static final String PERFIL_PROFESSOR = "professor";
    public static final String PERFIL_ALUNO = "aluno";

    static final String STATUS_FALTA = "Falta";
    static final String STATUS_FALTA_JUSTIFICADA = "Falta justificada";

    // Limite seguro de valores por consulta whereIn do Firestore
    private static final int TAMANHO_LOTE = 10;

    private static final String[] CORES = {
            "#521BB3", "#E45832", "#22BEEA", "#8E44AD",
            "#27AE60", "#F39C12", "#E84393", "#16A085"
    };

    private FirebaseFirestore db;
    private String uid;
    private boolean modoProfessor;

    private final SimpleDateFormat formatoData =
            new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    // Índices 1 a 4
    private final Date[] iniciosBimestre = new Date[5];
    private final Date[] finsBimestre = new Date[5];
    private final String[] periodosTexto = new String[5];

    private int bimestreSelecionado = 1;
    private boolean dadosCarregados = false;

    // Dados carregados do Firestore
    private final Map<String, String> nomesAlunos = new LinkedHashMap<>();
    private final Map<String, String> fotosAlunos = new HashMap<>();
    private final Map<String, String> nomesDisciplinas = new LinkedHashMap<>();
    private final Map<String, AtividadeInfo> atividades = new HashMap<>();
    private final List<Pontuacao> pontuacoes = new ArrayList<>();
    private final List<Falta> faltas = new ArrayList<>();

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

        db = FirebaseFirestore.getInstance();
        formatoData.setLenient(false);

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

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            finish();
            return;
        }

        uid = usuario.getUid();

        mostrarBimestre();
        carregarDados();
    }

    // =====================================================
    // CARREGAMENTO
    // =====================================================

    private void carregarDados() {

        db.collection("configuracoes")
                .document("geral")
                .get()
                .continueWithTask(tarefa -> {

                    lerPeriodos(tarefa.getResult());

                    return modoProfessor
                            ? carregarDadosProfessor()
                            : carregarDadosAluno();
                })
                .addOnSuccessListener(this, resultado -> {

                    dadosCarregados = true;
                    mostrarBimestre();
                })
                .addOnFailureListener(this, e -> {

                    progressDesempenho.setVisibility(View.GONE);

                    Toast.makeText(
                            this,
                            "Erro ao carregar desempenho.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    private void lerPeriodos(DocumentSnapshot documento) {

        Date hoje = new Date();

        for (int i = 1; i <= 4; i++) {

            String inicio = documento.exists()
                    ? documento.getString("bimestre" + i + "Inicio") : null;
            String fim = documento.exists()
                    ? documento.getString("bimestre" + i + "Fim") : null;

            iniciosBimestre[i] = converterData(inicio);
            finsBimestre[i] = converterData(fim);

            if (iniciosBimestre[i] != null && finsBimestre[i] != null) {

                periodosTexto[i] = inicio + " - " + fim;

                if (!hoje.before(iniciosBimestre[i]) && !hoje.after(finsBimestre[i])) {
                    bimestreSelecionado = i;
                }
            }
        }
    }

    // Professor: alunos matriculados, atividades, entregas e faltas
    // das disciplinas ativas do professor.
    private Task<Void> carregarDadosProfessor() {

        return db.collection("disciplinas")
                .whereEqualTo("professorId", uid)
                .whereEqualTo("ativo", true)
                .get()
                .continueWithTask(tarefa -> {

                    for (DocumentSnapshot disciplina : tarefa.getResult()) {

                        String nome = disciplina.getString("nome");
                        nomesDisciplinas.put(
                                disciplina.getId(),
                                nome != null ? nome : "Disciplina"
                        );
                    }

                    Set<String> idsDisciplinas = nomesDisciplinas.keySet();

                    Task<List<DocumentSnapshot>> tMatriculas =
                            buscarEmLotes("matriculas", "disciplinaId", idsDisciplinas);
                    Task<List<DocumentSnapshot>> tAtividades =
                            buscarEmLotes("atividades", "disciplinaId", idsDisciplinas);
                    Task<List<DocumentSnapshot>> tFrequencias =
                            buscarEmLotes("frequencias", "disciplinaId", idsDisciplinas);
                    Task<List<DocumentSnapshot>> tArquivadas =
                            buscarEmLotes("frequenciasArquivadas", "disciplinaId", idsDisciplinas);

                    return Tasks.whenAllSuccess(tMatriculas, tAtividades, tFrequencias, tArquivadas)
                            .continueWithTask(todas -> {

                                Map<String, String> encontrados = new HashMap<>();

                                for (DocumentSnapshot matricula : tMatriculas.getResult()) {

                                    String alunoId = matricula.getString("alunoId");
                                    String alunoNome = matricula.getString("alunoNome");

                                    if (alunoId != null && !encontrados.containsKey(alunoId)) {
                                        encontrados.put(
                                                alunoId,
                                                alunoNome != null ? alunoNome : "Aluno"
                                        );
                                    }
                                }

                                // Ordena os alunos por nome
                                List<String> idsAlunos = new ArrayList<>(encontrados.keySet());
                                Collections.sort(idsAlunos, (a, b) ->
                                        encontrados.get(a).compareToIgnoreCase(encontrados.get(b)));

                                for (String alunoId : idsAlunos) {
                                    nomesAlunos.put(alunoId, encontrados.get(alunoId));
                                }

                                carregarFotosAlunos(idsAlunos);

                                lerAtividades(tAtividades.getResult());

                                List<DocumentSnapshot> frequencias = new ArrayList<>();
                                frequencias.addAll(tFrequencias.getResult());
                                frequencias.addAll(tArquivadas.getResult());

                                return carregarEntregasEFaltas(
                                        buscarEmLotes("entregas", "atividadeId", atividades.keySet()),
                                        frequencias
                                );
                            });
                });
    }

    // Aluno: disciplinas em que está matriculado, suas entregas e faltas.
    private Task<Void> carregarDadosAluno() {

        return db.collection("matriculas")
                .whereEqualTo("alunoId", uid)
                .get()
                .continueWithTask(tarefa -> {

                    Map<String, String> matriculadas = new LinkedHashMap<>();

                    for (DocumentSnapshot matricula : tarefa.getResult()) {

                        String disciplinaId = matricula.getString("disciplinaId");
                        String disciplinaNome = matricula.getString("disciplinaNome");

                        if (disciplinaId != null) {
                            matriculadas.put(
                                    disciplinaId,
                                    disciplinaNome != null ? disciplinaNome : "Disciplina"
                            );
                        }
                    }

                    Task<List<DocumentSnapshot>> tDisciplinas =
                            buscarEmLotes("disciplinas", null, matriculadas.keySet());
                    Task<List<DocumentSnapshot>> tAtividades =
                            buscarEmLotes("atividades", "disciplinaId", matriculadas.keySet());
                    Task<QuerySnapshot> tFrequencias = db.collection("frequencias")
                            .whereEqualTo("alunoId", uid).get();
                    Task<QuerySnapshot> tArquivadas = db.collection("frequenciasArquivadas")
                            .whereEqualTo("alunoId", uid).get();

                    return Tasks.whenAllSuccess(tDisciplinas, tAtividades, tFrequencias, tArquivadas)
                            .continueWithTask(todas -> {

                                // Somente disciplinas ativas (usa o nome atualizado)
                                for (DocumentSnapshot disciplina : tDisciplinas.getResult()) {

                                    if (Boolean.FALSE.equals(disciplina.getBoolean("ativo"))) {
                                        continue;
                                    }

                                    String nome = disciplina.getString("nome");
                                    nomesDisciplinas.put(
                                            disciplina.getId(),
                                            nome != null ? nome : matriculadas.get(disciplina.getId())
                                    );
                                }

                                lerAtividades(tAtividades.getResult());

                                List<DocumentSnapshot> frequencias = new ArrayList<>();
                                frequencias.addAll(tFrequencias.getResult().getDocuments());
                                frequencias.addAll(tArquivadas.getResult().getDocuments());

                                return carregarEntregasEFaltas(
                                        db.collection("entregas")
                                                .whereEqualTo("alunoId", uid)
                                                .get()
                                                .continueWith(t -> t.getResult().getDocuments()),
                                        frequencias
                                );
                            });
                });
    }

    private void lerAtividades(List<DocumentSnapshot> documentos) {

        for (DocumentSnapshot atividade : documentos) {

            String disciplinaId = atividade.getString("disciplinaId");

            if (Boolean.FALSE.equals(atividade.getBoolean("ativo"))
                    || !nomesDisciplinas.containsKey(disciplinaId)) {
                continue;
            }

            atividades.put(
                    atividade.getId(),
                    new AtividadeInfo(
                            disciplinaId,
                            converterData(atividade.getString("prazo"))
                    )
            );
        }
    }

    // Lê as entregas avaliadas e busca a data das aulas em que houve falta.
    private Task<Void> carregarEntregasEFaltas(
            Task<List<DocumentSnapshot>> tEntregas,
            List<DocumentSnapshot> frequencias
    ) {

        List<DocumentSnapshot> ausencias = new ArrayList<>();
        Set<String> idsAulas = new HashSet<>();

        for (DocumentSnapshot frequencia : frequencias) {

            String status = frequencia.getString("status");
            String aulaId = frequencia.getString("aulaId");

            if (aulaId != null
                    && nomesDisciplinas.containsKey(frequencia.getString("disciplinaId"))
                    && (STATUS_FALTA.equals(status) || STATUS_FALTA_JUSTIFICADA.equals(status))) {

                ausencias.add(frequencia);
                idsAulas.add(aulaId);
            }
        }

        Task<List<DocumentSnapshot>> tAulas = buscarEmLotes("aulas", null, idsAulas);

        return Tasks.whenAllSuccess(tEntregas, tAulas).continueWith(todas -> {

            for (DocumentSnapshot entrega : tEntregas.getResult()) {

                AtividadeInfo atividade = atividades.get(entrega.getString("atividadeId"));
                Double nota = entrega.getDouble("nota");
                String alunoId = entrega.getString("alunoId");

                if (atividade == null || nota == null || alunoId == null
                        || !Boolean.TRUE.equals(entrega.getBoolean("avaliado"))) {
                    continue;
                }

                pontuacoes.add(new Pontuacao(
                        alunoId,
                        atividade.disciplinaId,
                        bimestreDa(atividade.prazo),
                        nota
                ));
            }

            Map<String, Date> datasAulas = new HashMap<>();

            for (DocumentSnapshot aula : tAulas.getResult()) {
                datasAulas.put(aula.getId(), converterData(aula.getString("data")));
            }

            for (DocumentSnapshot ausencia : ausencias) {

                faltas.add(new Falta(
                        ausencia.getString("alunoId"),
                        ausencia.getString("disciplinaId"),
                        bimestreDa(datasAulas.get(ausencia.getString("aulaId"))),
                        STATUS_FALTA_JUSTIFICADA.equals(ausencia.getString("status"))
                ));
            }

            return null;
        });
    }

    // Faz consultas whereIn em lotes e junta os resultados.
    // campo == null busca pelo ID do documento.
    private Task<List<DocumentSnapshot>> buscarEmLotes(
            String colecao,
            String campo,
            Collection<String> valores
    ) {

        List<String> lista = new ArrayList<>(valores);
        List<Task<QuerySnapshot>> consultas = new ArrayList<>();

        for (int i = 0; i < lista.size(); i += TAMANHO_LOTE) {

            List<String> lote = new ArrayList<>(
                    lista.subList(i, Math.min(i + TAMANHO_LOTE, lista.size()))
            );

            Query consulta = campo == null
                    ? db.collection(colecao).whereIn(FieldPath.documentId(), lote)
                    : db.collection(colecao).whereIn(campo, lote);

            consultas.add(consulta.get());
        }

        return Tasks.whenAllSuccess(consultas).continueWith(tarefa -> {

            List<DocumentSnapshot> documentos = new ArrayList<>();

            for (Object resultado : tarefa.getResult()) {
                documentos.addAll(((QuerySnapshot) resultado).getDocuments());
            }

            return documentos;
        });
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

        if (!dadosCarregados) {
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

        for (Map.Entry<String, String> aluno : nomesAlunos.entrySet()) {

            String alunoId = aluno.getKey();

            double pontos = 0;
            int qtdFaltas = 0;
            int justificadas = 0;

            for (Pontuacao p : pontuacoes) {
                if (p.bimestre == n && alunoId.equals(p.alunoId)) {
                    pontos += p.nota;
                }
            }

            for (Falta f : faltas) {
                if (f.bimestre == n && alunoId.equals(f.alunoId)) {
                    if (f.justificada) {
                        justificadas++;
                    } else {
                        qtdFaltas++;
                    }
                }
            }

            linhas.add(new Linha(
                    aluno.getValue(),
                    corPara(alunoId),
                    null,
                    formatarPontos(pontos),
                    qtdFaltas + (qtdFaltas == 1 ? " falta" : " faltas")
                            + " · " + justificadas + " justificada(s)",
                    true,
                    fotosAlunos.get(alunoId)
            ));
        }
    }

    // Busca a foto de cada aluno na coleção "alunos" (em lotes de até 10,
    // limite do whereIn do Firestore) e atualiza a lista já exibida.
    private void carregarFotosAlunos(List<String> idsAlunos) {

        buscarEmLotes("alunos", null, idsAlunos).addOnSuccessListener(documentos -> {

            for (DocumentSnapshot documento : documentos) {
                fotosAlunos.put(documento.getId(), documento.getString("fotoUrl"));
            }

            mostrarBimestre();
        });
    }

    private void montarLinhasAluno(int n) {

        tvSemDados.setText("Você ainda não está matriculado em disciplinas.");

        double totalPontos = 0;
        int qtdFaltas = 0;
        int justificadas = 0;

        for (Falta f : faltas) {
            if (f.bimestre == n && uid.equals(f.alunoId)) {
                if (f.justificada) {
                    justificadas++;
                } else {
                    qtdFaltas++;
                }
            }
        }

        for (Map.Entry<String, String> disciplina : nomesDisciplinas.entrySet()) {

            String disciplinaId = disciplina.getKey();

            double pontos = 0;
            int avaliadas = 0;
            int total = 0;

            for (AtividadeInfo atividade : atividades.values()) {
                if (disciplinaId.equals(atividade.disciplinaId)
                        && bimestreDa(atividade.prazo) == n) {
                    total++;
                }
            }

            for (Pontuacao p : pontuacoes) {
                if (p.bimestre == n && disciplinaId.equals(p.disciplinaId)) {
                    pontos += p.nota;
                    avaliadas++;
                }
            }

            totalPontos += pontos;

            linhas.add(new Linha(
                    disciplina.getValue(),
                    corPara(disciplinaId),
                    avaliadas + " de " + total + " atividade(s) avaliada(s)",
                    formatarPontos(pontos) + " pts",
                    null,
                    false,
                    null
            ));
        }

        tvPontosBimestre.setText(formatarPontos(totalPontos) + " pts");
        tvFaltasBimestre.setText(String.valueOf(qtdFaltas));
        tvJustificadasBimestre.setText(String.valueOf(justificadas));
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

    private int bimestreDa(Date data) {

        if (data == null) {
            return 0;
        }

        for (int i = 1; i <= 4; i++) {

            if (iniciosBimestre[i] != null && finsBimestre[i] != null
                    && !data.before(iniciosBimestre[i]) && !data.after(finsBimestre[i])) {
                return i;
            }
        }

        return 0;
    }

    private Date converterData(String texto) {

        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }

        try {
            return formatoData.parse(texto.trim());
        } catch (ParseException e) {
            return null;
        }
    }

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

    private static class AtividadeInfo {

        final String disciplinaId;
        final Date prazo;

        AtividadeInfo(String disciplinaId, Date prazo) {
            this.disciplinaId = disciplinaId;
            this.prazo = prazo;
        }
    }

    private static class Pontuacao {

        final String alunoId;
        final String disciplinaId;
        final int bimestre;
        final double nota;

        Pontuacao(String alunoId, String disciplinaId, int bimestre, double nota) {
            this.alunoId = alunoId;
            this.disciplinaId = disciplinaId;
            this.bimestre = bimestre;
            this.nota = nota;
        }
    }

    private static class Falta {

        final String alunoId;
        final String disciplinaId;
        final int bimestre;
        final boolean justificada;

        Falta(String alunoId, String disciplinaId, int bimestre, boolean justificada) {
            this.alunoId = alunoId;
            this.disciplinaId = disciplinaId;
            this.bimestre = bimestre;
            this.justificada = justificada;
        }
    }

    private static class Linha {

        final String nome;
        final String cor;
        final String detalhe;
        final String pontos;
        final String faltas;
        final boolean mostrarIconePessoa;
        final String fotoUrl;

        Linha(
                String nome,
                String cor,
                String detalhe,
                String pontos,
                String faltas,
                boolean mostrarIconePessoa,
                String fotoUrl
        ) {
            this.nome = nome;
            this.cor = cor;
            this.detalhe = detalhe;
            this.pontos = pontos;
            this.faltas = faltas;
            this.mostrarIconePessoa = mostrarIconePessoa;
            this.fotoUrl = fotoUrl;
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

            if (linha.mostrarIconePessoa && linha.fotoUrl != null && !linha.fotoUrl.isEmpty()) {

                holder.circulo.setBackground(null);

                ViewGroup.LayoutParams lp = holder.imgPessoa.getLayoutParams();
                int tamanho = (int) (36 * getResources().getDisplayMetrics().density);
                lp.width = tamanho;
                lp.height = tamanho;
                holder.imgPessoa.setLayoutParams(lp);

                holder.imgPessoa.setImageTintList(null);
                holder.imgPessoa.setScaleType(ImageView.ScaleType.CENTER_CROP);

                com.bumptech.glide.Glide.with(DesempenhoActivity.this)
                        .load(linha.fotoUrl)
                        .circleCrop()
                        .into(holder.imgPessoa);

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
