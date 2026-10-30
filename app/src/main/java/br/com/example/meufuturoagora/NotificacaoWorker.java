package br.com.example.meufuturoagora;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Novidade;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;

// Pergunta ao servidor o que mudou desde a última verificação e mostra notificações:
// - Aluno: novas disciplinas, novas atividades dessas disciplinas e entregas avaliadas
// - Professor: novas entregas de alunos nas atividades dele
// O servidor já devolve só o que o usuário pode ver (função novidades_desde).
// Cada notificação abre a tela do conteúdo correspondente.
public class NotificacaoWorker extends Worker {

    private static final String CHAVE_ULTIMA_VERIFICACAO = "notif_ultima_verificacao";

    private static final String TAG = "NotificacaoWorker";

    public NotificacaoWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {

        Context context = getApplicationContext();
        SharedPreferences preferencias = NotificacaoUtil.preferencias(context);
        Servicos servicos = Servicos.get(context);

        try {

            // Roda mesmo com o switch desligado: a lista do sino continua sendo preenchida
            Perfil perfil = servicos.auth.restaurarSessao();

            if (perfil == null) {
                return Result.success();
            }

            // Marco separado por usuário, caso outra conta entre no mesmo celular
            String chave = CHAVE_ULTIMA_VERIFICACAO + "_" + perfil.usuarioId;
            OffsetDateTime ultima = lerMarco(preferencias, chave);

            // Primeira vez neste usuário: só marca o ponto de partida, sem avisar o passado
            if (ultima == null) {

                guardarMarco(preferencias, chave, OffsetDateTime.now());
                return Result.success();
            }

            List<Novidade> novidades = servicos.novidades.desde(ultima);
            Log.d(TAG, "Novidades: " + novidades.size());

            OffsetDateTime maisRecente = ultima;

            // O servidor devolve da mais antiga para a mais nova: a lista do sino fica em ordem
            for (Novidade novidade : novidades) {

                NotificacaoHistorico.Item item = montarItem(novidade);

                if (item != null) {
                    NotificacaoUtil.mostrar(context, item);
                }

                if (novidade.momento != null && novidade.momento.isAfter(maisRecente)) {
                    maisRecente = novidade.momento;
                }
            }

            guardarMarco(preferencias, chave, maisRecente);

            return Result.success();

        } catch (ErroApi e) {

            // Sessão que acabou: não adianta tentar de novo até a pessoa entrar
            if (e.codigoHttp() == 401) {
                return Result.success();
            }

            Log.e(TAG, "Erro ao verificar novidades", e);
            return Result.retry();

        } catch (RuntimeException e) {

            Log.e(TAG, "Erro ao verificar novidades", e);
            return Result.retry();
        }
    }

    // Texto e destino de cada tipo de novidade; null para um tipo que o app não conhece
    static NotificacaoHistorico.Item montarItem(Novidade novidade) {

        String nome = texto(novidade.nome, "");
        String disciplina = texto(novidade.disciplinaNome, "sua disciplina");

        if (Novidade.DISCIPLINA.equals(novidade.tipo)) {

            String nomeDisciplina = texto(novidade.nome, "Disciplina");

            return new NotificacaoHistorico.Item(
                    "Nova disciplina",
                    "Foi adicionada a disciplina " + nomeDisciplina,
                    NotificacaoHistorico.TIPO_DISCIPLINA
            )
                    .extra("disciplinaId", novidade.disciplinaId)
                    .extra("disciplinaNome", nomeDisciplina);
        }

        if (Novidade.ATIVIDADE.equals(novidade.tipo)) {

            String nomeAtividade = texto(novidade.nome, "Nova atividade");

            return new NotificacaoHistorico.Item(
                    "Nova atividade em " + disciplina,
                    nomeAtividade + " foi adicionada pelo professor",
                    NotificacaoHistorico.TIPO_ATIVIDADE
            )
                    .extra("atividadeId", novidade.atividadeId)
                    .extra("atividadeNome", nomeAtividade);
        }

        if (Novidade.AVALIACAO.equals(novidade.tipo)) {

            String nomeAtividade = texto(novidade.nome, "Atividade");

            return new NotificacaoHistorico.Item(
                    "Atividade avaliada em " + disciplina,
                    "O professor avaliou " + nomeAtividade + ".",
                    NotificacaoHistorico.TIPO_ATIVIDADE
            )
                    .extra("atividadeId", novidade.atividadeId)
                    .extra("atividadeNome", nomeAtividade);
        }

        if (Novidade.ENTREGA.equals(novidade.tipo)) {

            String aluno = texto(novidade.alunoNome, "Um aluno");

            return new NotificacaoHistorico.Item(
                    "Nova entrega em " + disciplina,
                    aluno + " enviou a atividade " + nome,
                    NotificacaoHistorico.TIPO_ENTREGA
            )
                    .extra("atividadeId", novidade.atividadeId)
                    .extra("entregaId", novidade.itemId)
                    .extra("alunoNome", aluno);
        }

        return null;
    }

    private static OffsetDateTime lerMarco(SharedPreferences preferencias, String chave) {

        String texto = preferencias.getString(chave, null);

        if (texto == null) {
            return null;
        }

        try {
            return OffsetDateTime.parse(texto);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static void guardarMarco(SharedPreferences preferencias, String chave, OffsetDateTime marco) {
        preferencias.edit().putString(chave, marco.toString()).apply();
    }

    private static String texto(String valor, String padrao) {

        return valor != null && !valor.isEmpty() ? valor : padrao;
    }
}
