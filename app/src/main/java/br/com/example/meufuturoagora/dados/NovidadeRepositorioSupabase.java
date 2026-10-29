package br.com.example.meufuturoagora.dados;

import java.time.OffsetDateTime;
import java.util.List;

public class NovidadeRepositorioSupabase implements NovidadeRepositorio {

    private final RestSupabase rest;

    public NovidadeRepositorioSupabase(RestSupabase rest) {
        this.rest = rest;
    }

    @Override
    public List<Novidade> desde(OffsetDateTime momento) throws ErroApi {
        return Json.lista(rest.rpcLista("novidades_desde", Json.objeto("p_desde", momento.toString())),
                Novidade::deJson);
    }
}
