# Prompt: migração completa do MeuFuturoAgora de Firebase + Cloudinary para Supabase

> **Para a IA que vai executar este plano no terminal do Android Studio.**
> Leia este documento inteiro antes de qualquer ação. Siga as fases na ordem.
> Ao fim de cada fase há um **PORTÃO**: pare, mostre o resultado ao usuário e só
> continue com a aprovação explícita dele. Escreva para o usuário sempre em
> português do Brasil.

---

## 1. Contexto

- App Android **em Java** (não Kotlin), pacote `br.com.example.meufuturoagora`,
  `minSdk 31`, `targetSdk 36`, `compileSdk 36`, Java 11 no código-fonte.
- AGP 9.3.3 e Gradle 9.5.0, com o daemon fixado em JDK 21
  (`gradle/gradle-daemon-jvm.properties`). Não altere o JDK. O
  `gradle.properties` tem 10 flags de compatibilidade adicionadas pelo Upgrade
  Assistant, e 7 delas geram aviso de obsoleta. Removê-las é uma tarefa
  separada e opcional: proponha ao usuário, não faça junto com a migração.
- O repositório no GitHub (`arlindofranklin1/meufuturoagora`) é **público**.
  Tudo o que for commitado fica visível para qualquer pessoa.
- Uso atual de serviços externos:
  - **Firebase Auth**: só o login com Google (Credential Manager +
    `GoogleAuthProvider`) em `TelaEntrar.java`.
  - **Cloud Firestore**: cerca de 35 dos 52 arquivos Java. Coleções:
    `alunos`, `disciplinas`, `atividades`, `entregas`, `matriculas`,
    `configuracoes` (documento `geral`), `professores`, `turmas`, `frequencias`,
    `aulas`, `anosLetivos`, `penalidades`, `frequenciasArquivadas`, `entradas`,
    `diarios`, `administrador`.
    Recursos usados: `WriteBatch`, `whereIn` (22 usos), `orderBy`, `limit`,
    `FieldValue.serverTimestamp`, `FieldValue.delete`, `SetOptions.merge`, `FieldPath`
    e um `addSnapshotListener` (tempo real) em `DiarioAlunoActivity.java`.
  - **Cloudinary**: upload de atividades, entregas e fotos de perfil em
    `CloudinaryUtil.java`, com preset *unsigned* e URLs públicas.
  - **Firebase Storage**: está declarado no `app/build.gradle.kts`, mas não é usado.
  - **WorkManager**: `NotificacaoWorker` consulta o banco periodicamente para
    gerar notificações locais.
- **Não há dados a migrar.** Tudo no Firebase e no Cloudinary é de teste e
  pode ser descartado. Não escreva scripts de exportação ou importação.
- Testes existentes: só os exemplos gerados pelo Android Studio
  (`ExampleUnitTest`, `ExampleInstrumentedTest`).

### 1.1 Problemas de segurança conhecidos que a migração DEVE corrigir

1. **Arquivos públicos.** As URLs do Cloudinary abrem sem login. Entregas de
   alunos, que podem ser menores de idade, e fotos de perfil ficam expostas a
   quem tiver o link.
2. **Upload aberto.** O cloud name e o upload preset *unsigned* estão no
   código-fonte. Qualquer pessoa pode enviar arquivos para a conta.
3. **Senha do administrador em texto puro.** Fica na coleção `administrador` e
   é conferida no cliente com `whereEqualTo("senha", senha)`
   (`DefinirAnoLetivoActivity.java`).
4. **Autorização só no cliente.** Os perfis admin, professor e aluno são
   decididos no app (`AdminUtil.java`, `TelaEntrar.java`). Não há regras do
   Firestore versionadas no repositório.
5. **Pontuação e ranking gravados pelo cliente.** Um aluno com acesso à API
   poderia alterar a própria pontuação.
6. **Vinculação de conta por e-mail feita no cliente.** Em `TelaEntrar.java`,
   cadastros feitos pelo admin são ligados à conta Google pelo e-mail e
   documentos são apagados ou recriados pelo próprio app.

---

## 2. Regras de trabalho (obrigatórias)

1. **Branch.** Trabalhe em `migracao-supabase`, criada a partir de `main`.
   Nunca faça commit direto na `main` nem `git push --force`. Só faça push
   quando o usuário pedir.
2. **Commits pequenos**, um por unidade lógica, com mensagens em português.
   Rode os testes antes de cada commit.
