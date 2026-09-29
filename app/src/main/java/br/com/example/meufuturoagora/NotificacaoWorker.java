package br.com.example.meufuturoagora;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.HashMap;
import java.util.Map;

// Verifica no Firestore o que mudou desde a última verificação e mostra notificações:
// - Aluno: novas disciplinas da turma, novas atividades dessas disciplinas e entregas avaliadas
// - Professor: novas entregas de alunos nas atividades dele
// Cada notificação abre a tela do conteúdo correspondente.
public class NotificacaoWorker extends Worker {

    private static final String CHAVE_ULTIMA_DISCIPLINA = "notif_ultima_disciplina";
    private static final String CHAVE_ULTIMA_ATIVIDADE = "notif_ultima_atividade";
    private static final String CHAVE_ULTIMA_AVALIACAO = "notif_ultima_avaliacao";
    private static final String CHAVE_ULTIMA_ENTREGA = "notif_ultima_entrega";

    private static final String TAG = "NotificacaoWorker";

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private SharedPreferences preferencias;
    private String uidUsuario;

    public NotificacaoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context context = getApplicationContext();
        preferencias = NotificacaoUtil.preferencias(context);

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        // Roda mesmo com o switch desligado: a lista do sino continua sendo preenchida
        if (usuario == null) {
            return Result.success();
        }

        uidUsuario = usuario.getUid();

