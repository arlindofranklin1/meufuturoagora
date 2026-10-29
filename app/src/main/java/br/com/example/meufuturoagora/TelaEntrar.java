package br.com.example.meufuturoagora;

import android.os.Bundle;
import android.os.CancellationSignal;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialCancellationException;
import androidx.credentials.exceptions.GetCredentialException;
import androidx.credentials.exceptions.NoCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.android.material.card.MaterialCardView;

import br.com.example.meufuturoagora.dados.ErroApi;
import br.com.example.meufuturoagora.dados.Nonce;
import br.com.example.meufuturoagora.dados.Perfil;
import br.com.example.meufuturoagora.dados.Servicos;
import br.com.example.meufuturoagora.dados.Tarefas;

// Login com Google. Quem decide se o e-mail pode entrar, e como professor ou
// aluno, é o servidor (vincular_conta, pelo e-mail verificado da conta): o
// app não consulta cadastros nem liga contas por conta própria.
public class TelaEntrar extends AppCompatActivity {

    // Mensagem vinda da Splash (ex.: sessão expirada, sem internet)
    static final String EXTRA_MENSAGEM = "mensagem";

    private CredentialManager credentialManager;

    private MaterialCardView googleButton;
    private TextView entrarTextView;

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

        credentialManager = CredentialManager.create(this);

        googleButton = findViewById(R.id.googleButton);
        entrarTextView = findViewById(R.id.entrarTextView);

        googleButton.setOnClickListener(v -> iniciarLoginGoogle());

        String mensagem = getIntent().getStringExtra(EXTRA_MENSAGEM);

        if (mensagem != null) {
            Toast.makeText(this, mensagem, Toast.LENGTH_LONG).show();
        }
    }

    private void iniciarLoginGoogle() {

        // O Google recebe o hash do nonce e o coloca no ID token; o Supabase
        // recebe o valor cru e confere. Assim um token roubado não é reaproveitado.
        Nonce nonce = Nonce.gerar();

        GetGoogleIdOption googleIdOption =
                new GetGoogleIdOption.Builder()
                        .setFilterByAuthorizedAccounts(false)
                        .setServerClientId(
                                getString(R.string.default_web_client_id)
                        )
                        .setAutoSelectEnabled(false)
                        .setNonce(nonce.hash)
                        .build();

        GetCredentialRequest request =
                new GetCredentialRequest.Builder()
                        .addCredentialOption(googleIdOption)
                        .build();

        mostrarCarregando(true);

        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                getMainExecutor(),

                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse result) {
                        processarLoginGoogle(result, nonce);
                    }

                    @Override
                    public void onError(GetCredentialException e) {

                        mostrarCarregando(false);

                        // Fechar a janela de contas não é erro
                        if (e instanceof GetCredentialCancellationException) {
                            return;
                        }

                        String mensagem = e instanceof NoCredentialException
                                ? "Nenhuma conta Google neste aparelho. Adicione uma nas configurações do Android."
                                : "Não foi possível entrar com o Google. Tente de novo.";

                        Toast.makeText(TelaEntrar.this, mensagem, Toast.LENGTH_LONG).show();
                    }
                }
        );
    }

    private void processarLoginGoogle(GetCredentialResponse result, Nonce nonce) {

        Credential credential = result.getCredential();

        if (!(credential instanceof CustomCredential)
                || !GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {

            mostrarCarregando(false);
            Toast.makeText(this, "Tipo de credencial não suportado", Toast.LENGTH_SHORT).show();
            return;
        }

        String idToken;

        try {
            idToken = GoogleIdTokenCredential.createFrom(credential.getData()).getIdToken();
        } catch (Exception e) {
            mostrarCarregando(false);
            Toast.makeText(this, "Erro ao obter os dados da conta Google", Toast.LENGTH_LONG).show();
            return;
        }

        Tarefas.executar(this,
                () -> Servicos.get(this).auth.entrarComGoogle(idToken, nonce.cru),
                new Tarefas.Retorno<Perfil>() {
                    @Override
                    public void sucesso(Perfil perfil) {

                        Toast.makeText(TelaEntrar.this, "Bem-vindo, " + perfil.nome, Toast.LENGTH_SHORT).show();

                        PonteFirebase.entrar(idToken, () -> {
                            if (!isFinishing() && !isDestroyed()) {
                                RotaInicial.abrir(TelaEntrar.this, perfil);
                            }
                        });
                    }

                    @Override
                    public void erro(ErroApi erro) {
                        mostrarCarregando(false);
                        Toast.makeText(TelaEntrar.this, erro.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    // Evita dois logins ao mesmo tempo com toques repetidos
    private void mostrarCarregando(boolean carregando) {
        googleButton.setEnabled(!carregando);
        entrarTextView.setText(carregando ? "Entrando..." : "Entrar com o Google");
    }
}