3. **Segredos.**
   - Nenhum segredo vai para o Git. `local.properties`, `credenciais.txt` e
     `supabase/.env` ficam fora do repositório. Confira o `.gitignore` antes do
     primeiro commit.
   - O app só pode conter a **chave pública** do Supabase (`anon` ou
     `sb_publishable_...`). A chave **`service_role` / `sb_secret_...` nunca
     entra no app**, nem em testes instrumentados.
   - Peça ao usuário que grave os valores nos arquivos. Não peça que cole
     segredos no chat quando houver alternativa. Nunca repita um segredo em
     respostas, logs ou mensagens de commit.
4. **Ações destrutivas ou externas** exigem confirmação antes. Isso inclui
   apagar arquivos, `supabase db reset` no projeto remoto, `supabase db push`,
   alterar configurações no painel e desativar serviços do Firebase ou do
   Cloudinary.
5. **Honestidade nos resultados.** Se um teste falhar, mostre a saída. Se pular
   algo, diga que pulou. Nunca diga que algo está testado sem ter rodado o teste.
6. **Estilo de código.** Imite o código existente: Java, nomes em português,
   comentários curtos explicando o "porquê", sem comentários óbvios.
   Não converta o projeto para Kotlin.
7. **Versões de bibliotecas.** Use a versão estável mais recente de cada
   dependência, conferida no momento da execução. Nunca use versões dinâmicas
   (`+`, `latest`). Registre-as em `gradle/libs.versions.toml`.
8. **Não invente.** Antes de usar qualquer API do Supabase (Auth, PostgREST,
   Storage, Realtime, CLI), confira a documentação oficial atual em
   https://supabase.com/docs. Os nomes de chaves e endpoints mudam com o tempo.
9. **Dúvida de produto** (regra de negócio, quem pode ver o quê): pergunte ao
   usuário em vez de supor.

---

## 3. Credenciais e informações a solicitar ao usuário (Fase 0)

### 3.0 Já decidido na preparação (não pergunte de novo)

- **Ferramentas:** o próprio usuário instala o Supabase CLI e o Docker Desktop.
  A IA só verifica (`supabase --version`, `docker info`) e, se faltar algo,
  repete as instruções, sem instalar por conta própria.
- **Google OAuth:** o usuário tem acesso de administrador ao projeto Google
  Cloud `meufuturoagora-79b76`, que é o do Firebase. **Reaproveite** os clientes
  existentes:
  - Web Client ID: `301921058257-fqdmdlufq5m6f8t6j7v81s9s0v5s31k6.apps.googleusercontent.com`
  - Android Client ID do `app/debug.keystore` (SHA-1 `EE:6F:D6:17:D9:63:35:9B:08:C7:61:65:E8:64:ED:65:55:81:BA:95`):
    `301921058257-nnf0e7b6r1feko9e3h23lulaka8f6p97.apps.googleusercontent.com`

  Client IDs não são segredos. G1 e G2 já estão resolvidos: falta só o usuário
  cadastrar o Web Client ID no provedor Google do Supabase. **Não** exclua o
  projeto Google Cloud ao desativar o Firebase na Fase 6.
- **Cloudinary:** o usuário vai desativar o upload preset `meufuturoagora_unsigned`
  antes da migração. O upload de arquivos do app fica quebrado até a Fase 5,
  módulo 5, e isso é esperado. Não tente "consertar" o Cloudinary.
- **Projeto Supabase:** por enquanto, **um único projeto de desenvolvimento**.
  O projeto de produção será criado depois, antes de o app ir para usuários
  reais. Tudo deve ser feito por migrações versionadas, para que o mesmo esquema
  possa ser aplicado ao projeto de produção sem passos manuais.
- **Respostas de negócio** (substituem as perguntas N1 a N6 da Seção 3.3):
  - **N1, admin inicial:** o e-mail está no arquivo local `admins.local.txt`, na
    raiz do projeto e fora do Git. Use-o para gerar o seed de admin. **Nunca**
    escreva esse e-mail em arquivo versionado, porque o repositório é público.
  - **N2, diário:** **sem tempo real**. Atualize ao abrir a tela, em `onResume`
    e com puxar para atualizar (`SwipeRefreshLayout`). Remova o
    `addSnapshotListener`.
  - **N3, Docker:** o usuário instala (ver acima).
  - **N4, CI:** **não** configure CI. Pule o passo 5 da Fase 7.
  - **N5, ano letivo:** **remova a senha do administrador** sem substituto. Basta
    o usuário estar logado e ser admin (`perfis.admin = true`, verificado na
    função do servidor). Sem reautenticação.
  - **N6, fotos de perfil:** todos os usuários **autenticados** (alunos e
    professores) podem ver as fotos de perfil de qualquer usuário. O bucket
    `fotos-perfil` continua privado, com URLs assinadas e sem acesso `anon`.
    Só o dono grava a própria foto.
  - **Professores:** cada professor vê e gerencia **apenas as próprias
    disciplinas** e o que pertence a elas (aulas, atividades, frequências,
    entregas, alunos matriculados).

