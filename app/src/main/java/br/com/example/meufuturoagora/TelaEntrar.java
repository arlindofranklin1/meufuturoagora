package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.content.Intent;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.SetOptions;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class TelaEntrar extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    private MaterialCardView googleButton;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_tela_entrar);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        // Firebase Authentication
        mAuth = FirebaseAuth.getInstance();

        // Credential Manager
        credentialManager = CredentialManager.create(this);

        // Botão Google
        googleButton = findViewById(R.id.googleButton);

        googleButton.setOnClickListener(v -> iniciarLoginGoogle());

        db = FirebaseFirestore.getInstance();

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        FirebaseUser user = mAuth.getCurrentUser();

        if (user != null) {

            // Usuário já está logado
            verificarUsuario(user);

        } else {

            // Usuário ainda não está logado
            googleButton.setOnClickListener(v -> iniciarLoginGoogle());
        }

        // Acessar como administrador
        findViewById(R.id.tvAcessarAdministrador).setOnClickListener(v ->
                startActivity(new Intent(this, TelaAdminLogin.class))
        );
    }

    private void iniciarLoginGoogle() {

        // Gera um nonce para proteger a autenticação
        String nonce = UUID.randomUUID().toString();

        // Configuração do login Google
        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                getString(R.string.default_web_client_id)
                        )
                        .setAutoSelectEnabled(false)
                        .setNonce(nonce)
                        .build();

        // Cria a solicitação de credencial
        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        // Abre a tela para escolher a conta Google
        credentialManager.getCredentialAsync(
                this,
                request,
                new android.os.CancellationSignal(),
                getMainExecutor(),

                new androidx.credentials.CredentialManagerCallback<
                        GetCredentialResponse,
                        androidx.credentials.exceptions.GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse result) {
                        processarLoginGoogle(result);
                    }

                    @Override
                    public void onError(
                            androidx.credentials.exceptions.GetCredentialException e) {

                        Toast.makeText(
                                TelaEntrar.this,
                                "ERRO: " + e.getMessage(),
                                Toast.LENGTH_SHORT
                        ).show();

                        e.printStackTrace();
                    }
                }
        );
    }

    private void processarLoginGoogle(GetCredentialResponse result) {

        Credential credential = result.getCredential();

        if (credential instanceof CustomCredential) {

            CustomCredential customCredential =
                    (CustomCredential) credential;

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                    .equals(customCredential.getType())) {

                try {

                    GoogleIdTokenCredential googleCredential =
                            GoogleIdTokenCredential.createFrom(
                                    customCredential.getData()
                            );

                    String idToken = googleCredential.getIdToken();

                    autenticarFirebase(idToken);

                } catch (Exception e) {

                    Toast.makeText(
                            TelaEntrar.this,
                            "Erro ao obter os dados da conta Google",
                            Toast.LENGTH_LONG
                    ).show();

                    e.printStackTrace();
                }

            } else {

                Toast.makeText(
                        TelaEntrar.this,
                        "Credencial Google inválida",
                        Toast.LENGTH_SHORT
                ).show();
            }

        } else {

            Toast.makeText(
                    TelaEntrar.this,
                    "Tipo de credencial não suportado",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

    private void autenticarFirebase(String idToken) {

        // Cria uma credencial do Firebase usando o token do Google
        AuthCredential firebaseCredential =
                GoogleAuthProvider.getCredential(
                        idToken,
                        null
                );

        // Faz login no Firebase
        mAuth.signInWithCredential(firebaseCredential)
                .addOnCompleteListener(this, task -> {

                    if (task.isSuccessful()) {

                        // Usuário autenticado
                        FirebaseUser user =
                                mAuth.getCurrentUser();

                        if (user != null) {

                            String nome = user.getDisplayName();
                            String email = user.getEmail();

                            Toast.makeText(
                                    TelaEntrar.this,
                                    "Bem-vindo, " + nome,
                                    Toast.LENGTH_SHORT
                            ).show();

                            verificarUsuario(user);

                        }

                    } else {

                        Toast.makeText(
                                TelaEntrar.this,
                                "Falha na autenticação com o Firebase",
                                Toast.LENGTH_LONG
                        ).show();

                        if (task.getException() != null) {
                            task.getException().printStackTrace();
                        }
                    }
                });
    }

    private void verificarUsuario(FirebaseUser user) {

        String email = user.getEmail();

        // O acesso ao aplicativo é restrito: o e-mail precisa estar
        // cadastrado e ativo em "professores" ou em "alunos" no Firestore.

        db.collection("professores")
                .whereEqualTo("email", email)
                .whereEqualTo("ativo", true)
                .get()
                .addOnSuccessListener(professores -> {

                    if (!professores.isEmpty()) {

                        atualizarFotoProfessor(user, professores.getDocuments().get(0).getId());
                        startActivity(new Intent(TelaEntrar.this, TelaInicialProfessor.class));
                        finish();
                        return;
                    }

                    verificarAluno(user, email);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Erro ao verificar e-mail: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void atualizarFotoProfessor(FirebaseUser user, String documentoId) {

        Map<String, Object> dados = new HashMap<>();

        dados.put(
                "fotoUrl",
                user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : ""
        );

        db.collection("professores")
                .document(documentoId)
                .set(dados, SetOptions.merge());
    }

    private void verificarAluno(FirebaseUser user, String email) {

        String uidAluno = user.getUid();

        db.collection("alunos")
                .whereEqualTo("email", email)
                .get()
                .addOnSuccessListener(registros -> {

                    // O cadastro feito pelo administrador tem um id aleatório; a conta do aluno
                    // usa o uid do Google. Se a conta já existe, ela vale; senão, vale o cadastro.
                    com.google.firebase.firestore.DocumentSnapshot registro = null;
                    List<String> cadastrosParaJuntar = new ArrayList<>();

                    for (com.google.firebase.firestore.DocumentSnapshot documento : registros.getDocuments()) {

                        if (documento.getId().equals(uidAluno)) {
                            registro = documento;
                        } else {
                            cadastrosParaJuntar.add(documento.getId());
                        }
                    }

                    if (registro == null && !registros.isEmpty()) {
                        registro = registros.getDocuments().get(0);
                    }

                    if (registro == null || !Boolean.TRUE.equals(registro.getBoolean("ativo"))) {
                        acessoNaoAutorizado();
                        return;
                    }

                    String turmaId = registro.getString("turmaId");
                    String turmaNome = registro.getString("turmaNome");

                    if (turmaId == null) {
                        acessoNaoAutorizado();
                        return;
                    }

                    prepararContaDoAluno(user, turmaId, turmaNome, cadastrosParaJuntar);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Erro ao verificar e-mail: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void prepararContaDoAluno(FirebaseUser user, String turmaId, String turmaNome,
                                      List<String> cadastrosParaJuntar) {

        String uidAluno = user.getUid();

        db.collection("alunos")
                .document(uidAluno)
                .get()
                .addOnSuccessListener(alunoDoc -> {

                    Map<String, Object> dadosAluno = new HashMap<>();
                    dadosAluno.put("nome", user.getDisplayName() != null ? user.getDisplayName() : "Aluno");
                    dadosAluno.put("email", user.getEmail());
                    dadosAluno.put(
                            "fotoUrl",
                            user.getPhotoUrl() != null ? user.getPhotoUrl().toString() : ""
                    );
                    dadosAluno.put("turmaId", turmaId);
                    dadosAluno.put("turmaNome", turmaNome != null ? turmaNome : "");
                    dadosAluno.put("ativo", true);

                    if (!alunoDoc.exists() || alunoDoc.getLong("pontuacao") == null) {
                        dadosAluno.put("pontuacao", 0L);
                    }

                    db.collection("alunos")
                            .document(uidAluno)
                            .set(dadosAluno, SetOptions.merge())
                            .addOnSuccessListener(unused -> {

                                // A conta passa a ser o único registro do aluno: remove o cadastro
                                // duplicado para não aparecer duas vezes na lista do administrador
                                for (String cadastroId : cadastrosParaJuntar) {
                                    db.collection("alunos").document(cadastroId).delete();
                                }

                                matricularNasDisciplinasDaTurma(uidAluno, turmaId, turmaNome);
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(
                                            this,
                                            "Erro ao preparar conta do aluno: " + e.getMessage(),
                                            Toast.LENGTH_LONG
                                    ).show()
                            );
                })
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Erro ao preparar conta do aluno: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void matricularNasDisciplinasDaTurma(String alunoId, String turmaId, String turmaNome) {

        db.collection("alunos")
                .document(alunoId)
                .get()
                .addOnSuccessListener(alunoDoc -> {

                    String alunoNome = alunoDoc.getString("nome");

                    db.collection("disciplinas")
                            .whereEqualTo("turmaId", turmaId)
                            .whereEqualTo("ativo", true)
                            .get()
                            .addOnSuccessListener(disciplinas -> {

                                for (QueryDocumentSnapshot disciplina : disciplinas) {

                                    Map<String, Object> matricula = new HashMap<>();

                                    matricula.put("disciplinaId", disciplina.getId());
                                    matricula.put("disciplinaNome", disciplina.getString("nome"));
                                    matricula.put("alunoId", alunoId);
                                    matricula.put("alunoNome", alunoNome != null ? alunoNome : "Aluno");
                                    matricula.put("turmaId", turmaId);
                                    matricula.put("turmaNome", turmaNome != null ? turmaNome : "");

                                    db.collection("matriculas")
                                            .document(disciplina.getId() + "_" + alunoId)
                                            .set(matricula);
                                }

                                irParaHomeDoAluno();
                            })
                            .addOnFailureListener(e -> irParaHomeDoAluno());
                })
                .addOnFailureListener(e -> irParaHomeDoAluno());
    }

    private void irParaHomeDoAluno() {

        startActivity(new Intent(TelaEntrar.this, TelaInicialAluno.class));
        finish();
    }

    private void acessoNaoAutorizado() {

        Toast.makeText(
                this,
                "Seu e-mail não está autorizado a acessar o aplicativo. Fale com a coordenação da escola.",
                Toast.LENGTH_LONG
        ).show();

        mAuth.signOut();
    }
}