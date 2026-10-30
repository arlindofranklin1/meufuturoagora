package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.OffsetDateTime;

// Item para as notificações (NotificacaoWorker)
public final class Novidade {

    public static final String DISCIPLINA = "disciplina";
    public static final String ATIVIDADE = "atividade";
    public static final String AVALIACAO = "avaliacao";
    public static final String ENTREGA = "entrega";

    public final String tipo;
    public final String itemId;
    public final String nome;
    public final String disciplinaId;
    public final String disciplinaNome;
    public final String atividadeId;
    public final String alunoNome;
    public final OffsetDateTime momento;

    public Novidade(String tipo, String itemId, String nome, String disciplinaId, String disciplinaNome,
                    String atividadeId, String alunoNome, OffsetDateTime momento) {
        this.tipo = tipo;
        this.itemId = itemId;
        this.nome = nome;
        this.disciplinaId = disciplinaId;
        this.disciplinaNome = disciplinaNome;
        this.atividadeId = atividadeId;
        this.alunoNome = alunoNome;
        this.momento = momento;
    }

    static Novidade deJson(JSONObject json) {
        return new Novidade(
                Json.texto(json, "tipo"),
                Json.texto(json, "item_id"),
                Json.texto(json, "nome"),
                Json.texto(json, "disciplina_id"),
                Json.texto(json, "disciplina_nome"),
                Json.texto(json, "atividade_id"),
                Json.texto(json, "aluno_nome"),
                Json.momento(json, "momento")
        );
    }
}