Peça tudo isto **no início**, explicando onde encontrar cada item. Marque o que
for opcional. Não avance para a Fase 3 sem os itens obrigatórios.

### 3.1 Supabase

| # | Item | Obrigatório | Onde encontrar | Onde guardar |
|---|---|---|---|---|
| S1 | **URL do projeto** (`https://<ref>.supabase.co`) | Sim | Painel → Project Settings → API | `local.properties` → `SUPABASE_URL` |
| S2 | **Chave pública** (`anon` ou `sb_publishable_...`) | Sim | Painel → Project Settings → API Keys | `local.properties` → `SUPABASE_ANON_KEY` |
| S3 | **Project ref** | Sim | Parte da URL, ou Project Settings → General | Pode ser dito no chat (não é segredo) |
| S4 | **Personal Access Token** para a CLI | Sim | https://supabase.com/dashboard/account/tokens | Variável de ambiente `SUPABASE_ACCESS_TOKEN`, definida pelo próprio usuário. **Não colar no chat.** |
| S5 | **Senha do banco** (para `supabase link` / `db push`) | Sim | Definida na criação do projeto, ou redefinida em Database Settings | Digitada pelo usuário quando a CLI pedir, ou `SUPABASE_DB_PASSWORD` no ambiente. **Não colar no chat.** |
| S6 | Chave `service_role` / `sb_secret_...` | **Não pedir** | — | Os testes usam o Supabase local, que gera chaves próprias. |

Se o usuário ainda não tiver o projeto, oriente a criação:
- Região **South America (São Paulo)**, por latência e porque os dados são de
  estudantes brasileiros (LGPD).
- Um projeto para desenvolvimento/homologação. Pergunte se ele quer um segundo
  projeto para produção (recomendado).

### 3.2 Google (login)

| # | Item | Obrigatório | Onde encontrar |
|---|---|---|---|
| G1 | **Web Client ID** do OAuth (o que o app usa como `serverClientId`, hoje `default_web_client_id`, gerado pelo `google-services.json`) | Sim | Google Cloud Console → APIs & Services → Credentials, no mesmo projeto do Firebase ou num novo |
| G2 | **Android Client ID** com o pacote `br.com.example.meufuturoagora` e o **SHA-1 do `app/debug.keystore`** | Sim | Mesmo local. Gere o SHA-1 com `keytool -list -v -keystore app/debug.keystore -alias androiddebugkey -storepass android` |
| G3 | SHA-1 do keystore de **release** | Só se houver publicação | Keystore de release do usuário (nunca versionado) |
| G4 | Client Secret do Web Client | Só se o login web via OAuth for habilitado; o fluxo nativo por ID token não precisa | Google Cloud Console |

O usuário precisa cadastrar o G1 em *Supabase → Authentication → Sign In / Providers → Google*,
no campo de Client IDs autorizados. Mantenha a verificação de *nonce* ativa.

### 3.3 Informações de negócio

| # | Pergunta |
|---|---|
| N1 | Quais e-mails serão **administradores** iniciais? (vai para um seed SQL fora do Git ou executado manualmente) |
| N2 | A tela de diário precisa de **tempo real**, ou basta atualizar ao abrir e ao puxar para atualizar? |
| N3 | Há **Docker Desktop** instalado? É necessário para o Supabase local (`supabase start`), onde rodam os testes de banco e de RLS. Se não houver, oriente a instalação. |
| N4 | Deve haver **CI** (GitHub Actions) rodando os testes a cada push? O repositório é público ou privado? |
| N5 | Confirma que a "senha do administrador" para trocar o ano letivo pode ser **substituída por**: ser admin e confirmar com um novo login Google recente (reautenticação)? |
| N6 | Os alunos podem ver a **foto de perfil** dos colegas (ranking) e dos professores? E os professores, as dos alunos? |

---

## 4. Arquitetura alvo

### 4.1 Visão geral

```
Telas (Activities)  ──►  Repositórios (interfaces Java)  ──►  Implementação Supabase
                                                           ├─ SupabaseAuth      (GoTrue  /auth/v1)
                                                           ├─ SupabaseRest      (PostgREST /rest/v1 + RPC)
                                                           └─ SupabaseStorage   (/storage/v1)
```

