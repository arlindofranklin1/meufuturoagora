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

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import br.com.example.meufuturoagora.dados.AnoLetivo;
import br.com.example.meufuturoagora.dados.Bimestre;
import br.com.example.meufuturoagora.dados.Bimestres;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.HistoricoAluno;
import br.com.example.meufuturoagora.dados.HistoricoEntrega;
import br.com.example.meufuturoagora.dados.HistoricoRepositorio;
import br.com.example.meufuturoagora.dados.HistoricoTrilha;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;
import br.com.example.meufuturoagora.dados.Turma;

// Histórico do administrador: consulta, por ano letivo e bimestre, a pontuação
// dos alunos, as trilhas enviadas pelos professores e as entregas dos alunos.
// O servidor calcula (historico_alunos, historico_trilhas, historico_entregas)
// e devolve só o período pedido; aqui ficam a pesquisa, o filtro por turma e
// o agrupamento, que não precisam de nova consulta.
public class HistoricoAnoLetivoActivity extends AppCompatActivity {

    private static final int ABA_ALUNOS = 0;
    private static final int ABA_TRILHAS = 1;
    private static final int ABA_ENTREGAS = 2;

    private static final String SEM_TURMA = "Sem turma";

    private static final String[] PERIODOS = {
            "Ano inteiro", "1º bimestre", "2º bimestre", "3º bimestre", "4º bimestre"
    };

    // A escola fica no Brasil: a data de envio aparece no horário de Brasília
    private static final DateTimeFormatter DATA_ENVIO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy").withZone(ZoneId.of("America/Sao_Paulo"));

    private Spinner spinnerAno;
    private Spinner spinnerBimestre;
    private Spinner spinnerTurma;

    private TextView tvResumo;
    private EditText edtBuscar;
    private ProgressBar progress;
    private final TextView[] abas = new TextView[3];

    // Opções do filtro de turma (a primeira é "Todas as turmas")
    private final List<String> turmas = new ArrayList<>();
    private final List<Integer> anos = new ArrayList<>();
    private Integer anoAtual;

    // Linhas do período escolhido, sem filtro; "linhas" é o que a lista mostra
    private final List<Linha> base = new ArrayList<>();
    private final List<Linha> linhas = new ArrayList<>();
    private List<Bimestre> bimestresDoAno = new ArrayList<>();
    private HistoricoAdapter adapter;

    private int abaSelecionada = ABA_ALUNOS;
    private boolean anosCarregados = false;
    private boolean periodoCarregado = false;

