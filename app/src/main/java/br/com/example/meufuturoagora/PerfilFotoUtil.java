package br.com.example.meufuturoagora;

import android.app.Activity;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;

// Foto de perfil do aluno/professor: por padrão é a da conta Google; quando o
// usuário escolhe outra no perfil, ela vai para o Cloudinary, fica salva no
// Firestore (fotoUrl + fotoPersonalizada) e passa a valer em todo o app.
class PerfilFotoUtil {

    static final String CAMPO_FOTO_PERSONALIZADA = "fotoPersonalizada";

    // Lado maior da foto enviada (pixels): suficiente para o avatar e bem abaixo de 3 MB
    private static final int TAMANHO_MAXIMO = 512;

    private PerfilFotoUtil() {
    }

    // Documento do usuário: aluno usa o uid; professor é encontrado pelo e-mail
    private static Task<DocumentReference> buscarDocumento(FirebaseFirestore db, FirebaseUser usuario,
                                                           boolean professor) {

        if (!professor) {
            return Tasks.forResult(db.collection("alunos").document(usuario.getUid()));
        }

        return db.collection("professores")
                .whereEqualTo("email", usuario.getEmail())
                .limit(1)
                .get()
                .continueWith(tarefa -> tarefa.isSuccessful() && !tarefa.getResult().isEmpty()
                        ? tarefa.getResult().getDocuments().get(0).getReference()
                        : null);
    }

    static void carregarFotoDoUsuario(Activity activity, ImageView imageView, boolean professor) {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null) {
            return;
        }

        String fotoGoogle = usuario.getPhotoUrl() != null ? usuario.getPhotoUrl().toString() : null;

        mostrar(activity, fotoGoogle, imageView);

        buscarDocumento(FirebaseFirestore.getInstance(), usuario, professor)
                .addOnSuccessListener(referencia -> {

                    if (referencia == null) {
                        return;
                    }

                    referencia.get().addOnSuccessListener(documento -> {

                        String foto = fotoDoDocumento(documento);

                        if (foto != null && !foto.equals(fotoGoogle)) {
                            mostrar(activity, foto, imageView);
                        }
                    });
                });
    }

    // Foto salva no documento (aluno ou professor), ou null se não houver
    static String fotoDoDocumento(DocumentSnapshot documento) {

        if (documento == null || !documento.exists()) {
            return null;
        }

        String foto = documento.getString("fotoUrl");

        return foto != null && !foto.isEmpty() ? foto : null;
    }

    static void mostrar(Activity activity, String fotoUrl, ImageView imageView) {

        if (activity.isDestroyed() || fotoUrl == null || fotoUrl.isEmpty()) {
            return;
        }

        Glide.with(activity)
                .load(fotoUrl)
                .placeholder(R.drawable.ic_perfil)
                .error(R.drawable.ic_perfil)
                .override(200, 200)
                .circleCrop()
                .into(imageView);
    }

    // Reduz a imagem escolhida, envia e salva como nova foto de perfil
    static void trocarFoto(Activity activity, Uri uri, boolean professor, ImageView imageView) {

        FirebaseUser usuario = FirebaseAuth.getInstance().getCurrentUser();

        if (usuario == null || uri == null) {
            return;
        }

        Toast.makeText(activity, "Enviando foto...", Toast.LENGTH_SHORT).show();

        Handler principal = new Handler(Looper.getMainLooper());

        new Thread(() -> {

            byte[] conteudo;

            try {

                conteudo = comprimir(activity, uri);

            } catch (Exception e) {

                principal.post(() -> Toast.makeText(
                        activity,
                        "Não foi possível abrir a imagem selecionada.",
                        Toast.LENGTH_LONG
                ).show());
                return;
            }

            CloudinaryUtil.enviarBytes(
                    conteudo,
                    "image/jpeg",
                    "perfil_" + usuario.getUid() + ".jpg",
                    "perfis/" + usuario.getUid(),
                    new CloudinaryUtil.Callback() {

                        @Override
                        public void onSucesso(String url) {
                            salvarFoto(activity, usuario, professor, url, imageView);
                        }

                        @Override
                        public void onErro(String mensagem) {

                            Toast.makeText(
                                    activity,
                                    "Erro ao enviar foto: " + mensagem,
                                    Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );
        }).start();
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

    private static void salvarFoto(Activity activity, FirebaseUser usuario, boolean professor,
                                    String url, ImageView imageView) {

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        Map<String, Object> dados = new HashMap<>();
        dados.put("fotoUrl", url);
        dados.put(CAMPO_FOTO_PERSONALIZADA, true);

        // Também atualiza a conta, para as telas que leem a foto direto do login
        usuario.updateProfile(new UserProfileChangeRequest.Builder()
                .setPhotoUri(Uri.parse(url))
                .build());

        buscarDocumento(db, usuario, professor)
                .addOnSuccessListener(referencia -> {

                    if (referencia == null) {

                        Toast.makeText(activity, "Cadastro não encontrado.", Toast.LENGTH_LONG).show();
                        return;
                    }

                    referencia.set(dados, SetOptions.merge())
                            .addOnSuccessListener(unused -> {

                                mostrar(activity, url, imageView);
                                Toast.makeText(activity, "Foto de perfil atualizada!", Toast.LENGTH_SHORT).show();
                            })
                            .addOnFailureListener(e -> Toast.makeText(
                                    activity,
                                    "Erro ao salvar foto: " + e.getMessage(),
                                    Toast.LENGTH_LONG
                            ).show());
                });
    }
}