- **Sem SDK Kotlin.** Use a API REST do Supabase diretamente, com **OkHttp**,
  e testes com **MockWebServer**. Isso mantém o projeto 100% Java.
- **Camada de repositórios.** Crie interfaces (`AlunoRepositorio`,
  `DisciplinaRepositorio`, `AtividadeRepositorio`, `EntregaRepositorio`,
  `FrequenciaRepositorio`, `ArquivoRepositorio`, `AuthRepositorio`, etc.).
  As telas dependem só das interfaces, obtidas de um localizador simples
  (`Servicos.java`) que os testes podem substituir por *fakes*. Não adicione
  Hilt/Dagger sem pedir ao usuário.
- **Threads.** Nada de rede na thread principal. Use um `ExecutorService` e
  devolva o resultado na *main thread*, como o `CloudinaryUtil` já faz. Trate
  `Activity` destruída antes de atualizar a UI.
- **Erros.** Uma classe de erro única (`ErroApi`) com código HTTP e mensagem
  amigável em português. Nunca mostre ao usuário a resposta crua do servidor
  nem o token.
- **Configuração.** `SUPABASE_URL` e `SUPABASE_ANON_KEY` são lidos do
  `local.properties` e expostos via `BuildConfig` (`buildFeatures.buildConfig = true`).
  O build deve falhar com mensagem clara se faltarem.

### 4.2 Autenticação

- Mantenha o Credential Manager com `GetGoogleIdOption`, mas com
  **nonce**: gere um nonce aleatório, envie o hash SHA-256 ao Google e o nonce
  original ao Supabase.
- Troque o ID token do Google por uma sessão Supabase com o fluxo
  `grant_type=id_token` (provider `google`). Confira o endpoint atual na
  documentação.
- **Sessão.** Guarde o *refresh token* **criptografado com o Android Keystore**
  (AES-GCM). Não use `EncryptedSharedPreferences`, que foi descontinuado.
  O *access token* fica só em memória. Implemente a renovação automática ao
  receber 401 e na abertura do app (`Splash`).
- O logout revoga a sessão no servidor e apaga o armazenamento local.
- **Não** confie no e-mail para autorizar nada no cliente. Toda autorização é
  feita no banco, com RLS e funções.

### 4.3 Banco de dados (Postgres)

- Esquema versionado em `supabase/migrations/` (Supabase CLI), um arquivo por
  mudança e sem editar migrações já aplicadas.
- **Tabela central `perfis`**: `id uuid` PK que referencia `auth.users(id)` (nulo
  até o primeiro login), `email citext unique not null`, `nome`, `tipo`
  (`'aluno' | 'professor'`, com `check`), `admin boolean default false`, `ativo`,
  `foto_path`, `foto_personalizada`, `criado_em`, `atualizado_em`. Se o
  inventário (Fase 1) mostrar que alunos e professores têm campos muito
  diferentes, use `perfis` mais as tabelas `alunos` e `professores` 1:1.
- **Vinculação do cadastro com a conta.** O admin cadastra o e-mail antes. No
  primeiro login, uma função `security definer` (`vincular_conta()`), chamada
  via RPC ou por trigger em `auth.users`, liga o cadastro ao `auth.uid()` usando
  o **e-mail verificado** do token. Isso substitui toda a lógica de "conta
  recriada / uid novo" de `TelaEntrar.java` e `DisciplinaUtil.vincularAoProfessor`.
- Mapeie cada coleção para tabelas normalizadas:
  - IDs `uuid default gen_random_uuid()`.
  - **Chaves estrangeiras** com `on delete` definido conscientemente.
  - **Índices** para toda FK e todo filtro frequente.
  - `check` constraints para valores fechados (status, bimestre, nota de 0 a 10…).
  - `criado_em timestamptz default now()` no lugar de `serverTimestamp`.
  - Trigger `atualizado_em`.
  - Unicidade composta onde hoje há ID composto, como `matriculas`
    (`disciplina_id`, `aluno_id`).
  - Remova a desnormalização feita só por limitação do Firestore (por exemplo
    `alunoNome` copiado em `matriculas`). Use `JOIN` ou *views*.
- **Operações em lote ou com regra de negócio** viram **funções Postgres
  transacionais** chamadas via RPC, no lugar de `WriteBatch` no cliente:
  - definir ou trocar o ano letivo (arquivando frequências e reiniciando o ranking),
  - matricular um aluno em todas as disciplinas da turma,
  - registrar a frequência de uma aula,
  - avaliar uma entrega e aplicar a pontuação,
  - penalidades por falta,
  - excluir ou desativar usuário.
