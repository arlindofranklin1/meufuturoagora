package br.com.example.meufuturoagora;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import br.com.example.meufuturoagora.dados.Arquivos;
import br.com.example.meufuturoagora.dados.Atividade;
import br.com.example.meufuturoagora.dados.Datas;
import br.com.example.meufuturoagora.dados.Disciplina;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

public class CriarAtividade extends AppCompatActivity {

    private EditText edtNomeAtividade;
    private EditText edtDescricao;
    private EditText edtPontos;
    private EditText edtPrazo;
    private EditText edtLink;

    private Spinner spinnerDisciplina;

    private MaterialButton btnSelecionarArquivo;
    private Button btnSalvar;

    private String disciplinaId;
    private String atividadeId;

    private boolean modoEdicao = false;

    // Trilha que está sendo editada (guarda o anexo atual, que fica se não escolherem outro)
    private Atividade atividadeAtual;


    private Uri arquivoSelecionadoUri;
    private String nomeArquivoSelecionado;
    private String tipoArquivoSelecionado;

    private ActivityResultLauncher<String[]> selecionarArquivoLauncher;

    private final List<String> nomesDisciplinas = new ArrayList<>();
    private final List<String> idsDisciplinas = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_criar_atividade);

        // Título sempre na mesma altura: margem do topo conta abaixo da barra de status
        InsetsUtil.aplicarInsetsSistema(this);

        disciplinaId = getIntent().getStringExtra("disciplinaId");
        atividadeId = getIntent().getStringExtra("atividadeId");
        modoEdicao = atividadeId != null && !atividadeId.isEmpty();

        edtNomeAtividade = findViewById(R.id.edtNomeAtividade);
        edtDescricao = findViewById(R.id.edtDescricao);
        edtPontos = findViewById(R.id.edtPontos);
        edtPrazo = findViewById(R.id.edtPrazo);
        edtLink = findViewById(R.id.edtLink);
        spinnerDisciplina = findViewById(R.id.spinnerDisciplina);
        btnSalvar = findViewById(R.id.btnSalvar);
        btnSelecionarArquivo = findViewById(R.id.btnSelecionarArquivo);

        ImageView btnVoltar = findViewById(R.id.btnVoltar);
        btnVoltar.setOnClickListener(v -> finish());

        selecionarArquivoLauncher = registerForActivityResult(
                new ActivityResultContracts.OpenDocument(),
                uri -> {

                    if (uri == null) {
                        return;
                    }

                    String problema = ArquivoUtil.validar(getContentResolver(), uri);

                    if (problema != null) {

                        Toast.makeText(this, problema, Toast.LENGTH_LONG).show();
                        return;
                    }

                    arquivoSelecionadoUri = uri;
                    tipoArquivoSelecionado = getContentResolver().getType(uri);
                    nomeArquivoSelecionado = ArquivoUtil.obterNome(getContentResolver(), uri);
                    btnSelecionarArquivo.setText(nomeArquivoSelecionado);
                }
        );

        btnSelecionarArquivo.setOnClickListener(v ->
                selecionarArquivoLauncher.launch(ArquivoUtil.TIPOS_DO_SELETOR));

        edtPrazo.setOnClickListener(v -> abrirCalendario());

        if (modoEdicao) {
            btnSalvar.setText("Salvar alterações");
        }

        carregarTela();

        btnSalvar.setOnClickListener(v -> salvarAtividade());
    }

    private static final class Carga {

        final List<Disciplina> disciplinas;
        final Atividade atividade;

        Carga(List<Disciplina> disciplinas, Atividade atividade) {
            this.disciplinas = disciplinas;
            this.atividade = atividade;
        }
    }

    // Disciplinas ativas do professor e, na edição, a trilha
    private void carregarTela() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this, () -> {

            Perfil eu = servicos.perfis.meu();
            List<Disciplina> minhas = new ArrayList<>();

            for (Disciplina d : servicos.disciplinas.listar()) {

                if (d.ativo && eu.id.equals(d.professorId)) {
                    minhas.add(d);
                }
            }

            return new Carga(minhas, modoEdicao ? servicos.atividades.buscar(atividadeId) : null);

        }, new Tarefas.Retorno<Carga>() {
            @Override
            public void sucesso(Carga carga) {

                if (modoEdicao && carga.atividade == null) {

                    Toast.makeText(CriarAtividade.this, "Trilha não encontrada.", Toast.LENGTH_SHORT).show();
                    finish();
                    return;
                }

                mostrarDisciplinas(carga.disciplinas);

                if (carga.atividade != null) {
                    preencherParaEdicao(carga.atividade);
                }
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(CriarAtividade.this, "Erro ao carregar a tela: " + erro.getMessage(),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void mostrarDisciplinas(List<Disciplina> disciplinas) {

        nomesDisciplinas.clear();
        idsDisciplinas.clear();

        for (Disciplina d : disciplinas) {

            idsDisciplinas.add(d.id);
            nomesDisciplinas.add(d.nome);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.item_spinner, nomesDisciplinas);
        adapter.setDropDownViewResource(R.layout.item_spinner_dropdown);
        spinnerDisciplina.setAdapter(adapter);

        // Toda trilha precisa de uma disciplina
        if (idsDisciplinas.isEmpty()) {

            Toast.makeText(this, "Crie uma disciplina primeiro para depois criar uma trilha.",
                    Toast.LENGTH_LONG).show();

            finish();
            return;
        }

        if (disciplinaId != null) {
            selecionarDisciplina(disciplinaId);
        }
    }

    private void preencherParaEdicao(Atividade atividade) {

        atividadeAtual = atividade;

        edtNomeAtividade.setText(atividade.nome);

        if (atividade.descricao != null) {
            edtDescricao.setText(atividade.descricao);
        }

        edtPontos.setText(String.valueOf(atividade.pontos));

        if (atividade.prazo != null) {

            edtPrazo.setText(Datas.formatar(atividade.prazo));
        }

        if (atividade.link != null) {
            edtLink.setText(atividade.link);
        }

        // O anexo fica no Storage sob a disciplina: a trilha não muda de disciplina
        disciplinaId = atividade.disciplinaId;
        selecionarDisciplina(atividade.disciplinaId);
        spinnerDisciplina.setEnabled(false);

        if (atividade.arquivoNome != null && !atividade.arquivoNome.isEmpty()) {
            btnSelecionarArquivo.setText(atividade.arquivoNome);
        }
    }

    private void selecionarDisciplina(String idDisciplina) {

        int posicao = idsDisciplinas.indexOf(idDisciplina);

        if (posicao >= 0) {
            spinnerDisciplina.setSelection(posicao);
        }
    }

    private void abrirCalendario() {

        Calendar calendario = Calendar.getInstance();

        new DatePickerDialog(
                this,
                (view, ano, mes, dia) -> {

                    edtPrazo.setText(Datas.formatar(LocalDate.of(ano, mes + 1, dia)));
                },
                calendario.get(Calendar.YEAR),
                calendario.get(Calendar.MONTH),
                calendario.get(Calendar.DAY_OF_MONTH)
        ).show();
    }

    private void salvarAtividade() {

        String nome = edtNomeAtividade.getText().toString().trim();
        String descricao = edtDescricao.getText().toString().trim();
        String pontosTexto = edtPontos.getText().toString().trim();
        String link = edtLink.getText().toString().trim();

        // Só links seguros (https). Sem o início, o https:// é colocado
        // automaticamente; links http:// são recusados
        if (!link.isEmpty()) {

            String linkMinusculo = link.toLowerCase();

            if (linkMinusculo.startsWith("http://")) {

                edtLink.setError("Use um link seguro, que comece com https://");
                return;
            }

            if (!linkMinusculo.startsWith("https://")) {
                link = "https://" + link;
            }

            if (link.contains(" ") || !android.util.Patterns.WEB_URL.matcher(link).matches()) {

                edtLink.setError("Digite um link válido (ex: https://site.com)");
                return;
            }
        }

        if (nome.isEmpty()) {

            edtNomeAtividade.setError("Digite o título da trilha");
            return;
        }

        if (pontosTexto.isEmpty()) {

            edtPontos.setError("Digite a pontuação");
            return;
        }

        LocalDate prazo;

        try {
            prazo = Datas.ler(edtPrazo.getText().toString());
        } catch (IllegalArgumentException e) {

            edtPrazo.setError("Data inválida");
            return;
        }

        if (prazo == null) {

            edtPrazo.setError("Selecione o prazo");
            return;
        }

        if (spinnerDisciplina.getSelectedItemPosition() < 0 || idsDisciplinas.isEmpty()) {

            Toast.makeText(this, "Selecione uma disciplina.", Toast.LENGTH_SHORT).show();
            return;
        }

        int pontos;

        try {
            pontos = Integer.parseInt(pontosTexto);
        } catch (NumberFormatException e) {

            edtPontos.setError("Digite uma pontuação válida");
            return;
        }

        if (pontos < 1 || pontos > 10) {

            edtPontos.setError("A pontuação máxima deve ser entre 1 e 10");
            return;
        }

        String idDisciplina = idsDisciplinas.get(spinnerDisciplina.getSelectedItemPosition());

        gravar(idDisciplina, nome, descricao, pontos, prazo, link.isEmpty() ? null : link);
    }

    // Envia o anexo (se houver) e grava a trilha. Se a gravação falhar, o anexo
    // novo é apagado; se der certo na edição, o anexo antigo é que sai.
    private void gravar(String idDisciplina, String nome, String descricao, int pontos,
                        LocalDate prazoFinal, String link) {

        btnSalvar.setEnabled(false);

        Servicos servicos = Servicos.get(this);
        Uri uri = arquivoSelecionadoUri;
        String nomeArquivo = nomeArquivoSelecionado;
        String tipo = tipoArquivoSelecionado;
        Atividade anterior = atividadeAtual;
        boolean edicao = modoEdicao;

        if (uri != null) {
            Toast.makeText(this, "Enviando arquivo...", Toast.LENGTH_SHORT).show();
        }

        Tarefas.executar(this, () -> {

            String id = edicao ? atividadeId : UUID.randomUUID().toString();
            String caminhoNovo = null;

            if (uri != null) {

                byte[] conteudo;

                try {
                    conteudo = ArquivoUtil.ler(getContentResolver(), uri);
                } catch (IOException e) {
                    throw new IllegalStateException(e);
                }

                caminhoNovo = Arquivos.caminhoAtividade(idDisciplina, id, tipo);
                servicos.arquivos.enviar(Arquivos.BUCKET_ATIVIDADES, caminhoNovo, conteudo, tipo);
            }

            // Sem arquivo novo, a edição mantém o anexo que já existia
            String caminho = caminhoNovo != null ? caminhoNovo : (anterior != null ? anterior.arquivoPath : null);
            String nomeFinal = caminhoNovo != null ? nomeArquivo : (anterior != null ? anterior.arquivoNome : null);

            try {

                if (edicao) {
                    servicos.atividades.atualizar(id, nome, descricao, pontos, prazoFinal, link, caminho, nomeFinal);
                } else {
                    servicos.atividades.criar(id, idDisciplina, nome, descricao, pontos, prazoFinal, link,
                            caminho, nomeFinal);
                }

            } catch (ErroApi e) {

                apagarSilenciosamente(servicos, caminhoNovo);
                throw e;
            }

            if (caminhoNovo != null && anterior != null) {
                apagarSilenciosamente(servicos, anterior.arquivoPath);
            }

            return null;

        }, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {

                Toast.makeText(CriarAtividade.this, edicao ? "Trilha atualizada!" : "Trilha criada!",
                        Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void erro(ErroApi erro) {

                btnSalvar.setEnabled(true);

                Toast.makeText(CriarAtividade.this,
                        (edicao ? "Erro ao atualizar trilha: " : "Erro ao criar trilha: ") + erro.getMessage(),
                        Toast.LENGTH_LONG).show();
            }
        });
    }

    // Arquivo que sobrou só ocupa espaço: se não der para apagar, segue
    private static void apagarSilenciosamente(Servicos servicos, String caminho) {

        if (caminho == null) {
            return;
        }

        try {
            servicos.arquivos.apagar(Arquivos.BUCKET_ATIVIDADES, Collections.singletonList(caminho));
        } catch (ErroApi ignorado) {
            // segue
        }
    }
}
