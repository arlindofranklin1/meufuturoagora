package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

// Professor ou colega de uma disciplina (InfoDisciplinaDialog). O e-mail dos
// alunos só vem para o professor da disciplina e o admin.
public final class Participante {

    public final String perfilId;
    public final String nome;
    public final String tipo;
    public final String email;
    public final String fotoGoogleUrl;
    public final String fotoPath;

    public Participante(String perfilId, String nome, String tipo, String email,
                        String fotoGoogleUrl, String fotoPath) {
        this.perfilId = perfilId;
        this.nome = nome;
        this.tipo = tipo;
        this.email = email;
        this.fotoGoogleUrl = fotoGoogleUrl;
        this.fotoPath = fotoPath;
    }

    static Participante deJson(JSONObject json) {
        return new Participante(
                Json.texto(json, "perfil_id"),
                Json.texto(json, "nome"),
                Json.texto(json, "tipo"),
                Json.texto(json, "email"),
                Json.texto(json, "foto_google_url"),
                Json.texto(json, "foto_path")
        );
    }
}
