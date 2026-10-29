package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

public final class Turma {

    public final String id;
    public final String nome;
    public final Integer serie;
    public final boolean ativo;

    public Turma(String id, String nome, Integer serie, boolean ativo) {
        this.id = id;
        this.nome = nome;
        this.serie = serie;
        this.ativo = ativo;
    }

    static Turma deJson(JSONObject json) {
        return new Turma(
                Json.texto(json, "id"),
                Json.texto(json, "nome"),
                Json.inteiro(json, "serie"),
                json.optBoolean("ativo", true)
        );
    }
}