- **Pontuação e ranking são calculados no servidor**, por função ou trigger.
  O cliente **não** tem permissão de `update` nessas colunas. O ranking é uma
  *view* ou função que expõe só o mínimo (nome, foto, pontos), conforme a resposta N6.
- **Elimine a tabela/coleção `administrador` e a senha em texto puro.** O
  admin é `perfis.admin = true`. Ações críticas exigem admin, verificado
  dentro da função no servidor. Não há reautenticação nem senha extra
  (decisão N5). Remova o diálogo de senha de `DefinirAnoLetivoActivity`.

### 4.4 Row Level Security (RLS)

- **RLS habilitado em todas as tabelas** do esquema `public`, sem exceção.
  Tabela nova sem política = ninguém acessa.
- Funções auxiliares `security definer`, `stable`, com `set search_path = ''`:
  `eh_admin()`, `perfil_atual()`, `professor_da_disciplina(disciplina_id)`,
  `aluno_matriculado(disciplina_id)`.
- Políticas separadas para `select`, `insert`, `update` e `delete`, nunca
  `for all` genérico. Use `(select auth.uid())` nas políticas (melhor
  desempenho).
- Matriz de permissões, a ser **detalhada na Fase 1 e aprovada pelo usuário**:
  - **Aluno**: lê os próprios dados, as disciplinas em que está matriculado,
    as atividades dessas disciplinas, as próprias entregas, frequências e
    diário, e o ranking (só os campos públicos). Cria e edita a **própria**
    entrega enquanto o prazo estiver aberto. Não altera nota, pontuação nem
    frequência.
  - **Professor**: lê e escreve dados das **suas** disciplinas (aulas,
    atividades, frequência, avaliação de entregas). **Não** vê disciplinas de
    outros professores (confirmado pelo usuário).
  - **Fotos de perfil**: qualquer usuário autenticado vê as fotos de todos
    (confirmado pelo usuário).
  - **Admin**: gerencia usuários, turmas, disciplinas, ano letivo, bimestres e
    configurações.
  - **Anônimo (`anon`)**: nenhum acesso a tabelas.
- Revogue os privilégios padrão desnecessários e não exponha funções internas
  via RPC (`revoke execute ... from anon, public`).

### 4.5 Storage

- Buckets **privados**: `atividades`, `entregas`, `fotos-perfil`.
- Em cada bucket, configure **limite de 3 MB** (mesmo limite de
  `ArquivoUtil.LIMITE_TAMANHO_BYTES`) e **lista de MIME types permitidos**.
- Convenção de caminho que as políticas de `storage.objects` possam validar:
  - `atividades/<disciplina_id>/<atividade_id>/<uuid>.<ext>`: escreve o professor da disciplina; leem o professor e os alunos matriculados.
  - `entregas/<atividade_id>/<aluno_uid>/<uuid>.<ext>`: escreve só o próprio aluno; leem o aluno e o professor da disciplina.
  - `fotos-perfil/<uid>/<uuid>.jpg`: escreve só o dono; leitura conforme N6.
- **Nunca use o nome original do arquivo no caminho.** Guarde o nome original
  numa coluna do banco, para exibição.
- No banco, guarde o **caminho** do objeto, nunca uma URL.
- **Leitura**: gere **URL assinada de curta duração** (por exemplo 10 minutos)
  no momento de abrir. Para documentos, baixe para o cache do app e abra com
  `FileProvider` e `Intent.ACTION_VIEW`, em vez de passar a URL ao navegador.
  Para imagens, use Glide com a URL assinada e uma chave de cache estável (o
  caminho), não a URL.
- Ao excluir uma atividade, entrega ou foto, remova também o objeto. Use uma
  função no servidor ou garanta a ordem no repositório.

### 4.6 Tempo real e notificações

- **Diário (`DiarioAlunoActivity`)**: **sem tempo real** (decisão N2).
  Atualize ao abrir, ao voltar (`onResume`) e com puxar para atualizar. Não use
  o Supabase Realtime.
- **`NotificacaoWorker`**: troque as consultas ao Firestore por uma RPC única
  `novidades_desde(timestamp)`, que devolve só o que o usuário pode ver (RLS).
  O worker usa a sessão salva e a renova se necessário. Se não houver sessão,
  não faz nada.

---

## 5. Estratégia de testes

