package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.OffsetDateTime;

public final class AnoLetivo {

    public final int ano;
    public final boolean atual;
    public final OffsetDateTime iniciadoEm;
    public final OffsetDateTime encerradoEm;

    public AnoLetivo(int ano, boolean atual, OffsetDateTime iniciadoEm, OffsetDateTime encerradoEm) {
        this.ano = ano;
        this.atual = atual;
        this.iniciadoEm = iniciadoEm;
        this.encerradoEm = encerradoEm;
    }

    static AnoLetivo deJson(JSONObject json) {
        return new AnoLetivo(
                json.optInt("ano"),
                Json.logico(json, "atual"),
                Json.momento(json, "iniciado_em"),
                Json.momento(json, "encerrado_em")
        );
    }
}
