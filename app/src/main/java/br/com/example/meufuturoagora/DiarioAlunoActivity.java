package br.com.example.meufuturoagora;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import br.com.example.meufuturoagora.dados.EntradaDiario;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// Diário pessoal do aluno: metas e sonhos que só ele vê.
// Fica em diario_entradas — o RLS libera só o próprio dono (decisão D8). Sem tempo
// real (N2): recarrega ao abrir a tela e depois de cada gravação.
public class DiarioAlunoActivity extends AppCompatActivity {

    private static final int LIMITE_TEXTO = 1000;

    private static final int[] CORES_FOLHAS = {
            R.color.folha_lilas,
            R.color.folha_rosa,
            R.color.folha_pessego,
            R.color.folha_menta,
            R.color.folha_ceu
    };

    private final DateTimeFormatter formatoData =
            DateTimeFormatter.ofPattern("dd 'de' MMMM 'de' yyyy", new Locale("pt", "BR"));


    private EditText etNovaMeta;
    private MaterialButton btnGuardarMeta;
    private LinearLayout containerMetas;
    private View layoutDiarioVazio;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_diario_aluno);

        etNovaMeta = findViewById(R.id.etNovaMeta);
        btnGuardarMeta = findViewById(R.id.btnGuardarMeta);
        containerMetas = findViewById(R.id.containerMetas);
        layoutDiarioVazio = findViewById(R.id.layoutDiarioVazio);

        animarBrilhos();

        findViewById(R.id.cardMeuFuturo).setOnClickListener(v ->
                startActivity(new Intent(this, MeuFuturoActivity.class)));

        TextView tvContadorMeta = findViewById(R.id.tvContadorMeta);

        etNovaMeta.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                tvContadorMeta.setText(s.length() + "/" + LIMITE_TEXTO);
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        btnGuardarMeta.setOnClickListener(v -> guardarNovaMeta());

        // =========================
        // BOTTOM NAVIGATION
        // =========================

        BottomNavigationView bottomNavigation = findViewById(R.id.bottomNavigation);

        InsetsUtil.aplicarInsetsBottomNav(bottomNavigation);

        bottomNavigation.setItemIconTintList(null);
        bottomNavigation.setSelectedItemId(R.id.nav_diario);

        bottomNavigation.setOnItemSelectedListener(item -> {

            int id = item.getItemId();

            if (id == R.id.nav_inicio) {

                startActivity(new Intent(this, TelaInicialAluno.class));
                return true;

            } else if (id == R.id.nav_disciplinas) {

                startActivity(new Intent(this, TelaDisciplinaAluno.class));
                return true;

            } else if (id == R.id.nav_diario) {

                return true;

            } else if (id == R.id.nav_ranking) {

                startActivity(new Intent(this, RankingActivity.class)
                        .putExtra(RankingActivity.EXTRA_PERFIL, RankingActivity.PERFIL_ALUNO));
                return true;

            } else if (id == R.id.nav_perfil) {

                startActivity(new Intent(this, TelaPerfilAluno.class));
                return true;
            }

            return false;
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

        carregarEntradas();
    }

    private static final class Carga {

        final Perfil aluno;
        final List<EntradaDiario> entradas;

        Carga(Perfil aluno, List<EntradaDiario> entradas) {
            this.aluno = aluno;
            this.entradas = entradas;
        }
    }

    private void carregarEntradas() {

        Servicos servicos = Servicos.get(this);

        Tarefas.executar(this,
                () -> new Carga(servicos.perfis.meu(), servicos.diario.entradas()),
                new Tarefas.Retorno<Carga>() {
                    @Override
                    public void sucesso(Carga carga) {

                        String nome = carga.aluno != null ? carga.aluno.nome : null;

                        if (nome != null && !nome.trim().isEmpty()) {
                            String primeiroNome = nome.trim().split(" ")[0];
                            ((TextView) findViewById(R.id.tvSaudacaoDiario))
                                    .setText("Oi, " + primeiroNome + "! Um cantinho só seu para sonhar alto.");
                        }

                        mostrarEntradas(carga.entradas);
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        Toast.makeText(DiarioAlunoActivity.this,
                                "Não foi possível carregar o diário. Tente de novo.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // =========================
    // LISTA DE PÁGINAS
    // =========================

    private void mostrarEntradas(List<EntradaDiario> entradas) {

        containerMetas.removeAllViews();

        boolean vazio = entradas == null || entradas.isEmpty();
        layoutDiarioVazio.setVisibility(vazio ? View.VISIBLE : View.GONE);

        if (vazio) {
            return;
        }

        LayoutInflater inflater = LayoutInflater.from(this);

        for (int i = 0; i < entradas.size(); i++) {

            EntradaDiario entrada = entradas.get(i);

            View folha = inflater.inflate(R.layout.item_meta_diario, containerMetas, false);

            folha.setBackgroundTintList(ColorStateList.valueOf(
                    ContextCompat.getColor(this, CORES_FOLHAS[i % CORES_FOLHAS.length])
            ));

            ((TextView) folha.findViewById(R.id.tvTextoMeta)).setText(entrada.texto);
            ((TextView) folha.findViewById(R.id.tvDataMeta)).setText(
                    entrada.criadoEm != null
                            ? formatoData.format(entrada.criadoEm.atZoneSameInstant(ZoneId.systemDefault()))
                            : "Agora mesmo"
            );

            folha.setOnClickListener(v -> editarMeta(entrada.id, entrada.texto));
            folha.findViewById(R.id.btnApagarMeta).setOnClickListener(v -> confirmarApagar(entrada.id));

            containerMetas.addView(folha);
        }
    }

    // =========================
    // CRIAR / EDITAR / APAGAR
    // =========================

    // Roda a gravação e recarrega a lista. O texto digitado só some se der certo.
    private void gravar(Tarefas.Trabalho<Object> trabalho, String falha, Runnable aoDarCerto) {

        Tarefas.executar(this, trabalho, new Tarefas.Retorno<Object>() {
            @Override
            public void sucesso(Object resultado) {

                if (aoDarCerto != null) {
                    aoDarCerto.run();
                }

                carregarEntradas();
            }

            @Override
            public void erro(ErroApi erro) {
                btnGuardarMeta.setEnabled(true);
                Toast.makeText(DiarioAlunoActivity.this, falha, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void guardarNovaMeta() {

        String texto = etNovaMeta.getText().toString().trim();

        if (texto.isEmpty()) {
            Toast.makeText(this, "Escreva algo antes de guardar ✍️", Toast.LENGTH_SHORT).show();
            return;
        }

        Servicos servicos = Servicos.get(this);

        // Evita guardar duas vezes enquanto a primeira ainda está indo
        btnGuardarMeta.setEnabled(false);

        gravar(() -> servicos.diario.escrever(texto), "Não foi possível guardar. Tente de novo.", () -> {

            btnGuardarMeta.setEnabled(true);
            etNovaMeta.setText("");
            esconderTeclado();
            Toast.makeText(this, "Guardado no seu diário ✨", Toast.LENGTH_SHORT).show();
        });
    }

    private void editarMeta(String idEntrada, String textoAtual) {

        EditText campo = new EditText(this);
        campo.setText(textoAtual);
        campo.setSelection(textoAtual.length());
        campo.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        campo.setMinLines(3);
        campo.setFilters(new android.text.InputFilter[]{
                new android.text.InputFilter.LengthFilter(LIMITE_TEXTO)
        });

        FrameLayout moldura = new FrameLayout(this);
        int margem = Math.round(20 * getResources().getDisplayMetrics().density);
        moldura.setPadding(margem, margem / 2, margem, 0);
        moldura.addView(campo);

        new MaterialAlertDialogBuilder(this)
                .setTitle("Editar página")
                .setView(moldura)
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Salvar", (dialogo, qual) -> {

                    String novoTexto = campo.getText().toString().trim();

                    if (novoTexto.isEmpty() || novoTexto.equals(textoAtual)) {
                        return;
                    }

                    Servicos servicos = Servicos.get(this);

                    gravar(() -> {
                        servicos.diario.editar(idEntrada, novoTexto);
                        return null;
                    }, "Não foi possível salvar. Tente de novo.", null);
                })
                .show();
    }

    private void confirmarApagar(String idEntrada) {

        new MaterialAlertDialogBuilder(this)
                .setTitle("Apagar esta página?")
                .setMessage("Ela vai sumir do seu diário para sempre.")
                .setNegativeButton("Cancelar", null)
                .setPositiveButton("Apagar", (dialogo, qual) -> {

                    Servicos servicos = Servicos.get(this);

                    gravar(() -> {
                        servicos.diario.apagar(idEntrada);
                        return null;
                    }, "Não foi possível apagar. Tente de novo.", null);
                })
                .show();
    }

    // =========================
    // TOQUE MÁGICO
    // =========================

    // Os brilhos do cabeçalho piscam devagar, cada um no seu ritmo
    private void animarBrilhos() {

        int[] ids = {R.id.brilho1, R.id.brilho2, R.id.brilho3, R.id.brilho4};
        long[] duracoes = {1600, 2100, 1800, 2500};

        for (int i = 0; i < ids.length; i++) {

            ObjectAnimator animacao = ObjectAnimator.ofPropertyValuesHolder(
                    findViewById(ids[i]),
                    PropertyValuesHolder.ofFloat(View.ALPHA, 0.25f, 1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_X, 0.7f, 1.1f),
                    PropertyValuesHolder.ofFloat(View.SCALE_Y, 0.7f, 1.1f)
            );

            animacao.setDuration(duracoes[i]);
            animacao.setStartDelay(i * 300L);
            animacao.setRepeatCount(ObjectAnimator.INFINITE);
            animacao.setRepeatMode(ObjectAnimator.REVERSE);
            animacao.start();
        }
    }

    private void esconderTeclado() {

        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        if (imm != null) {
            imm.hideSoftInputFromWindow(etNovaMeta.getWindowToken(), 0);
        }

        etNovaMeta.clearFocus();
    }
}
