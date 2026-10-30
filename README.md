# MeuFuturoAgora

App Android (Java) de uma escola: aluno, professor e administrador acompanham disciplinas,
trilhas (atividades), entregas, notas, frequência, ranking e um diário pessoal do aluno.

O app antigo usava Firebase e Cloudinary. Hoje o back-end é todo **Supabase** (Postgres com
RLS, Auth e Storage). O histórico da migração está em `docs/migracao/`.

## Como ele é por dentro

- **Login só com Google.** O e-mail e senha do Supabase ficam desligados em todo projeto. Quem
  entra precisa estar cadastrado pelo administrador (a função `vincular_conta` liga a conta
  Google ao cadastro pelo e-mail verificado).
- **Quem pode o quê é do banco**, não do app: Row Level Security em todas as tabelas, regras
  de negócio (nota, frequência, pontuação, exclusão) em funções do Postgres.
- **Arquivos** em buckets privados (`atividades`, `entregas`, `fotos-perfil`), até 3 MB, só PDF
  e imagens, abertos por link assinado de 10 minutos.
- **No app**, as telas pedem tudo a repositórios (`app/.../dados/`): uma interface por assunto e
  uma implementação sobre a API REST do Supabase. Nos testes de tela, entram repositórios falsos.

## Pré-requisitos

