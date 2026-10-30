package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.OffsetDateTime;

public final class Entrega {

    public final String id;
    public final String atividadeId;
    public final String alunoId;
    public final String comentarioAluno;
    public final String arquivoPath;
    public final String arquivoNome;
    public final OffsetDateTime enviadoEm;
    // Decisão D4: entrega atrasada é aceita e marcada
    public final int diasAtraso;
    public final Double nota;
    public final String comentarioProfessor;
    public final OffsetDateTime avaliadoEm;

    public Entrega(String id, String atividadeId, String alunoId, String comentarioAluno,
                   String arquivoPath, String arquivoNome, OffsetDateTime enviadoEm, int diasAtraso,
                   Double nota, String comentarioProfessor, OffsetDateTime avaliadoEm) {
        this.id = id;
        this.atividadeId = atividadeId;
        this.alunoId = alunoId;
        this.comentarioAluno = comentarioAluno;
        this.arquivoPath = arquivoPath;
        this.arquivoNome = arquivoNome;
        this.enviadoEm = enviadoEm;
        this.diasAtraso = diasAtraso;
        this.nota = nota;
        this.comentarioProfessor = comentarioProfessor;
        this.avaliadoEm = avaliadoEm;
    }

    public boolean avaliada() {
        return avaliadoEm != null;
    }

    public boolean atrasada() {
        return diasAtraso > 0;
    }

    static Entrega deJson(JSONObject json) {
        return new Entrega(
                Json.texto(json, "id"),
                Json.texto(json, "atividade_id"),
                Json.texto(json, "aluno_id"),
                Json.texto(json, "comentario_aluno"),
                Json.texto(json, "arquivo_path"),
                Json.texto(json, "arquivo_nome"),
                Json.momento(json, "enviado_em"),
                json.optInt("dias_atraso", 0),
                Json.decimal(json, "nota"),
                Json.texto(json, "comentario_professor"),
                Json.momento(json, "avaliado_em")
        );
    }
}
