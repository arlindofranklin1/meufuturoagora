package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.OffsetDateTime;

public final class Disciplina {

    public final String id;
    public final String nome;
    public final String turmaId;
    // Vem da turma embutida na consulta (select=*,turmas(nome))
    public final String turmaNome;
    public final String professorId;
    public final int anoLetivo;
    public final Integer bimestre;
    public final boolean ativo;
    public final OffsetDateTime criadoEm;

    public Disciplina(String id, String nome, String turmaId, String turmaNome, String professorId,
                      int anoLetivo, Integer bimestre, boolean ativo, OffsetDateTime criadoEm) {
        this.id = id;
        this.nome = nome;
        this.turmaId = turmaId;
        this.turmaNome = turmaNome;
        this.professorId = professorId;
        this.anoLetivo = anoLetivo;
        this.bimestre = bimestre;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
    }

    static Disciplina deJson(JSONObject json) {

        JSONObject turma = json.optJSONObject("turmas");

        return new Disciplina(
                Json.texto(json, "id"),
                Json.texto(json, "nome"),
                Json.texto(json, "turma_id"),
                turma != null ? Json.texto(turma, "nome") : null,
                Json.texto(json, "professor_id"),
                json.optInt("ano_letivo"),
                Json.inteiro(json, "bimestre"),
                json.optBoolean("ativo", true),
                Json.momento(json, "criado_em")
        );
    }
}
