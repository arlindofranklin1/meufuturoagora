package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.OffsetDateTime;

// Decisão D12: pedido de exclusão dos dados, aprovado ou recusado pelo admin
public final class SolicitacaoExclusao {

    public static final String PENDENTE = "pendente";
    public static final String APROVADA = "aprovada";
    public static final String RECUSADA = "recusada";

    public final String id;
    public final String perfilId;
    public final String motivo;
    public final String status;
    public final OffsetDateTime criadoEm;
    public final OffsetDateTime decididoEm;

    public SolicitacaoExclusao(String id, String perfilId, String motivo, String status,
                               OffsetDateTime criadoEm, OffsetDateTime decididoEm) {
        this.id = id;
        this.perfilId = perfilId;
        this.motivo = motivo;
        this.status = status;
        this.criadoEm = criadoEm;
        this.decididoEm = decididoEm;
    }

    public boolean pendente() {
        return PENDENTE.equals(status);
    }

    static SolicitacaoExclusao deJson(JSONObject json) {
        return new SolicitacaoExclusao(
                Json.texto(json, "id"),
                Json.texto(json, "perfil_id"),
                Json.texto(json, "motivo"),
                Json.texto(json, "status"),
                Json.momento(json, "criado_em"),
                Json.momento(json, "decidido_em")
        );
    }
}
