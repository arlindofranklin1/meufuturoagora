package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

// Linha da aba "Alunos" do histórico: o que o aluno fez no período
public final class HistoricoAluno {

    public final String alunoId;
    public final String nome;
    // Nulo quando o aluno ficou sem turma
    public final String turmaNome;
    public final int entregas;
    public final int diasFalta;
    public final int pontos;

    public HistoricoAluno(String alunoId, String nome, String turmaNome, int entregas, int diasFalta, int pontos) {
        this.alunoId = alunoId;
        this.nome = nome;
        this.turmaNome = turmaNome;
        this.entregas = entregas;
        this.diasFalta = diasFalta;
        this.pontos = pontos;
    }

    static HistoricoAluno deJson(JSONObject json) {
        return new HistoricoAluno(
                Json.texto(json, "aluno_id"),
                Json.texto(json, "nome"),
                Json.texto(json, "turma_nome"),
                json.optInt("entregas"),
                json.optInt("dias_falta"),
                json.optInt("pontos")
        );
    }
}
