package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// "Trilha" na interface do app
public final class Atividade {

    public final String id;
    public final String disciplinaId;
    public final String nome;
    public final String descricao;
    public final int pontos;
    public final LocalDate prazo;
    public final String link;
    public final String arquivoPath;
    public final String arquivoNome;
    public final int anoLetivo;
    public final Integer bimestre;
    public final OffsetDateTime criadoEm;

    public Atividade(String id, String disciplinaId, String nome, String descricao, int pontos,
                     LocalDate prazo, String link, String arquivoPath, String arquivoNome,
                     int anoLetivo, Integer bimestre, OffsetDateTime criadoEm) {
        this.id = id;
        this.disciplinaId = disciplinaId;
        this.nome = nome;
        this.descricao = descricao;
        this.pontos = pontos;
        this.prazo = prazo;
        this.link = link;
        this.arquivoPath = arquivoPath;
        this.arquivoNome = arquivoNome;
        this.anoLetivo = anoLetivo;
        this.bimestre = bimestre;
        this.criadoEm = criadoEm;
    }

    public boolean temArquivo() {
        return arquivoPath != null;
    }

    static Atividade deJson(JSONObject json) {
        return new Atividade(
                Json.texto(json, "id"),
                Json.texto(json, "disciplina_id"),
                Json.texto(json, "nome"),
                Json.texto(json, "descricao"),
                json.optInt("pontos"),
                Json.data(json, "prazo"),
                Json.texto(json, "link"),
                Json.texto(json, "arquivo_path"),
                Json.texto(json, "arquivo_nome"),
                json.optInt("ano_letivo"),
                Json.inteiro(json, "bimestre"),
                Json.momento(json, "criado_em")
        );
    }
}
