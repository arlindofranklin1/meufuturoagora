package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.OffsetDateTime;

public final class EntradaDiario {

    public final String id;
    public final String texto;
    public final OffsetDateTime criadoEm;
    public final OffsetDateTime atualizadoEm;

    public EntradaDiario(String id, String texto, OffsetDateTime criadoEm, OffsetDateTime atualizadoEm) {
        this.id = id;
        this.texto = texto;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    static EntradaDiario deJson(JSONObject json) {
        return new EntradaDiario(
                Json.texto(json, "id"),
                Json.texto(json, "texto"),
                Json.momento(json, "criado_em"),
                Json.momento(json, "atualizado_em")
        );
    }
}
