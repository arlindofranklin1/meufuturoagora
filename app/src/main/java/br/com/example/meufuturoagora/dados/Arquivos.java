package br.com.example.meufuturoagora.dados;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

// Buckets, caminhos e tipos de arquivo aceitos. Os caminhos seguem o que as
// políticas do Storage conferem (migração 20261029161607_storage.sql) e nunca
// usam o nome original do arquivo, que fica só no banco.
public final class Arquivos {

    public static final String BUCKET_ATIVIDADES = "atividades";
    public static final String BUCKET_ENTREGAS = "entregas";
    public static final String BUCKET_FOTOS = "fotos-perfil";

    // Mesmo limite dos buckets e de ArquivoUtil.LIMITE_TAMANHO_BYTES
    public static final long LIMITE_BYTES = 3L * 1024 * 1024;

    // Decisão D5: só PDF e imagens
    public static final List<String> TIPOS_PERMITIDOS =
            Arrays.asList("application/pdf", "image/jpeg", "image/png", "image/webp");

    // Para abrir links assinados: curto, só o tempo de abrir o arquivo
    public static final int VALIDADE_LINK_SEGUNDOS = 600;

    private Arquivos() {
    }

    public static boolean tipoPermitido(String tipoMime) {
        return tipoMime != null && TIPOS_PERMITIDOS.contains(tipoMime);
    }

    public static String extensao(String tipoMime) {

        if (tipoMime == null) {
            return "bin";
        }

        switch (tipoMime) {
            case "application/pdf":
                return "pdf";
            case "image/jpeg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/webp":
                return "webp";
            default:
                return "bin";
        }
    }

    // atividades/<disciplina>/<atividade>/<uuid>.<ext>
    public static String caminhoAtividade(String disciplinaId, String atividadeId, String tipoMime) {
        return disciplinaId + "/" + atividadeId + "/" + UUID.randomUUID() + "." + extensao(tipoMime);
    }

    // entregas/<atividade>/<aluno>/<uuid>.<ext>
    public static String caminhoEntrega(String atividadeId, String alunoId, String tipoMime) {
        return atividadeId + "/" + alunoId + "/" + UUID.randomUUID() + "." + extensao(tipoMime);
    }

    // fotos-perfil/<perfil>/<uuid>.jpg (o app sempre converte a foto para JPEG)
    public static String caminhoFoto(String perfilId) {
        return perfilId + "/" + UUID.randomUUID() + ".jpg";
    }
}
