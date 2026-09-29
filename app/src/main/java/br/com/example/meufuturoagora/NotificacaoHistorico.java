package br.com.example.meufuturoagora;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.DefaultLifecycleObserver;
import androidx.lifecycle.LifecycleOwner;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// Lista de notificações que aparece ao tocar no sino das telas iniciais.
// Fica salva no celular, separada por usuário (aluno vê as dele, professor as dele).
class NotificacaoHistorico {

    static final String TIPO_DISCIPLINA = "disciplina";
    static final String TIPO_ATIVIDADE = "atividade";
    static final String TIPO_ENTREGA = "entrega";

    private static final String PREFS_NOME = "notificacoes_historico";
    private static final int LIMITE = 50;

    private NotificacaoHistorico() {
    }

    static class Item {

        String titulo;
        String mensagem;
        long tempo;
        boolean lida;
        String tipo;
        final Map<String, String> extras = new HashMap<>();

        Item(String titulo, String mensagem, String tipo) {
            this.titulo = titulo;
            this.mensagem = mensagem;
            this.tipo = tipo;
            this.tempo = System.currentTimeMillis();
        }

        Item extra(String chave, String valor) {
            extras.put(chave, valor);
            return this;
        }

        // Tela que abre ao tocar na notificação
        Intent destino(Context context) {

            Class<?> tela;

            if (TIPO_DISCIPLINA.equals(tipo)) {
                tela = TelaDetalhesDisciplinaAluno.class;
            } else if (TIPO_ENTREGA.equals(tipo)) {
                tela = AvaliarEntregaActivity.class;
            } else {
                tela = TelaAtividadeAluno.class;
            }

            Intent intent = new Intent(context, tela);

            for (Map.Entry<String, String> extra : extras.entrySet()) {
                intent.putExtra(extra.getKey(), extra.getValue());
            }

            return intent;
        }

        // Tela para onde o "voltar" leva depois de abrir o destino
        Class<?> telaInicial() {
            return TIPO_ENTREGA.equals(tipo) ? TelaInicialProfessor.class : TelaInicialAluno.class;
        }
    }

    // =========================
    // LEITURA E ESCRITA
    // =========================

    static synchronized void adicionar(Context context, Item item) {

        List<Item> itens = listar(context);
        itens.add(0, item);

        while (itens.size() > LIMITE) {
            itens.remove(itens.size() - 1);
        }

        salvar(context, itens);
    }

    static synchronized List<Item> listar(Context context) {

        List<Item> itens = new ArrayList<>();
        String chave = chaveUsuario();

        if (chave == null) {
            return itens;
        }

        try {

            JSONArray array = new JSONArray(prefs(context).getString(chave, "[]"));

            for (int i = 0; i < array.length(); i++) {

                JSONObject objeto = array.getJSONObject(i);

                Item item = new Item(
                        objeto.optString("titulo"),
                        objeto.optString("mensagem"),
                        objeto.optString("tipo")
                );
                item.tempo = objeto.optLong("tempo");
                item.lida = objeto.optBoolean("lida");

                JSONObject extras = objeto.optJSONObject("extras");

                if (extras != null) {

                    Iterator<String> chaves = extras.keys();

                    while (chaves.hasNext()) {
                        String k = chaves.next();
                        item.extras.put(k, extras.optString(k));
                    }
                }

                itens.add(item);
            }

        } catch (Exception ignored) {
            // Histórico corrompido: começa vazio
        }

        return itens;
    }

    static boolean temNaoLidas(Context context) {

        for (Item item : listar(context)) {

            if (!item.lida) {
                return true;
            }
        }

        return false;
    }

    static synchronized void marcarTodasComoLidas(Context context) {

        List<Item> itens = listar(context);

        for (Item item : itens) {
            item.lida = true;
        }

        salvar(context, itens);
    }

    static synchronized void limpar(Context context) {

        String chave = chaveUsuario();

        if (chave != null) {
            prefs(context).edit().remove(chave).apply();
        }
    }

    private static void salvar(Context context, List<Item> itens) {

        String chave = chaveUsuario();

        if (chave == null) {
            return;
        }

        try {

            JSONArray array = new JSONArray();

            for (Item item : itens) {

                JSONObject objeto = new JSONObject();
                objeto.put("titulo", item.titulo);
                objeto.put("mensagem", item.mensagem);
                objeto.put("tempo", item.tempo);
                objeto.put("lida", item.lida);
                objeto.put("tipo", item.tipo);
                objeto.put("extras", new JSONObject(item.extras));

                array.put(objeto);
            }

            prefs(context).edit().putString(chave, array.toString()).apply();

        } catch (Exception ignored) {
        }
    }

    // =========================
    // BOLINHA DO SINO
    // =========================

    // Mostra/esconde a bolinha laranja e atualiza sozinha quando chega notificação nova
    static void vincularBolinha(AppCompatActivity activity, View bolinha) {

        SharedPreferences preferencias = prefs(activity);

        SharedPreferences.OnSharedPreferenceChangeListener ouvinte =
                (sp, chave) -> atualizarBolinha(activity, bolinha);

        activity.getLifecycle().addObserver(new DefaultLifecycleObserver() {

            @Override
            public void onResume(@NonNull LifecycleOwner owner) {
                preferencias.registerOnSharedPreferenceChangeListener(ouvinte);
                atualizarBolinha(activity, bolinha);
            }

            @Override
            public void onPause(@NonNull LifecycleOwner owner) {
                preferencias.unregisterOnSharedPreferenceChangeListener(ouvinte);
            }
        });
    }

    static void atualizarBolinha(Context context, View bolinha) {
        bolinha.setVisibility(temNaoLidas(context) ? View.VISIBLE : View.GONE);
    }

    // =========================
    // AUXILIARES
    // =========================

    private static SharedPreferences prefs(Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_NOME, Context.MODE_PRIVATE);
    }

    private static String chaveUsuario() {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        return usuario != null ? "historico_" + usuario.getUid() : null;
    }
}