Nenhuma fase está concluída sem os testes dela escritos **e** passando.

### 5.1 Banco (pgTAP, `supabase test db`, com Supabase local)

- Pasta `supabase/tests/`.
- Para **cada tabela**: RLS habilitado. Para cada perfil (anon, aluno A,
  aluno B, professor da disciplina, outro professor, admin): testes positivos
  (pode) **e negativos** (não pode) de `select`, `insert`, `update` e `delete`.
- Para **cada função RPC**: caminho feliz, permissão negada, entradas
  inválidas e atomicidade (se falhar no meio, nada é gravado).
- **Storage**: políticas de `storage.objects` por bucket e caminho, incluindo
  a tentativa de gravar no caminho de outro usuário.
- Casos de segurança obrigatórios:
  - Aluno não lê a entrega de outro aluno.
  - Aluno não altera a própria nota nem a pontuação.
  - Professor não acessa disciplina de outro professor.
  - Não admin não chama funções de admin.
  - `anon` não lê nada.
  - Um usuário não se torna admin alterando o próprio perfil.
  - `vincular_conta()` não vincula um e-mail diferente do e-mail do token.

### 5.2 Testes unitários JVM (`app/src/test`, JUnit 4)

- **Lógica pura existente**: escreva testes de caracterização **antes** de
  mexer em `PontuacaoUtil`, `BimestreUtil`, `PenalidadeFaltaUtil`,
  `AnoLetivoUtil`, `ArquivoUtil` e `CursosIfUtil`, para garantir que o
  comportamento não muda. Se a lógica for para o servidor, os mesmos casos
  viram testes pgTAP.
- **Clientes HTTP e repositórios** com **MockWebServer**: verifique URL,
  método, cabeçalhos (`apikey`, `Authorization: Bearer`), corpo, parsing das
  respostas, erros (400, 401 com renovação de token, 403, 404, 409, 5xx,
  timeout) e que nenhum token aparece em logs.
- **Sessão**: renovação de token, logout e sessão corrompida ou ausente.

### 5.3 Testes instrumentados (`app/src/androidTest`, Espresso)

- Use **repositórios *fake*** injetados por `Servicos` para testes de UI
  determinísticos. Cubra pelo menos:
  - roteamento após o login (aluno, professor, admin, usuário sem cadastro),
  - criar uma atividade com anexo,
  - entregar uma atividade como aluno,
  - avaliar uma entrega,
  - registrar frequência,
  - definir o ano letivo como admin,
  - ranking,
  - logout.
- Um pequeno conjunto de **testes de ponta a ponta** contra o **Supabase local**
  (emulador acessando `http://10.0.2.2:54321`), com usuários de teste criados
  por seed local. Nunca rode contra o projeto de produção.

### 5.4 Qualidade e segurança estáticas

- `./gradlew lint` sem novos erros. Trate os avisos de segurança (`Security`,
  `HardcodedText` quando aplicável, `SetJavaScriptEnabled`, etc.).
- `supabase db lint` sem erros.
- **Supabase Security Advisor e Performance Advisor** (painel ou CLI/MCP) sem
  alertas de segurança. Explique ao usuário qualquer aviso remanescente.
- Cobertura com **JaCoCo** nos testes JVM: meta de pelo menos 80% nas classes
  de repositório, cliente HTTP e sessão. Reporte o número real.
- Busca por segredos antes de cada commit (`git diff --cached` procurando
  `eyJ`, `sb_secret`, `service_role`, `password`, `senha`).
- Confirme que `android:allowBackup` e as regras de backup não exportam o
  armazenamento da sessão. Ajuste `data_extraction_rules` se necessário.

### 5.5 QA manual no emulador (checklist ao fim de cada fase e na final)

Para cada perfil (aluno, professor, admin), percorra **todas** as telas
listadas na Fase 1. Registre em `docs/migracao/qa.md`: tela, ação, esperado,
obtido, ok/falha. Inclua casos de rede ruim (modo avião durante o upload) e
de sessão expirada.

---

## 6. Fases de execução

### Fase 0: Preparação

1. Confirme que o build atual passa: `./gradlew assembleDebug`.
2. Crie a branch `migracao-supabase`.
3. Solicite as credenciais e informações da **Seção 3**.
4. Verifique as ferramentas: Supabase CLI (`supabase --version`) e Docker.
   Se faltar alguma, oriente a instalação no Windows.
5. Rode `supabase init` e `supabase start`. Confirme que o ambiente local sobe.
6. Ajuste o `.gitignore`: `supabase/.temp`, `supabase/.env`, arquivos de seed
   com e-mails reais.

