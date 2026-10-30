package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.LocalDate;
import java.time.OffsetDateTime;

// Linha da aba "Trilhas" do histórico
public final class HistoricoTrilha {

    public final String atividadeId;
    public final String nome;
    public final String disciplinaNome;
    public final String professorNome;
    public final String turmaNome;
    public final LocalDate prazo;
    public final int pontos;
    // Todas as entregas da trilha, não só as do período
    public final int entregas;
    public final OffsetDateTime criadoEm;

    public HistoricoTrilha(String atividadeId, String nome, String disciplinaNome, String professorNome,
                           String turmaNome, LocalDate prazo, int pontos, int entregas, OffsetDateTime criadoEm) {
        this.atividadeId = atividadeId;
        this.nome = nome;
        this.disciplinaNome = disciplinaNome;
        this.professorNome = professorNome;
        this.turmaNome = turmaNome;
        this.prazo = prazo;
        this.pontos = pontos;
        this.entregas = entregas;
        this.criadoEm = criadoEm;
    }

    static HistoricoTrilha deJson(JSONObject json) {
        return new HistoricoTrilha(
                Json.texto(json, "atividade_id"),
                Json.texto(json, "nome"),
                Json.texto(json, "disciplina_nome"),
                Json.texto(json, "professor_nome"),
                Json.texto(json, "turma_nome"),
                Json.data(json, "prazo"),
                json.optInt("pontos"),
                json.optInt("entregas"),
                Json.momento(json, "criado_em")
        );
    }
}
