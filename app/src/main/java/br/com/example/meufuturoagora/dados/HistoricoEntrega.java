package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.OffsetDateTime;

// Linha da aba "Entregas" do histórico
public final class HistoricoEntrega {

    public final String entregaId;
    public final String alunoNome;
    public final String atividadeNome;
    public final String disciplinaNome;
    public final String turmaNome;
    public final OffsetDateTime enviadoEm;
    public final int diasAtraso;
    // Nulo = ainda não avaliada
    public final Double nota;

    public HistoricoEntrega(String entregaId, String alunoNome, String atividadeNome, String disciplinaNome,
                            String turmaNome, OffsetDateTime enviadoEm, int diasAtraso, Double nota) {
        this.entregaId = entregaId;
        this.alunoNome = alunoNome;
        this.atividadeNome = atividadeNome;
        this.disciplinaNome = disciplinaNome;
        this.turmaNome = turmaNome;
        this.enviadoEm = enviadoEm;
        this.diasAtraso = diasAtraso;
        this.nota = nota;
    }

    public boolean avaliada() {
        return nota != null;
    }

    static HistoricoEntrega deJson(JSONObject json) {
        return new HistoricoEntrega(
                Json.texto(json, "entrega_id"),
                Json.texto(json, "aluno_nome"),
                Json.texto(json, "atividade_nome"),
                Json.texto(json, "disciplina_nome"),
                Json.texto(json, "turma_nome"),
                Json.momento(json, "enviado_em"),
                json.optInt("dias_atraso"),
                Json.decimal(json, "nota")
        );
    }
}
