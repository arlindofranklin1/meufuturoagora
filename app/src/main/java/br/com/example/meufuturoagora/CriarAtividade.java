package br.com.example.meufuturoagora;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.Button;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CriarAtividade extends AppCompatActivity {

    private FirebaseFirestore db;
    private FirebaseStorage storage;

    private EditText edtNomeAtividade;
    private EditText edtDescricao;
    private EditText edtPontos;
    private EditText edtPrazo;

    private Spinner spinnerDisciplina;

    private MaterialButton btnSelecionarArquivo;

    private String disciplinaId;
    private String disciplinaCor;
    private String atividadeId;

    private boolean modoEdicao = false;

    // =============================
    // ARQUIVO
    // =============================

    private Uri arquivoSelecionadoUri;
    private String nomeArquivoSelecionado;

    private ActivityResultLauncher<String> selecionarArquivoLauncher;

    // =============================
    // DISCIPLINAS
    // =============================

    private final List<String> nomesDisciplinas =
            new ArrayList<>();

    private final List<String> idsDisciplinas =
            new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_criar_atividade);

        // =============================
        // FIREBASE
        // =============================

        db = FirebaseFirestore.getInstance();

        storage = FirebaseStorage.getInstance();

        // =============================
        // RECEBER DADOS DA TELA ANTERIOR
        // =============================

        disciplinaId =
                getIntent().getStringExtra("disciplinaId");

        disciplinaCor =
                getIntent().getStringExtra("disciplinaCor");

        atividadeId =
                getIntent().getStringExtra("atividadeId");

        modoEdicao =
                atividadeId != null &&
                        !atividadeId.isEmpty();

        // =============================
        // COMPONENTES
        // =============================

        edtNomeAtividade =
                findViewById(R.id.edtNomeAtividade);

        edtDescricao =
                findViewById(R.id.edtDescricao);

        edtPontos =
                findViewById(R.id.edtPontos);

        edtPrazo =
                findViewById(R.id.edtPrazo);

        spinnerDisciplina =
                findViewById(R.id.spinnerDisciplina);

        Button btnSalvar =
                findViewById(R.id.btnSalvar);

        ImageView btnVoltar =
                findViewById(R.id.btnVoltar);

        btnSelecionarArquivo =
                findViewById(R.id.btnSelecionarArquivo);

        // =============================
        // SELECIONAR ARQUIVO
        // =============================

        selecionarArquivoLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.GetContent(),
                        uri -> {

                            if (uri != null) {

                                arquivoSelecionadoUri = uri;

                                nomeArquivoSelecionado =
                                        obterNomeArquivo(uri);

                                btnSelecionarArquivo.setText(
                                        nomeArquivoSelecionado
                                );
                            }
                        }
                );

        btnSelecionarArquivo.setOnClickListener(v -> {

            selecionarArquivoLauncher.launch("*/*");

        });

        // =============================
        // VOLTAR
        // =============================

        btnVoltar.setOnClickListener(v -> finish());

        // =============================
        // DATA
        // =============================

        edtPrazo.setOnClickListener(v ->
                abrirCalendario()
        );

        // =============================
        // CARREGAR DISCIPLINAS
        // =============================

        carregarDisciplinas();

        // =============================
        // MODO EDIÇÃO
        // =============================

        if (modoEdicao) {

            btnSalvar.setText(
                    "Salvar alterações"
            );

            carregarAtividadeParaEdicao();
        }

        // =============================
        // SALVAR
        // =============================

        btnSalvar.setOnClickListener(v ->
                salvarAtividade()
        );
    }

    // =========================================================
    // CARREGAR DISCIPLINAS
    // =========================================================

    private void carregarDisciplinas() {

        FirebaseUser usuario =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (usuario == null) {
            return;
        }

        String uidProfessor =
                usuario.getUid();

        db.collection("disciplinas")
                .whereEqualTo(
                        "professorId",
                        uidProfessor
                )
                .whereEqualTo(
                        "ativo",
                        true
                )
                .get()
                .addOnSuccessListener(querySnapshot -> {

                    nomesDisciplinas.clear();
                    idsDisciplinas.clear();

                    for (QueryDocumentSnapshot documento :
                            querySnapshot) {

                        String id =
                                documento.getId();

                        String nome =
                                documento.getString("nome");

                        if (nome != null) {

                            idsDisciplinas.add(id);

                            nomesDisciplinas.add(nome);
                        }
                    }

                    ArrayAdapter<String> adapter =
                            new ArrayAdapter<>(
                                    CriarAtividade.this,
                                    android.R.layout.simple_spinner_item,
                                    nomesDisciplinas
                            );

                    adapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item
                    );

                    spinnerDisciplina.setAdapter(adapter);

                    // =============================
                    // SELECIONAR DISCIPLINA
                    // =============================

                    if (disciplinaId != null) {

                        selecionarDisciplina(
                                disciplinaId
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            CriarAtividade.this,
                            "Erro ao carregar disciplinas.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================================================
    // CARREGAR ATIVIDADE PARA EDIÇÃO
    // =========================================================

    private void carregarAtividadeParaEdicao() {

        db.collection("atividades")
                .document(atividadeId)
                .get()
                .addOnSuccessListener(documento -> {

                    if (!documento.exists()) {

                        Toast.makeText(
                                this,
                                "Atividade não encontrada.",
                                Toast.LENGTH_SHORT
                        ).show();

                        finish();

                        return;
                    }

                    // =============================
                    // NOME
                    // =============================

                    String nome =
                            documento.getString("nome");

                    if (nome != null) {

                        edtNomeAtividade.setText(nome);
                    }

                    // =============================
                    // DESCRIÇÃO
                    // =============================

                    String descricao =
                            documento.getString("descricao");

                    if (descricao != null) {

                        edtDescricao.setText(
                                descricao
                        );
                    }

                    // =============================
                    // PONTOS
                    // =============================

                    Long pontos =
                            documento.getLong("pontos");

                    if (pontos != null) {

                        edtPontos.setText(
                                String.valueOf(pontos)
                        );
                    }

                    // =============================
                    // PRAZO
                    // =============================

                    String prazo =
                            documento.getString("prazo");

                    if (prazo != null) {

                        edtPrazo.setText(prazo);
                    }

                    // =============================
                    // DISCIPLINA
                    // =============================

                    String idDisciplina =
                            documento.getString(
                                    "disciplinaId"
                            );

                    if (idDisciplina != null) {

                        disciplinaId =
                                idDisciplina;

                        selecionarDisciplina(
                                idDisciplina
                        );
                    }

                    // =============================
                    // ARQUIVO
                    // =============================

                    String arquivoNome =
                            documento.getString(
                                    "arquivoNome"
                            );

                    if (arquivoNome != null &&
                            !arquivoNome.isEmpty()) {

                        nomeArquivoSelecionado =
                                arquivoNome;

                        btnSelecionarArquivo.setText(
                                arquivoNome
                        );
                    }

                })
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao carregar atividade.",
                            Toast.LENGTH_SHORT
                    ).show();
                });
    }

    // =========================================================
    // SELECIONAR DISCIPLINA
    // =========================================================

    private void selecionarDisciplina(
            String idDisciplina
    ) {

        for (int i = 0;
             i < idsDisciplinas.size();
             i++) {

            if (idsDisciplinas
                    .get(i)
                    .equals(idDisciplina)) {

                spinnerDisciplina.setSelection(i);

                break;
            }
        }
    }

    // =========================================================
    // CALENDÁRIO
    // =========================================================

    private void abrirCalendario() {

        Calendar calendario =
                Calendar.getInstance();

        int ano =
                calendario.get(Calendar.YEAR);

        int mes =
                calendario.get(Calendar.MONTH);

        int dia =
                calendario.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog dialog =
                new DatePickerDialog(
                        this,
                        (view,
                         anoSelecionado,
                         mesSelecionado,
                         diaSelecionado) -> {

                            String data =
                                    String.format(
                                            "%02d/%02d/%04d",
                                            diaSelecionado,
                                            mesSelecionado + 1,
                                            anoSelecionado
                                    );

                            edtPrazo.setText(data);
                        },
                        ano,
                        mes,
                        dia
                );

        dialog.show();
    }

    // =========================================================
    // OBTER NOME DO ARQUIVO
    // =========================================================

    private String obterNomeArquivo(Uri uri) {

        String resultado = null;

        if ("content".equals(uri.getScheme())) {

            Cursor cursor =
                    getContentResolver().query(
                            uri,
                            null,
                            null,
                            null,
                            null
                    );

            try {

                if (cursor != null &&
                        cursor.moveToFirst()) {

                    int indice =
                            cursor.getColumnIndex(
                                    OpenableColumns.DISPLAY_NAME
                            );

                    if (indice >= 0) {

                        resultado =
                                cursor.getString(indice);
                    }
                }

            } finally {

                if (cursor != null) {

                    cursor.close();
                }
            }
        }

        if (resultado == null) {

            resultado =
                    uri.getLastPathSegment();
        }

        return resultado;
    }

    // =========================================================
    // SALVAR ATIVIDADE
    // =========================================================

    private void salvarAtividade() {

        FirebaseUser usuario =
                FirebaseAuth.getInstance()
                        .getCurrentUser();

        if (usuario == null) {

            Toast.makeText(
                    this,
                    "Usuário não está logado.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =============================
        // PEGAR DADOS
        // =============================

        String nome =
                edtNomeAtividade
                        .getText()
                        .toString()
                        .trim();

        String descricao =
                edtDescricao
                        .getText()
                        .toString()
                        .trim();

        String pontosTexto =
                edtPontos
                        .getText()
                        .toString()
                        .trim();

        String prazo =
                edtPrazo
                        .getText()
                        .toString()
                        .trim();

        // =============================
        // VALIDAÇÕES
        // =============================

        if (nome.isEmpty()) {

            edtNomeAtividade.setError(
                    "Digite o título da atividade"
            );

            return;
        }

        if (pontosTexto.isEmpty()) {

            edtPontos.setError(
                    "Digite a pontuação"
            );

            return;
        }

        if (prazo.isEmpty()) {

            edtPrazo.setError(
                    "Selecione o prazo"
            );

            return;
        }

        if (spinnerDisciplina
                .getSelectedItemPosition() < 0 ||
                idsDisciplinas.isEmpty()) {

            Toast.makeText(
                    this,
                    "Selecione uma disciplina.",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }

        // =============================
        // DISCIPLINA
        // =============================

        int posicao =
                spinnerDisciplina
                        .getSelectedItemPosition();

        String idDisciplina =
                idsDisciplinas.get(posicao);

        String professorId =
                usuario.getUid();

        // =============================
        // PONTOS
        // =============================

        long pontos;

        try {

            pontos =
                    Long.parseLong(pontosTexto);

        } catch (NumberFormatException e) {

            edtPontos.setError(
                    "Digite uma pontuação válida"
            );

            return;
        }

        if (pontos < 1 || pontos > 10) {

            edtPontos.setError(
                    "A pontuação máxima deve ser entre 1 e 10"
            );

            return;
        }

        // =============================
        // MAPA DA ATIVIDADE
        // =============================

        Map<String, Object> atividade =
                new HashMap<>();

        atividade.put(
                "nome",
                nome
        );

        atividade.put(
                "descricao",
                descricao
        );

        atividade.put(
                "pontos",
                pontos
        );

        atividade.put(
                "prazo",
                prazo
        );

        atividade.put(
                "disciplinaId",
                idDisciplina
        );

        atividade.put(
                "professorId",
                professorId
        );

        atividade.put(
                "ativo",
                true
        );

        // =============================
        // ARQUIVO
        // =============================

        if (arquivoSelecionadoUri != null) {

            enviarArquivo(atividade);

        } else {

            // Não selecionou arquivo novo

            if (modoEdicao) {

                atualizarAtividade(
                        atividade
                );

            } else {

                criarNovaAtividade(
                        atividade
                );
            }
        }
    }

    // =========================================================
    // ENVIAR ARQUIVO
    // =========================================================

    private void enviarArquivo(
            Map<String, Object> atividade
    ) {

        String nomeArquivo =
                nomeArquivoSelecionado;

        String idDisciplina =
                (String) atividade.get(
                        "disciplinaId"
                );

        // =============================
        // LOCAL DO ARQUIVO NO STORAGE
        // =============================

        StorageReference referenciaArquivo =
                storage.getReference()
                        .child("atividades")
                        .child(idDisciplina)
                        .child(
                                System.currentTimeMillis()
                                        + "_"
                                        + nomeArquivo
                        );

        Toast.makeText(
                this,
                "Enviando arquivo...",
                Toast.LENGTH_SHORT
        ).show();

        // =============================
        // UPLOAD
        // =============================

        referenciaArquivo
                .putFile(arquivoSelecionadoUri)
                .addOnSuccessListener(
                        taskSnapshot -> {

                            // =============================
                            // PEGAR URL
                            // =============================

                            referenciaArquivo
                                    .getDownloadUrl()
                                    .addOnSuccessListener(
                                            uri -> {

                                                // =============================
                                                // SALVAR DADOS DO ARQUIVO
                                                // =============================

                                                atividade.put(
                                                        "arquivoNome",
                                                        nomeArquivo
                                                );

                                                atividade.put(
                                                        "arquivoUrl",
                                                        uri.toString()
                                                );

                                                // =============================
                                                // SALVAR ATIVIDADE
                                                // =============================

                                                if (modoEdicao) {

                                                    atualizarAtividade(
                                                            atividade
                                                    );

                                                } else {

                                                    criarNovaAtividade(
                                                            atividade
                                                    );
                                                }
                                            }
                                    )
                                    .addOnFailureListener(e -> {

                                        Toast.makeText(
                                                this,
                                                "Não foi possível obter o arquivo.",
                                                Toast.LENGTH_SHORT
                                        ).show();
                                    });
                        }
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao enviar arquivo: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // CRIAR NOVA ATIVIDADE
    // =========================================================

    private void criarNovaAtividade(
            Map<String, Object> atividade
    ) {

        db.collection("atividades")
                .add(atividade)
                .addOnSuccessListener(
                        documentReference -> {

                            Toast.makeText(
                                    this,
                                    "Atividade criada!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao criar atividade: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }

    // =========================================================
    // ATUALIZAR ATIVIDADE
    // =========================================================

    private void atualizarAtividade(
            Map<String, Object> atividade
    ) {

        db.collection("atividades")
                .document(atividadeId)
                .update(atividade)
                .addOnSuccessListener(
                        unused -> {

                            Toast.makeText(
                                    this,
                                    "Atividade atualizada!",
                                    Toast.LENGTH_SHORT
                            ).show();

                            finish();
                        }
                )
                .addOnFailureListener(e -> {

                    Toast.makeText(
                            this,
                            "Erro ao atualizar atividade: "
                                    + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();
                });
    }
}