**PORTÃO 0**: mostre o que foi recebido (sem exibir segredos), o que falta e
que o ambiente local funciona.

### Fase 1: Inventário e especificação

Gere `docs/migracao/inventario.md` com:

1. **Por coleção**: todos os campos, tipos, quem lê e quem escreve (arquivo:linha),
   consultas usadas (filtros, ordenação, limites) e relações com outras coleções.
2. **Por tela**: o que lê, o que grava, perfil que acessa e fluxos de navegação.
3. **Regras de negócio** implícitas no código: pontuação, penalidades,
   bimestres, ano letivo, prazos de entrega, vinculação de conta.
4. **Matriz de permissões** perfil × tabela × operação (Seção 4.4), com as
   dúvidas destacadas.
5. **Esquema proposto**: diagrama em texto (ou Mermaid) com tabelas, colunas,
   tipos, FKs, índices e funções RPC.
6. **Ordem dos módulos** para a Fase 4, com as dependências entre eles.

**PORTÃO 1**: o usuário revisa e aprova o inventário, a matriz de permissões e o
esquema. Esta é a decisão mais importante do projeto. Não escreva código de
produção antes da aprovação.

### Fase 2: Testes de caracterização

1. Escreva os testes JVM da lógica pura existente (Seção 5.2, primeiro item)
   e faça-os passar **sem alterar** a lógica.
2. Configure JaCoCo e o relatório de cobertura.

**PORTÃO 2**: mostre os testes e o resultado.

### Fase 3: Banco, RLS e Storage (com Supabase local)

1. Migrações em `supabase/migrations/`: extensões, tabelas, constraints,
   índices, triggers, funções auxiliares, funções RPC, *views*, RLS e políticas.
2. Buckets e políticas de Storage, também por migração.
3. `supabase/seed.sql` **apenas com dados fictícios**, para desenvolvimento
   local.
4. Testes pgTAP completos (Seção 5.1). Rode `supabase db reset` (local) e
   `supabase test db`.
5. `supabase db lint` e advisors.

**PORTÃO 3**: mostre o resumo das migrações, a saída dos testes e dos advisors.
Peça autorização **antes** de aplicar no projeto remoto
(`supabase link` e `supabase db push`).

### Fase 4: Camada de dados no app

1. Adicione as dependências (OkHttp, MockWebServer nos testes) e o `BuildConfig`
   com as credenciais do `local.properties`.
2. Implemente `SupabaseCliente` (HTTP base, cabeçalhos, erros, renovação de
   token), `SessaoArmazenamento` (Keystore) e as interfaces e implementações
   dos repositórios.
3. Testes MockWebServer de tudo (Seção 5.2).

**PORTÃO 4**: mostre os testes e a cobertura.

### Fase 5: Migração das telas, módulo por módulo

Ordem sugerida (ajuste conforme o inventário):

1. **Login e sessão**: `Splash`, `TelaLogin1`, `TelaEntrar`, `ConfiguracoesActivity` (logout).
2. **Perfis e admin**: `AdminUtil`, `TelaAdminHome`, `CadastrarUsuarioActivity`,
   `ListaUsuariosActivity`, `TelaPerfilAluno`, `TelaPerfilProfessor`,
   `PerfilFotoUtil`, `FotoUtil`, `RecortarFotoActivity`.
3. **Estrutura escolar**: anos letivos, bimestres, turmas, disciplinas,
   matrículas (`DefinirAnoLetivoActivity`, `DefinirBimestresActivity`,
   `HistoricoAnoLetivoActivity`, `CriarDisciplinaActivity`, `DisciplinaUtil`,
   `AnoLetivoUtil`, `BimestreUtil`).
4. **Aulas e frequência**: `CadastrarAulaActivity`, `RegistrarFrequenciaActivity`,
   `FrequenciaAulaActivity`, `PenalidadeFaltaUtil`.
5. **Atividades, entregas e arquivos**: `CriarAtividade`, `TelaAtividadeAluno`,
   `AvaliarEntregaActivity`, `QuestoesDoLivroActivity`, `ArquivoUtil`. Substitua o
   `CloudinaryUtil` pelo repositório de arquivos do Supabase Storage.
6. **Diário**: `DiarioAlunoActivity` (tempo real conforme N2).
7. **Desempenho e ranking**: `DesempenhoActivity`, `RankingActivity`, `PontuacaoUtil`.
8. **Notificações**: `NotificacaoWorker`, `NotificacaoUtil`, `NotificacaoHistorico`,
   `NotificacoesDialog`.