    // Evita buscar duas vezes o mesmo período (o Android avisa a seleção inicial
    // dos spinners) e descarta respostas atrasadas de uma escolha anterior
    private String chaveSolicitada;
    private int pedido = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_historico_ano_letivo);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

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
                recarregar();
            });
        }

        ArrayAdapter<String> adapterPeriodos =
                new ArrayAdapter<>(this, R.layout.item_spinner, PERIODOS);
        adapterPeriodos.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerBimestre.setAdapter(adapterPeriodos);
        spinnerBimestre.setOnItemSelectedListener(new AoSelecionar(true));

        RecyclerView recycler = findViewById(R.id.recyclerHistorico);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoricoAdapter();
        recycler.setAdapter(adapter);

        mostrar();
        carregarAnosETurmas();
    }

    // =====================================================
    // CARREGAMENTO
    // =====================================================

    private void carregarAnosETurmas() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            List<Integer> comRegistro = servicos.historico.anos();
            AnoLetivo atual = servicos.escola.anoAtual();
            List<Turma> cadastradas = servicos.escola.turmas();

            return new Base(comRegistro, atual != null ? atual.ano : null, cadastradas);

        }, new Tarefas.Retorno<Base>() {
            @Override
            public void sucesso(Base carregada) {

                anoAtual = carregada.anoAtual;
                montarListaDeAnos(carregada.anos);
                montarListaDeTurmas(carregada.turmas);

                anosCarregados = true;
                recarregar();
            }

            @Override
            public void erro(ErroApi erro) {

                progress.setVisibility(View.GONE);
                Toast.makeText(
                        HistoricoAnoLetivoActivity.this,
                        "Erro ao carregar o histórico: " + erro.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    private void montarListaDeAnos(List<Integer> comRegistro) {

        // Do mais recente para o mais antigo
        TreeSet<Integer> conjunto = new TreeSet<>((a, b) -> b.compareTo(a));
        conjunto.addAll(comRegistro);

        if (anoAtual != null) {
            conjunto.add(anoAtual);
        }

        if (conjunto.isEmpty()) {
            conjunto.add(Calendar.getInstance().get(Calendar.YEAR));
        }

        anos.clear();
        anos.addAll(conjunto);

        List<String> rotulos = new ArrayList<>();

        for (Integer ano : anos) {
            rotulos.add(ano.equals(anoAtual) ? ano + " (atual)" : String.valueOf(ano));
        }

        ArrayAdapter<String> adapterAnos = new ArrayAdapter<>(this, R.layout.item_spinner, rotulos);
        adapterAnos.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerAno.setAdapter(adapterAnos);
        spinnerAno.setSelection(Math.max(0, anos.indexOf(anoAtual)));
        spinnerAno.setOnItemSelectedListener(new AoSelecionar(true));
    }

    // Só as turmas cadastradas; o registro de uma turma excluída fica em "Sem turma"
    private void montarListaDeTurmas(List<Turma> cadastradas) {

        TreeSet<String> nomes = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (Turma turma : cadastradas) {
            nomes.add(turma.nome);
        }

        turmas.clear();
        turmas.add("Todas as turmas");
        turmas.addAll(nomes);
        turmas.add(SEM_TURMA);

        ArrayAdapter<String> adapterTurmas = new ArrayAdapter<>(this, R.layout.item_spinner, turmas);
        adapterTurmas.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerTurma.setAdapter(adapterTurmas);
        spinnerTurma.setOnItemSelectedListener(new AoSelecionar(false));
    }

    private int anoSelecionado() {

        int posicao = Math.max(0, spinnerAno.getSelectedItemPosition());
        return anos.get(Math.min(posicao, anos.size() - 1));
    }

    private int bimestreSelecionado() {
        return Math.max(0, spinnerBimestre.getSelectedItemPosition());
    }

    // Busca no servidor o período escolhido (ano + bimestre) da aba aberta
    private void recarregar() {

        if (!anosCarregados || anos.isEmpty()) {
            return;
        }

        int ano = anoSelecionado();
        int bimestre = bimestreSelecionado();
        int aba = abaSelecionada;

        String chave = ano + "|" + bimestre + "|" + aba;

        // Já buscado (ou sendo buscado) este mesmo período
        if (chave.equals(chaveSolicitada)) {
            return;
        }

        chaveSolicitada = chave;
        periodoCarregado = false;
        int meuPedido = ++pedido;

        // Esvazia a lista até chegar o período novo, para não mostrar dados de outro filtro
        mostrar();
        progress.setVisibility(View.VISIBLE);

        HistoricoRepositorio historico = Servicos.get(this).historico;
        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            List<Linha> novas = new ArrayList<>();

            if (aba == ABA_ALUNOS) {
                for (HistoricoAluno aluno : historico.alunos(ano, bimestre)) {
                    novas.add(linhaDoAluno(aluno));
                }
            } else if (aba == ABA_TRILHAS) {
                for (HistoricoTrilha trilha : historico.trilhas(ano, bimestre)) {
                    novas.add(linhaDaTrilha(trilha));
                }
            } else {
                for (HistoricoEntrega entrega : historico.entregas(ano, bimestre)) {
                    novas.add(linhaDaEntrega(entrega));
                }
            }

            return new Periodo(novas, servicos.escola.bimestres(ano));

        }, new Tarefas.Retorno<Periodo>() {
            @Override
            public void sucesso(Periodo periodo) {

                if (meuPedido != pedido) {
                    return;
                }

                base.clear();
                base.addAll(periodo.linhas);
                bimestresDoAno = periodo.bimestres;
                periodoCarregado = true;

                progress.setVisibility(View.GONE);
                mostrar();
            }

            @Override
            public void erro(ErroApi erro) {

                if (meuPedido != pedido) {
                    return;
                }

                chaveSolicitada = null;
                progress.setVisibility(View.GONE);
                Toast.makeText(
                        HistoricoAnoLetivoActivity.this,
                        "Erro ao carregar o histórico: " + erro.getMessage(),
                        Toast.LENGTH_LONG
                ).show();
            }
        });
    }

    // =====================================================
    // LINHAS DE CADA ABA
    // =====================================================

    private static Linha linhaDoAluno(HistoricoAluno aluno) {

        return new Linha(
                aluno.nome,
                aluno.entregas + " entrega(s) • " + aluno.diasFalta + " dia(s) com falta",
                aluno.pontos + " pts",
                aluno.turmaNome,
                aluno.nome
        );
    }

    private static Linha linhaDaTrilha(HistoricoTrilha trilha) {

        return new Linha(
                trilha.nome,
                trilha.disciplinaNome + " • Prof. " + trilha.professorNome
                        + "\nPrazo: " + (trilha.prazo != null ? Bimestres.formatar(trilha.prazo) : "—")
                        + " • " + trilha.entregas + " entrega(s)",
                trilha.pontos + " pts",
                trilha.turmaNome,
                trilha.nome + " " + trilha.disciplinaNome + " " + trilha.professorNome
        );
    }

    private static Linha linhaDaEntrega(HistoricoEntrega entrega) {

        String atraso = entrega.diasAtraso > 0 ? " • " + entrega.diasAtraso + " dia(s) de atraso" : "";

        return new Linha(
                entrega.alunoNome,
                entrega.atividadeNome + " • " + entrega.disciplinaNome
                        + "\nEnviada em " + (entrega.enviadoEm != null ? DATA_ENVIO.format(entrega.enviadoEm) : "—")
                        + atraso,
                entrega.avaliada() ? formatarNota(entrega.nota) + " pts" : "Pendente",
                entrega.turmaNome,
                entrega.alunoNome + " " + entrega.atividadeNome + " " + entrega.disciplinaNome
        );
    }

    private static String formatarNota(double nota) {
        return nota == Math.floor(nota) ? String.valueOf((long) nota) : String.valueOf(nota);
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

        if (!anosCarregados || anos.isEmpty() || !periodoCarregado) {

            // Mostra a lista vazia enquanto o período novo não chega
            linhas.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        int ano = anoSelecionado();
        int bimestre = bimestreSelecionado();

        String periodo = PERIODOS[bimestre].toLowerCase();

        if (bimestre > 0) {

            for (Bimestre b : bimestresDoAno) {
                if (b.numero == bimestre) {
                    periodo += " (" + Bimestres.formatar(b.inicio) + " a " + Bimestres.formatar(b.fim) + ")";
                }
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

    // Ano e bimestre pedem uma nova consulta ao servidor; a turma só refiltra o que já veio
    private class AoSelecionar implements AdapterView.OnItemSelectedListener {

        private final boolean pedeNovaConsulta;

        AoSelecionar(boolean pedeNovaConsulta) {
            this.pedeNovaConsulta = pedeNovaConsulta;
        }

        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

            if (pedeNovaConsulta) {
                recarregar();
            } else {
                mostrar();
            }
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
        final String turma;
        final String busca;

        // Cabeçalho de turma: titulo = nome da turma, valor = quantidade
        final boolean cabecalho;

        Linha(String titulo, String subtitulo, String valor, String turma, String busca) {
            this.titulo = titulo;
            this.subtitulo = subtitulo;
            this.valor = valor;
            this.turma = turma != null && !turma.isEmpty() ? turma : SEM_TURMA;
            this.busca = busca != null ? busca.toLowerCase() : "";
            this.cabecalho = false;
        }

        Linha(String turma, int quantidade) {
            this.titulo = turma;
            this.subtitulo = null;
            this.valor = String.valueOf(quantidade);
            this.turma = turma;
            this.busca = "";
            this.cabecalho = true;
        }
    }

    private static final class Base {

        final List<Integer> anos;
        final Integer anoAtual;
        final List<Turma> turmas;

        Base(List<Integer> anos, Integer anoAtual, List<Turma> turmas) {
            this.anos = anos;
            this.anoAtual = anoAtual;
            this.turmas = turmas;
        }
    }

    private static final class Periodo {

        final List<Linha> linhas;
        final List<Bimestre> bimestres;

        Periodo(List<Linha> linhas, List<Bimestre> bimestres) {
            this.linhas = linhas;
            this.bimestres = bimestres;
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
        Map<String, List<Linha>> porTurma = new TreeMap<>((a, b) -> {
            if (a.equals(b)) return 0;
            if (a.equals(SEM_TURMA)) return 1;
            if (b.equals(SEM_TURMA)) return -1;
            return a.compareToIgnoreCase(b);
        });

        for (Linha linha : base) {

            if (turmaFiltro != null && !turmaFiltro.equalsIgnoreCase(linha.turma)) {
                continue;
            }

            if (busca.isEmpty() || linha.busca.contains(busca)) {
                porTurma.computeIfAbsent(linha.turma, k -> new ArrayList<>()).add(linha);
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
