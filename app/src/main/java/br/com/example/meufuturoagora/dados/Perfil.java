package br.com.example.meufuturoagora.dados;

import org.json.JSONObject;

public final class Perfil {

    public static final String ALUNO = "aluno";
    public static final String PROFESSOR = "professor";

    public final String id;
    public final String usuarioId;
    public final String email;
    public final String nome;
    public final String tipo;
    public final boolean admin;
    public final boolean ativo;
    public final String turmaId;
    public final String fotoGoogleUrl;
    public final String fotoPath;

    public Perfil(String id, String usuarioId, String email, String nome, String tipo, boolean admin,
                  boolean ativo, String turmaId, String fotoGoogleUrl, String fotoPath) {
        this.id = id;
        this.usuarioId = usuarioId;
        this.email = email;
        this.nome = nome;
        this.tipo = tipo;
        this.admin = admin;
        this.ativo = ativo;
        this.turmaId = turmaId;
        this.fotoGoogleUrl = fotoGoogleUrl;
        this.fotoPath = fotoPath;
    }

    public boolean ehAluno() {
        return ALUNO.equals(tipo);
    }

    public boolean ehProfessor() {
        return PROFESSOR.equals(tipo);
    }

    // Cadastro feito pelo admin, de quem ainda não entrou no app
    public boolean semConta() {
        return usuarioId == null;
    }

    static Perfil deJson(JSONObject json) {
        return new Perfil(
                Json.texto(json, "id"),
                Json.texto(json, "usuario_id"),
                Json.texto(json, "email"),
                Json.texto(json, "nome"),
                Json.texto(json, "tipo"),
                Json.logico(json, "admin"),
                json.optBoolean("ativo", true),
                Json.texto(json, "turma_id"),
                Json.texto(json, "foto_google_url"),
                Json.texto(json, "foto_path")
        );
    }
}
