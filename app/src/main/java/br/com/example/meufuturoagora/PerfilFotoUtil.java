package br.com.example.meufuturoagora;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.model.GlideUrl;

import java.io.ByteArrayOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import br.com.example.meufuturoagora.dados.Arquivos;
import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Foto de perfil do aluno/professor: por padrão é a da conta Google; quando o
// usuário escolhe outra no perfil, ela vai para o bucket privado fotos-perfil
// (decisão N6: qualquer usuário logado vê, só o dono troca) e passa a valer
// em todo o app.
class PerfilFotoUtil {

    // Lado maior da foto enviada (pixels): suficiente para o avatar e bem abaixo de 3 MB
    private static final int TAMANHO_MAXIMO = 512;

    // Links assinados valem 10 minutos; reaproveita por 9 para não assinar a cada tela
    private static final long REUSO_LINK_MS = 9 * 60 * 1000L;
    private static final Map<String, LinkAssinado> LINKS = new HashMap<>();

    private PerfilFotoUtil() {
    }

    // Foto escolhida pelo usuário, se houver; senão a do Google; senão o ícone do layout
    static void mostrar(Activity activity, String fotoPath, String fotoGoogleUrl, ImageView imageView) {

        if (fotoPath == null) {
            carregarNoGlide(activity, fotoGoogleUrl, null, imageView);
            return;
        }

        String link = linkGuardado(fotoPath);

        if (link != null) {
            carregarNoGlide(activity, link, fotoPath, imageView);
            return;
        }

        Tarefas.executar(activity,
                () -> Servicos.get(activity).arquivos.urlAssinada(
                        Arquivos.BUCKET_FOTOS, fotoPath, Arquivos.VALIDADE_LINK_SEGUNDOS),
                new Tarefas.Retorno<String>() {
                    @Override
                    public void sucesso(String url) {
                        guardarLink(fotoPath, url);
                        carregarNoGlide(activity, url, fotoPath, imageView);
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        // Sem a foto enviada, a do Google serve
                        carregarNoGlide(activity, fotoGoogleUrl, null, imageView);
                    }
                });
    }

    static void mostrar(Activity activity, Perfil perfil, ImageView imageView) {
        mostrar(activity, perfil.fotoPath, perfil.fotoGoogleUrl, imageView);
    }

    // Foto do usuário logado (telas iniciais)
    static void carregarFotoDoUsuario(Activity activity, ImageView imageView) {

        Tarefas.executar(activity, () -> Servicos.get(activity).perfis.meu(), new Tarefas.Retorno<Perfil>() {
            @Override
            public void sucesso(Perfil perfil) {
                if (perfil != null) {
                    mostrar(activity, perfil, imageView);
                }
            }

            @Override
            public void erro(ErroApi erro) {
                // Fica o ícone padrão
            }
        });
    }

    // TEMPORÁRIO até o módulo 9: telas que ainda leem a foto (URL) do Firestore
    static void mostrar(Activity activity, String fotoUrl, ImageView imageView) {
        carregarNoGlide(activity, fotoUrl, null, imageView);
    }

    // TEMPORÁRIO até o módulo 9 (InfoDisciplinaDialog ainda lê o Firestore)
    static String fotoDoDocumento(com.google.firebase.firestore.DocumentSnapshot documento) {

        if (documento == null || !documento.exists()) {
            return null;
        }

        String foto = documento.getString("fotoUrl");

        return foto != null && !foto.isEmpty() ? foto : null;
    }

    // Reduz a imagem escolhida, envia e salva como nova foto de perfil.
    // depois recebe o perfil com o caminho novo.
    static void trocarFoto(Activity activity, Uri uri, Perfil perfil, ImageView imageView,
                           java.util.function.Consumer<Perfil> depois) {

        if (uri == null || perfil == null) {
            return;
        }

        Toast.makeText(activity, "Enviando foto...", Toast.LENGTH_SHORT).show();

        Servicos servicos = Servicos.get(activity);
        String caminhoNovo = Arquivos.caminhoFoto(perfil.id);

        Tarefas.executar(activity, () -> {

            byte[] conteudo;

            try {
                conteudo = comprimir(activity, uri);
            } catch (Exception e) {
                // Imagem que não abre (formato estranho, arquivo corrompido)
                throw new IllegalStateException(e);
            }

            servicos.arquivos.enviar(Arquivos.BUCKET_FOTOS, caminhoNovo, conteudo, "image/jpeg");
            servicos.perfis.definirFoto(perfil.id, caminhoNovo);

            // A foto anterior não serve mais; se não der para apagar, só ocupa espaço
            if (perfil.fotoPath != null) {
                try {
                    servicos.arquivos.apagar(Arquivos.BUCKET_FOTOS, Collections.singletonList(perfil.fotoPath));
                } catch (ErroApi ignorado) {
                    // segue
                }
            }

            return new Perfil(perfil.id, perfil.usuarioId, perfil.email, perfil.nome, perfil.tipo,
                    perfil.admin, perfil.ativo, perfil.turmaId, perfil.fotoGoogleUrl, caminhoNovo);

        }, new Tarefas.Retorno<Perfil>() {
            @Override
            public void sucesso(Perfil atualizado) {
                mostrar(activity, atualizado, imageView);
                Toast.makeText(activity, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show();

                if (depois != null) {
                    depois.accept(atualizado);
                }
            }

            @Override
            public void erro(ErroApi erro) {
                Toast.makeText(activity, "Erro ao enviar foto: " + erro.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    // O Glide guarda a imagem pelo caminho (estável), e não pelo link assinado,
    // que muda a cada assinatura: assim a mesma foto não é baixada de novo
    private static void carregarNoGlide(Activity activity, String url, String chaveCache, ImageView imageView) {

        if (activity.isDestroyed() || url == null || url.isEmpty()) {
            return;
        }

        Object modelo = chaveCache == null ? url : new GlideUrl(url) {
            @Override
            public String getCacheKey() {
                return chaveCache;
            }
        };

        Glide.with(activity)
                .load(modelo)
                .placeholder(R.drawable.ic_perfil)
                .error(R.drawable.ic_perfil)
                .override(200, 200)
                .circleCrop()
                .into(imageView);
    }

    private static synchronized String linkGuardado(String fotoPath) {
        LinkAssinado link = LINKS.get(fotoPath);
        return link != null && System.currentTimeMillis() - link.criadoEm < REUSO_LINK_MS ? link.url : null;
    }

    private static synchronized void guardarLink(String fotoPath, String url) {
        LINKS.put(fotoPath, new LinkAssinado(url, System.currentTimeMillis()));
    }

    private static byte[] comprimir(Activity activity, Uri uri) throws Exception {

        ImageDecoder.Source fonte = ImageDecoder.createSource(activity.getContentResolver(), uri);

        Bitmap bitmap = ImageDecoder.decodeBitmap(fonte, (decoder, info, origem) -> {

            int largura = info.getSize().getWidth();
            int altura = info.getSize().getHeight();
            int maior = Math.max(largura, altura);

            if (maior > TAMANHO_MAXIMO) {

                float escala = (float) TAMANHO_MAXIMO / maior;
                decoder.setTargetSize(
                        Math.max(1, Math.round(largura * escala)),
                        Math.max(1, Math.round(altura * escala))
                );
            }

            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
        });

        ByteArrayOutputStream saida = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, saida);

        return saida.toByteArray();
    }

    private static final class LinkAssinado {

        final String url;
        final long criadoEm;

        LinkAssinado(String url, long criadoEm) {
            this.url = url;
            this.criadoEm = criadoEm;
        }
    }
}
