package br.com.example.meufuturoagora;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;

import androidx.activity.ComponentActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import java.util.concurrent.TimeUnit;

class NotificacaoUtil {

    static final String PREFS_NOME = "preferencias_app";
    static final String CHAVE_NOTIFICACOES = "notificacoes_ativas";
    static final String CHAVE_PERFIL = "perfil_usuario";

    static final String PERFIL_ALUNO = "aluno";
    static final String PERFIL_PROFESSOR = "professor";

    private static final String CANAL_ID = "novidades";
    private static final String TRABALHO_PERIODICO = "verificar_notificacoes";
    private static final String TRABALHO_IMEDIATO = "verificar_notificacoes_agora";

    private static final int CODIGO_PERMISSAO = 1001;

    private NotificacaoUtil() {
    }

    static SharedPreferences preferencias(Context context) {
        return context.getSharedPreferences(PREFS_NOME, Context.MODE_PRIVATE);
    }

    static boolean notificacoesAtivas(Context context) {
        return preferencias(context).getBoolean(CHAVE_NOTIFICACOES, true);
    }

    // Chamado nas telas iniciais do aluno e do professor
    static void iniciar(ComponentActivity activity, String perfil) {

        preferencias(activity).edit().putString(CHAVE_PERFIL, perfil).apply();

        // A verificação roda sempre para alimentar a lista do sino;
        // o switch só decide se aparece notificação na barra do celular
        if (notificacoesAtivas(activity)) {
            pedirPermissao(activity);
        }

        agendar(activity);
    }

    static void pedirPermissao(ComponentActivity activity) {

        if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {

            activity.requestPermissions(
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    CODIGO_PERMISSAO
            );
        }
    }

    // Verifica agora e depois a cada 15 minutos (menor intervalo permitido pelo Android)
    static void agendar(Context context) {

        WorkManager workManager = WorkManager.getInstance(context);

        workManager.enqueueUniquePeriodicWork(
                TRABALHO_PERIODICO,
                ExistingPeriodicWorkPolicy.KEEP,
                new PeriodicWorkRequest.Builder(NotificacaoWorker.class, 15, TimeUnit.MINUTES).build()
        );

        workManager.enqueueUniqueWork(
                TRABALHO_IMEDIATO,
                ExistingWorkPolicy.REPLACE,
                new OneTimeWorkRequest.Builder(NotificacaoWorker.class).build()
        );
    }

    static void cancelar(Context context) {

        WorkManager workManager = WorkManager.getInstance(context);

        workManager.cancelUniqueWork(TRABALHO_PERIODICO);
        workManager.cancelUniqueWork(TRABALHO_IMEDIATO);
    }

    // Guarda na lista do sino e, se o switch estiver ligado, mostra na barra do celular
    static void mostrar(Context context, NotificacaoHistorico.Item item) {

        NotificacaoHistorico.adicionar(context, item);

        if (!notificacoesAtivas(context)
                || ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        String titulo = item.titulo;
        String mensagem = item.mensagem;
        Intent destino = item.destino(context);
        Class<?> telaInicial = item.telaInicial();

        NotificationManager manager = context.getSystemService(NotificationManager.class);

        manager.createNotificationChannel(new NotificationChannel(
                CANAL_ID,
                "Novidades",
                NotificationManager.IMPORTANCE_DEFAULT
        ));

        int id = (int) System.nanoTime();

        // Abre direto o conteúdo, com a tela inicial por baixo para o botão voltar
        PendingIntent pendingIntent = TaskStackBuilder.create(context)
                .addNextIntent(new Intent(context, telaInicial))
                .addNextIntent(destino)
                .getPendingIntent(
                        id,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CANAL_ID)
                .setSmallIcon(R.drawable.ic_notificacao_app)
                .setLargeIcon(iconeDoApp(context))
                .setColor(ContextCompat.getColor(context, R.color.roxo_primario))
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensagem))
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context)
                .notify(id, builder.build());
    }

    // Ícone do app (o mesmo da tela inicial do celular) em formato de imagem
    private static Bitmap iconeDoApp(Context context) {

        Drawable icone = ContextCompat.getDrawable(context, R.mipmap.ic_launcher);

        if (icone == null) {
            return null;
        }

        int tamanho = context.getResources()
                .getDimensionPixelSize(android.R.dimen.notification_large_icon_width);

        Bitmap bitmap = Bitmap.createBitmap(tamanho, tamanho, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);

        icone.setBounds(0, 0, tamanho, tamanho);
        icone.draw(canvas);

        return bitmap;
    }
}