        try {

            String perfil = preferencias.getString(NotificacaoUtil.CHAVE_PERFIL, null);
            Log.d(TAG, "Verificando novidades para o perfil: " + perfil);

            if (NotificacaoUtil.PERFIL_ALUNO.equals(perfil)) {

                verificarAluno(context, usuario.getUid());

            } else if (NotificacaoUtil.PERFIL_PROFESSOR.equals(perfil)) {

                verificarEntregas(context, usuario.getUid());
            }

            return Result.success();

        } catch (Exception e) {

            Log.e(TAG, "Erro ao verificar novidades", e);
            return Result.retry();
        }
    }

    // =========================
    // ALUNO
    // =========================

    private void verificarAluno(Context context, String alunoId) throws Exception {

        DocumentSnapshot aluno = Tasks.await(db.collection("alunos").document(alunoId).get());
        String turmaId = aluno.getString("turmaId");

        if (turmaId == null) {
            return;
        }

        // Disciplinas da turma do aluno (id -> nome)
        QuerySnapshot disciplinasTurma = Tasks.await(
                db.collection("disciplinas")
                        .whereEqualTo("turmaId", turmaId)
                        .get()
        );

        Map<String, String> nomesDisciplinas = new HashMap<>();

        for (DocumentSnapshot disciplina : disciplinasTurma) {
            nomesDisciplinas.put(disciplina.getId(), disciplina.getString("nome"));
        }

        verificarDisciplinas(context, disciplinasTurma);
        verificarAtividades(context, nomesDisciplinas);
        verificarAvaliacoes(context, alunoId);
    }

    private void verificarDisciplinas(Context context, QuerySnapshot disciplinasTurma) {

        Timestamp ultima = lerMarco(CHAVE_ULTIMA_DISCIPLINA);

        if (ultima == null) {

            salvarMarco(CHAVE_ULTIMA_DISCIPLINA, Timestamp.now());
            return;
        }

        Timestamp maisRecente = ultima;

        for (DocumentSnapshot disciplina : disciplinasTurma) {

            Timestamp criadoEm = disciplina.getTimestamp("criadoEm");

            if (criadoEm == null || criadoEm.compareTo(ultima) <= 0) {
                continue;
            }

            if (criadoEm.compareTo(maisRecente) > 0) {
                maisRecente = criadoEm;
            }

            if (Boolean.FALSE.equals(disciplina.getBoolean("ativo"))) {
                continue;
            }

            String nome = texto(disciplina.getString("nome"), "Disciplina");

            NotificacaoUtil.mostrar(context, new NotificacaoHistorico.Item(
                    "Nova disciplina",
                    "Foi adicionada a disciplina " + nome,
                    NotificacaoHistorico.TIPO_DISCIPLINA
            )
                    .extra("disciplinaId", disciplina.getId())
                    .extra("disciplinaNome", nome));
        }

        salvarMarco(CHAVE_ULTIMA_DISCIPLINA, maisRecente);
    }

    private void verificarAtividades(Context context, Map<String, String> nomesDisciplinas)
            throws Exception {

        Timestamp ultima = lerMarco(CHAVE_ULTIMA_ATIVIDADE);

        if (ultima == null) {

            salvarMarco(CHAVE_ULTIMA_ATIVIDADE, Timestamp.now());
            return;
        }

        QuerySnapshot novas = Tasks.await(
                db.collection("atividades")
                        .whereGreaterThan("criadoEm", ultima)
                        .get()
        );

        Log.d(TAG, "Atividades novas: " + novas.size());

        Timestamp maisRecente = ultima;

        for (DocumentSnapshot atividade : novas) {

            Timestamp criadoEm = atividade.getTimestamp("criadoEm");

            if (criadoEm != null && criadoEm.compareTo(maisRecente) > 0) {
                maisRecente = criadoEm;
            }

            String disciplinaId = atividade.getString("disciplinaId");

            if (disciplinaId == null || !nomesDisciplinas.containsKey(disciplinaId)) {
                continue;
            }

            String nome = texto(atividade.getString("nome"), "Nova atividade");

            NotificacaoUtil.mostrar(context, new NotificacaoHistorico.Item(
                    "Nova atividade em " + texto(nomesDisciplinas.get(disciplinaId), "sua disciplina"),
                    nome + " foi adicionada pelo professor",
                    NotificacaoHistorico.TIPO_ATIVIDADE
            )
                    .extra("atividadeId", atividade.getId())
                    .extra("atividadeNome", nome));
        }

        salvarMarco(CHAVE_ULTIMA_ATIVIDADE, maisRecente);
    }

    private void verificarAvaliacoes(Context context, String alunoId) throws Exception {

        Timestamp ultima = lerMarco(CHAVE_ULTIMA_AVALIACAO);

        if (ultima == null) {

            salvarMarco(CHAVE_ULTIMA_AVALIACAO, Timestamp.now());
            return;
        }

        QuerySnapshot avaliadas = Tasks.await(
                db.collection("entregas")
                        .whereGreaterThan("avaliadoEm", ultima)
                        .get()
        );

        Log.d(TAG, "Entregas avaliadas: " + avaliadas.size());

        Timestamp maisRecente = ultima;

        for (DocumentSnapshot entrega : avaliadas) {

            Timestamp avaliadoEm = entrega.getTimestamp("avaliadoEm");

            if (avaliadoEm != null && avaliadoEm.compareTo(maisRecente) > 0) {
                maisRecente = avaliadoEm;
            }

            String atividadeId = entrega.getString("atividadeId");

            if (!alunoId.equals(entrega.getString("alunoId")) || atividadeId == null) {
                continue;
            }

            DocumentSnapshot atividade = Tasks.await(
                    db.collection("atividades").document(atividadeId).get()
            );

            String nome = texto(atividade.getString("nome"), "Atividade");
            String disciplinaNome = nomeDisciplina(atividade.getString("disciplinaId"));

            Double nota = entrega.getDouble("nota");
            String textoNota = nota != null
                    ? " Nota: " + (nota % 1 == 0 ? String.valueOf(nota.intValue()) : String.valueOf(nota))
                    : "";

            NotificacaoUtil.mostrar(context, new NotificacaoHistorico.Item(
                    "Atividade avaliada em " + texto(disciplinaNome, "sua disciplina"),
                    "O professor avaliou " + nome + "." + textoNota,
                    NotificacaoHistorico.TIPO_ATIVIDADE
            )
                    .extra("atividadeId", atividadeId)
                    .extra("atividadeNome", nome));
        }

        salvarMarco(CHAVE_ULTIMA_AVALIACAO, maisRecente);
    }

    // =========================
    // PROFESSOR
    // =========================

    private void verificarEntregas(Context context, String professorId) throws Exception {

        Timestamp ultima = lerMarco(CHAVE_ULTIMA_ENTREGA);

        if (ultima == null) {

            salvarMarco(CHAVE_ULTIMA_ENTREGA, Timestamp.now());
            return;
        }

        QuerySnapshot novas = Tasks.await(
                db.collection("entregas")
                        .whereGreaterThan("enviadoEm", ultima)
                        .get()
        );

        Log.d(TAG, "Entregas novas: " + novas.size());

        Timestamp maisRecente = ultima;

        for (DocumentSnapshot entrega : novas) {

            Timestamp enviadoEm = entrega.getTimestamp("enviadoEm");

            if (enviadoEm != null && enviadoEm.compareTo(maisRecente) > 0) {
                maisRecente = enviadoEm;
            }

            String atividadeId = entrega.getString("atividadeId");
            String alunoId = entrega.getString("alunoId");

            if (atividadeId == null || alunoId == null) {
                continue;
            }

            DocumentSnapshot atividade = Tasks.await(
                    db.collection("atividades").document(atividadeId).get()
            );

            if (!professorId.equals(atividade.getString("professorId"))) {
                continue;
            }

            String disciplinaNome = nomeDisciplina(atividade.getString("disciplinaId"));

            String alunoNome = texto(Tasks.await(
                    db.collection("alunos").document(alunoId).get()
            ).getString("nome"), "Um aluno");

            NotificacaoUtil.mostrar(context, new NotificacaoHistorico.Item(
                    "Nova entrega em " + texto(disciplinaNome, "sua disciplina"),
                    alunoNome + " enviou a atividade " + texto(atividade.getString("nome"), ""),
                    NotificacaoHistorico.TIPO_ENTREGA
            )
                    .extra("atividadeId", atividadeId)
                    .extra("alunoId", alunoId)
                    .extra("alunoNome", alunoNome));
        }

        salvarMarco(CHAVE_ULTIMA_ENTREGA, maisRecente);
    }

    // =========================
    // AUXILIARES
    // =========================

    private String nomeDisciplina(String disciplinaId) throws Exception {

        if (disciplinaId == null) {
            return null;
        }

        return Tasks.await(
                db.collection("disciplinas").document(disciplinaId).get()
        ).getString("nome");
    }

    private Timestamp lerMarco(String chave) {

        chave = chave + "_" + uidUsuario;

        long segundos = preferencias.getLong(chave + "_s", -1);

        if (segundos < 0) {
            return null;
        }

        return new Timestamp(segundos, preferencias.getInt(chave + "_n", 0));
    }

    // Guarda segundos e nanossegundos para não notificar o mesmo item duas vezes
    private void salvarMarco(String chave, Timestamp marco) {

        // Marco separado por usuário, caso outra conta entre no mesmo celular
        chave = chave + "_" + uidUsuario;

        preferencias.edit()
                .putLong(chave + "_s", marco.getSeconds())
                .putInt(chave + "_n", marco.getNanoseconds())
                .apply();
    }

    private static String texto(String valor, String padrao) {

        return valor != null && !valor.isEmpty() ? valor : padrao;
    }
}
