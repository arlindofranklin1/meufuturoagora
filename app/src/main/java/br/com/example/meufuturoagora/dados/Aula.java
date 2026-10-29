package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.LocalDate;

public final class Aula {

    public final String id;
    public final String disciplinaId;
    public final LocalDate data;
    public final int quantidade;
    public final String descricao;
    public final Integer bimestre;

    public Aula(String id, String disciplinaId, LocalDate data, int quantidade, String descricao,
                Integer bimestre) {
        this.id = id;
        this.disciplinaId = disciplinaId;
        this.data = data;
        this.quantidade = quantidade;
        this.descricao = descricao;
        this.bimestre = bimestre;
    }

    static Aula deJson(JSONObject json) {
        return new Aula(
                Json.texto(json, "id"),
                Json.texto(json, "disciplina_id"),
                Json.data(json, "data"),
                json.optInt("quantidade"),
                Json.texto(json, "descricao"),
                Json.inteiro(json, "bimestre")
        );
    }
}
