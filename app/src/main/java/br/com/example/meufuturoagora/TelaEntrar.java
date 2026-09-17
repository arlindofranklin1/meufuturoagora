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

import java.util.HashMap;
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
        // na lista de professores ou de alunos em UsuariosAutorizados.

        if (UsuariosAutorizados.ehProfessor(email)) {

            startActivity(new Intent(TelaEntrar.this, TelaInicialProfessor.class));
            finish();
            return;
        }

        String turma = UsuariosAutorizados.turmaDoAluno(email);

        if (turma != null) {

            prepararContaDoAluno(user, turma);
            return;
        }

        acessoNaoAutorizado();
    }

    private void prepararContaDoAluno(FirebaseUser user, String turma) {

        String uidAluno = user.getUid();

        Map<String, Object> dadosAluno = new HashMap<>();
        dadosAluno.put("nome", user.getDisplayName() != null ? user.getDisplayName() : "Aluno");
        dadosAluno.put("email", user.getEmail());
        dadosAluno.put("turma", turma);

        db.collection("alunos")
                .document(uidAluno)
                .set(dadosAluno, SetOptions.merge())
                .addOnSuccessListener(unused -> matricularNasDisciplinasDaTurma(uidAluno, turma))
                .addOnFailureListener(e ->
                        Toast.makeText(
                                this,
                                "Erro ao preparar conta do aluno: " + e.getMessage(),
                                Toast.LENGTH_LONG
                        ).show()
                );
    }

    private void matricularNasDisciplinasDaTurma(String alunoId, String turma) {

        db.collection("alunos")
                .document(alunoId)
                .get()
                .addOnSuccessListener(alunoDoc -> {

                    String alunoNome = alunoDoc.getString("nome");

                    db.collection("disciplinas")
                            .whereEqualTo("turma", turma)
                            .whereEqualTo("ativo", true)
                            .get()
                            .addOnSuccessListener(disciplinas -> {

                                for (QueryDocumentSnapshot disciplina : disciplinas) {

                                    Map<String, Object> matricula = new HashMap<>();

                                    matricula.put("disciplinaId", disciplina.getId());
                                    matricula.put("disciplinaNome", disciplina.getString("nome"));
                                    matricula.put("alunoId", alunoId);
                                    matricula.put("alunoNome", alunoNome != null ? alunoNome : "Aluno");
                                    matricula.put("turma", turma);

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