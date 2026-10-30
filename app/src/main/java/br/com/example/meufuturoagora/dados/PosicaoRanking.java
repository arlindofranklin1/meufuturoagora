package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

public final class PosicaoRanking {

    public final int posicao;
    public final int pontos;
    public final int total;

    public PosicaoRanking(int posicao, int pontos, int total) {
        this.posicao = posicao;
        this.pontos = pontos;
        this.total = total;
    }

    static PosicaoRanking deJson(JSONObject json) {
        return new PosicaoRanking(json.optInt("posicao"), json.optInt("pontos"), json.optInt("total"));
    }
}
