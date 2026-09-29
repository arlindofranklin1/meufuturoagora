package br.com.example.meufuturoagora;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.text.format.DateUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

// Diálogo aberto pelo sino das telas iniciais do aluno e do professor
class NotificacoesDialog {

    private NotificacoesDialog() {
    }

    static void mostrar(AppCompatActivity activity, View bolinhaDoSino) {

        Dialog dialog = new Dialog(activity);
        dialog.setContentView(R.layout.dialog_notificacoes);

        Window janela = dialog.getWindow();

        if (janela != null) {

            // Fundo transparente para as bordas arredondadas aparecerem
            janela.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

            int largura = (int) (activity.getResources().getDisplayMetrics().widthPixels * 0.9f);
            janela.setLayout(largura, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        List<NotificacaoHistorico.Item> itens = NotificacaoHistorico.listar(activity);

        RecyclerView recycler = dialog.findViewById(R.id.recyclerNotificacoes);
        TextView tvVazio = dialog.findViewById(R.id.tvSemNotificacoes);

        recycler.setLayoutManager(new LinearLayoutManager(activity));
        recycler.setAdapter(new Adapter(activity, dialog, itens));

        Runnable atualizarVazio = () -> {
            tvVazio.setVisibility(itens.isEmpty() ? View.VISIBLE : View.GONE);
            recycler.setVisibility(itens.isEmpty() ? View.GONE : View.VISIBLE);
        };
        atualizarVazio.run();

        // Lixeira: esvazia a lista (com confirmação)
        dialog.findViewById(R.id.btnLimparNotificacoes).setOnClickListener(v -> {

            if (itens.isEmpty()) {
                return;
            }

            new AlertDialog.Builder(activity)
                    .setTitle("Esvaziar notificações")
                    .setMessage("Deseja apagar todas as notificações da lista?")
                    .setNegativeButton("Cancelar", null)
                    .setPositiveButton("Esvaziar", (d, w) -> {

                        NotificacaoHistorico.limpar(activity);
                        itens.clear();

                        if (recycler.getAdapter() != null) {
                            recycler.getAdapter().notifyDataSetChanged();
                        }

                        atualizarVazio.run();
                    })
                    .show();
        });

        dialog.findViewById(R.id.btnFecharNotificacoes).setOnClickListener(v -> dialog.dismiss());

        dialog.show();

        // Abriu o diálogo = viu as notificações: some a bolinha do sino.
        // Os itens continuam destacados nesta abertura para saber quais eram novos.
        NotificacaoHistorico.marcarTodasComoLidas(activity);
        NotificacaoHistorico.atualizarBolinha(activity, bolinhaDoSino);
    }

    private static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

        private final AppCompatActivity activity;
        private final Dialog dialog;
        private final List<NotificacaoHistorico.Item> itens;

        Adapter(AppCompatActivity activity, Dialog dialog, List<NotificacaoHistorico.Item> itens) {
            this.activity = activity;
            this.dialog = dialog;
            this.itens = itens;
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            return new ViewHolder(activity.getLayoutInflater().inflate(
                    R.layout.item_notificacao, parent, false
            ));
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {

            NotificacaoHistorico.Item item = itens.get(position);

            holder.tvTitulo.setText(item.titulo);
            holder.tvMensagem.setText(item.mensagem);
            holder.tvTempo.setText(DateUtils.getRelativeTimeSpanString(
                    item.tempo, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS
            ));

            holder.bolinha.setVisibility(item.lida ? View.GONE : View.VISIBLE);

            if (item.lida) {
                holder.itemView.setBackground(null);
            } else {
                holder.itemView.setBackgroundResource(R.drawable.bg_item_notificacao_nao_lida);
            }

            // Toque abre o conteúdo da notificação
            holder.itemView.setOnClickListener(v -> {
                dialog.dismiss();
                activity.startActivity(item.destino(activity));
            });
        }

        @Override
        public int getItemCount() {
            return itens.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {

            final TextView tvTitulo;
            final TextView tvMensagem;
            final TextView tvTempo;
            final View bolinha;

            ViewHolder(View itemView) {
                super(itemView);

                tvTitulo = itemView.findViewById(R.id.tvTituloNotificacao);
                tvMensagem = itemView.findViewById(R.id.tvMensagemNotificacao);
                tvTempo = itemView.findViewById(R.id.tvTempoNotificacao);
                bolinha = itemView.findViewById(R.id.bolinhaNaoLida);
            }
        }
    }
}
