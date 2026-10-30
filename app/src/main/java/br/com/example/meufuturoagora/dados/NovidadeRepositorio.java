package br.com.example.meufuturoagora.dados;

import java.time.OffsetDateTime;
import java.util.List;

// Para o NotificacaoWorker: o que mudou desde a última verificação, já
// filtrado pelo servidor para o que o usuário pode ver
public interface NovidadeRepositorio {

    List<Novidade> desde(OffsetDateTime momento) throws ErroApi;
}