9. **Demais telas**: `MeuFuturoActivity`, `CursoDetalheActivity`, `TelaInicial*`,
   `TelaDisciplina*`, `TelaDetalhesDisciplina*`, `InfoDisciplinaDialog`, `SobreActivity`.

Para **cada módulo**:
1. Troque o acesso ao Firestore pelo repositório.
2. Escreva ou atualize os testes Espresso com *fakes* (Seção 5.3).
3. Rode **todos** os testes (JVM, instrumentados e pgTAP) e o lint.
4. Faça QA manual do módulo no emulador, contra o Supabase local ou de desenvolvimento.
5. Faça um commit do módulo.

**PORTÃO 5.x**: ao fim de cada módulo, resumo curto ao usuário com o resultado
dos testes. O usuário pode pedir para seguir vários módulos sem parar.

### Fase 6: Remoção do Firebase e do Cloudinary

1. Confirme com `grep` que não resta nenhum `com.google.firebase`,
   `FirebaseFirestore`, `FirebaseAuth` nem `cloudinary` no código.
2. Remova as dependências do Firebase, o plugin `com.google.gms.google-services`
   (raiz e app), o `app/google-services.json` e o `CloudinaryUtil.java`.
3. Mova o `default_web_client_id` para um recurso ou `BuildConfig` próprio.
   Antes ele era gerado pelo plugin do Google Services.
4. Build, todos os testes e lint.
5. **Instruções ao usuário**, sem executar por ele: desativar o upload preset
   e apagar os arquivos de teste no Cloudinary; desativar ou excluir o projeto
   Firebase quando estiver seguro. O Web Client ID do Google pode depender do
   projeto do Google Cloud ligado ao Firebase, então **não** exclua esse projeto
   do Google Cloud se as credenciais OAuth estiverem nele.

**PORTÃO 6**: mostre o resultado.

### Fase 7: Verificação final e entrega

1. Suíte completa: `./gradlew testDebugUnitTest connectedDebugAndroidTest lint`,
   `supabase test db`, `supabase db lint` e os advisors.
2. QA manual completo (Seção 5.5) com os três perfis.
3. **Revisão de segurança** final, item por item da Seção 1.1, com a evidência
   (teste ou trecho) de que cada problema foi resolvido.
4. Atualize ou crie o `README.md` com:
   - pré-requisitos (JDK é automático, Supabase CLI, Docker),
   - como preencher o `local.properties`,
   - como rodar o Supabase local e os testes,
   - como aplicar migrações no remoto,
   - como cadastrar o primeiro admin.
5. (Pulado: o usuário não quer CI.)
6. Gere `docs/migracao/relatorio-final.md`: o que mudou, decisões, testes e
   cobertura (números reais), pendências e riscos conhecidos.

**PORTÃO FINAL**: apresente o relatório. Pergunte se deve abrir um Pull Request
de `migracao-supabase` para `main`.

---

## 7. Critérios de aceite

- [ ] Nenhuma referência a Firebase ou Cloudinary no código nem no build.
- [ ] Todas as telas funcionam para os três perfis (QA manual registrado).
- [ ] RLS habilitado em 100% das tabelas, com testes positivos e negativos por perfil.
- [ ] Buckets privados, URLs assinadas de curta duração, limite de tamanho e MIME no servidor.
- [ ] Nenhuma senha armazenada pelo app. O admin é definido no banco.
- [ ] Pontuação, notas e frequência não são alteráveis pelo aluno (provado por teste).
- [ ] Nenhum segredo no repositório. Só a chave pública no app.
- [ ] Testes JVM, instrumentados e pgTAP passando. Cobertura reportada com números reais.
- [ ] Lint, `supabase db lint` e advisors sem erros de segurança.
- [ ] README atualizado e relatório final entregue.

---

## 8. Primeira mensagem esperada da IA

Ao começar, a IA deve:
1. Confirmar que leu este plano.
2. Rodar a Fase 0, passos 1 e 2.
3. Conferir que `admins.local.txt` existe e que o Docker e o Supabase CLI estão
   instalados.
4. Pedir ao usuário, numa única mensagem organizada, **só o que ainda falta**
   da Seção 3: S1 a S5 e o cadastro do Web Client ID no Supabase. As decisões
   da Seção 3.0 já estão tomadas. Diga onde encontrar cada item e onde gravar.
   Os segredos (S4, S5) o usuário grava por conta própria, sem colar no chat.
