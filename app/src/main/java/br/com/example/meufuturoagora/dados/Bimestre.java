package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

import java.time.LocalDate;

public final class Bimestre {

    public final int ano;
    public final int numero;
    public final LocalDate inicio;
    public final LocalDate fim;

    public Bimestre(int ano, int numero, LocalDate inicio, LocalDate fim) {
        this.ano = ano;
        this.numero = numero;
        this.inicio = inicio;
        this.fim = fim;
    }

    // O último dia conta inteiro (BimestreUtil.fimDoDia)
    public boolean contem(LocalDate data) {
        return data != null && !data.isBefore(inicio) && !data.isAfter(fim);
    }

    static Bimestre deJson(JSONObject json) {
        return new Bimestre(
                json.optInt("ano"),
                json.optInt("numero"),
                Json.data(json, "inicio"),
                Json.data(json, "fim")
        );
    }
}
