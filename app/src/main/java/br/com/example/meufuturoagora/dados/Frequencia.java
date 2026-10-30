package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

public final class Frequencia {

    // Valores aceitos pelo banco (check em public.frequencias)
    public static final String PRESENTE = "presente";
    public static final String FALTA = "falta";
    public static final String FALTA_JUSTIFICADA = "falta_justificada";

    public final String aulaId;
    public final String alunoId;
    public final String status;

    public Frequencia(String aulaId, String alunoId, String status) {
        this.aulaId = aulaId;
        this.alunoId = alunoId;
        this.status = status;
    }

    static Frequencia deJson(JSONObject json) {
        return new Frequencia(
                Json.texto(json, "aula_id"),
                Json.texto(json, "aluno_id"),
                Json.texto(json, "status")
        );
    }
}
