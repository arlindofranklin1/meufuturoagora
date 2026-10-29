package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

public final class LinhaRanking {

    public final String perfilId;
    public final String nome;
    public final String fotoGoogleUrl;
    public final String fotoPath;
    public final int pontos;
    public final int posicao;
    public final boolean ehVoce;

    public LinhaRanking(String perfilId, String nome, String fotoGoogleUrl, String fotoPath,
                        int pontos, int posicao, boolean ehVoce) {
        this.perfilId = perfilId;
        this.nome = nome;
        this.fotoGoogleUrl = fotoGoogleUrl;
        this.fotoPath = fotoPath;
        this.pontos = pontos;
        this.posicao = posicao;
        this.ehVoce = ehVoce;
    }

    static LinhaRanking deJson(JSONObject json) {
        return new LinhaRanking(
                Json.texto(json, "perfil_id"),
                Json.texto(json, "nome"),
                Json.texto(json, "foto_google_url"),
                Json.texto(json, "foto_path"),
                json.optInt("pontos"),
                json.optInt("posicao"),
                Json.logico(json, "eh_voce")
        );
    }
}