| Ferramenta | Para quê |
|---|---|
| Android Studio recente | abrir e compilar o app (o JDK 21 é baixado pelo Gradle) |
| [Supabase CLI](https://supabase.com/docs/guides/cli) | banco local, migrações e testes do banco |
| Docker Desktop | o Supabase local roda em contêineres |
| Um emulador Android (API 31+) ou um celular | testes de tela e de ponta a ponta |

## Configuração (`local.properties`)

O arquivo `local.properties` (na raiz, **fora do Git**) precisa de três linhas:

```properties
sdk.dir=C\:\\Users\\voce\\AppData\\Local\\Android\\Sdk
SUPABASE_URL=https://<ref-do-projeto>.supabase.co
SUPABASE_ANON_KEY=<chave pública: sb_publishable_... ou a anon>
```

- Só a **chave pública** vai para o app. O build recusa uma chave `sb_secret_...` ou `service_role`.
- Para o Supabase **local**, a URL é `http://10.0.2.2:54321` (o emulador enxerga o computador por
  esse endereço) e a chave sai de `supabase status -o env` (`PUBLISHABLE_KEY`).
- O Web Client ID do Google está em `app/src/main/res/values/strings.xml`
  (`default_web_client_id`). O mesmo ID precisa estar em *Authentication → Providers → Google*
  do projeto Supabase.
- O `app/debug.keystore` é compartilhado de propósito: todo computador gera o mesmo SHA-1, que é o
  que o cliente OAuth Android do Google conhece.

## Supabase local

```bash
supabase start                 # sobe o banco e os serviços (Docker precisa estar aberto)
supabase db reset --local      # recria o banco: migrações + supabase/seed.sql (dados fictícios)
supabase status -o env         # mostra a URL local e a PUBLISHABLE_KEY
supabase stop
```

Os usuários de teste do `seed.sql` usam e-mails `@exemplo.test`. Eles só existem no banco local.
Para ter o admin real também no local, gere um `supabase/seed_admin.local.sql` (veja
"Primeiro admin"); arquivos `*.local.sql` nunca vão para o Git.

No Windows com Git Bash, rode os comandos da CLI com `</dev/null` e coloque o Docker no PATH
(`/c/Program Files/Docker/Docker/resources/bin`).

## Testes

| O que | Comando | Precisa de |
|---|---|---|
| Banco (pgTAP: RLS, funções, Storage) | `supabase test db --local` | Supabase local, **recém-zerado** |
| Lint do banco e advisors | `supabase db lint --local` e `supabase db advisors --local` | Supabase local |
| App, testes JVM | `./gradlew testDebugUnitTest` | nada |
| Lint do app | `./gradlew lintDebug` | nada |
| Cobertura JVM (JaCoCo) | `./gradlew createDebugUnitTestCoverageReport` | nada |
| App, testes de tela (repositórios falsos) | `./gradlew installDebug installDebugAndroidTest` e depois `adb shell am instrument -w br.com.example.meufuturoagora.test/androidx.test.runner.AndroidJUnitRunner` | emulador |
| Ponta a ponta (telas e dados reais, banco local) | o mesmo `am instrument`, com `-e supabaseChaveLocal <PUBLISHABLE_KEY>` | emulador + Supabase local zerado |

Cuidados:

- Rode `supabase db reset --local` **antes** do pgTAP e dos testes de ponta a ponta: eles mexem nos dados.
- Os testes de ponta a ponta (`E2eSupabaseLocalTest`) entram com e-mail e senha só pelo código de teste;
  o app continua só com Google. Sem a chave local, eles são pulados.
- Use `adb shell am instrument`, não `connectedAndroidTest`: este desinstala o app do aparelho no fim.
- Se faltar memória, rode os instrumentados em duas partes: `-e class br.com.example.meufuturoagora.E2eSupabaseLocalTest`
  e depois `-e notClass br.com.example.meufuturoagora.E2eSupabaseLocalTest`.
- O limite de logins do Auth local foi subido para 300 em `supabase/config.toml` (só local).

## Migrações no projeto remoto

As migrações ficam em `supabase/migrations/` e são a única fonte do esquema.

```bash
supabase login                                  # uma vez (token pessoal, não cole em chats)
supabase link --project-ref <ref-do-projeto>    # uma vez
supabase migration list --linked                # compara local x remoto
supabase db push --linked --dry-run             # ensaio: mostra o que seria aplicado
supabase db push --linked                       # aplica
```

- A CLI fala com o banco pelas portas **5432/6543**; algumas redes (casa, escola) as bloqueiam.
  Use o roteador do celular se `db push` não conectar.
- **Nunca** rode `supabase config push`: o `config.toml` local liga o login por e-mail para os testes,
  e o remoto precisa continuar só com Google.
- Depois de aplicar, confira com `supabase db advisors --linked`.

## Primeiro admin

O administrador não tem senha: é uma pessoa cadastrada no banco que entra pelo Google.

1. Crie `admins.local.txt` (fora do Git) com o e-mail Google do administrador.
2. Gere `supabase/seed_admin.local.sql` com este conteúdo (fora do Git):

   ```sql
   insert into public.perfis (email, nome, tipo, admin)
   values ('<email do admin>', 'Administrador', 'professor', true)
   on conflict (email) do update
     set tipo = 'professor', turma_id = null, admin = true, ativo = true;
   ```

3. Local: `supabase db reset --local` aplica o arquivo sozinho. Remoto:
   `supabase db query --linked -f supabase/seed_admin.local.sql`.
4. Entre no app com essa conta Google. A partir daí, o admin cadastra os demais usuários pela
   própria tela de administração (só e-mail, nome, tipo e turma).

## Segurança

- O repositório é público: segredos, e-mails reais e senhas nunca entram nos commits
  (`credenciais.txt`, `admins.local.txt`, `local.properties` e `*.local.*` estão no `.gitignore`).
- O armazenamento da sessão do Supabase é cifrado com o Android Keystore e fica fora do backup.
- Veja a revisão de segurança item por item em `docs/migracao/relatorio-final.md`.

## Documentação da migração

- `PROMPT_MIGRACAO_SUPABASE.md`: o plano completo.
- `docs/migracao/inventario.md`: esquema, decisões (D1 a D15) e matriz de permissões.
- `docs/migracao/qa.md`: roteiros de QA manual e resultados dos testes automáticos.
- `docs/migracao/relatorio-final.md`: o que mudou, números, pendências e riscos.
- `docs/migracao/passos-do-usuario.md`: o que só o dono das contas pode fazer.
