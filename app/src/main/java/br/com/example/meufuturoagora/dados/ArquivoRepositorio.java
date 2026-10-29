package br.com.example.meufuturoagora.dados;

import java.util.List;

// Arquivos de atividades, entregas e fotos de perfil (ver Arquivos para os caminhos)
public interface ArquivoRepositorio {

    void enviar(String bucket, String caminho, byte[] conteudo, String tipoMime) throws ErroApi;

    // Link temporário para abrir ou mostrar o arquivo (Glide, FileProvider)
    String urlAssinada(String bucket, String caminho, int segundos) throws ErroApi;

    byte[] baixar(String bucket, String caminho) throws ErroApi;

    void apagar(String bucket, List<String> caminhos) throws ErroApi;
}
