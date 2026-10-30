# Relatório final da migração Firebase + Cloudinary → Supabase

Data: 30/10/2026. Branch `migracao-supabase` (26 commits à frente da `main`, 179 arquivos
alterados: +18.954 / −6.395 linhas). **Nada foi enviado ao GitHub.**

## 1. Resumo

O app MeuFuturoAgora (Java, Android) usava Firebase (Auth e Firestore) e Cloudinary (arquivos).
Agora usa só o Supabase: Postgres com RLS, Auth com Google e Storage com buckets privados.
Firebase e Cloudinary não aparecem mais no código nem no build.

Os motivos eram de privacidade e segurança (muitos alunos são menores de idade): arquivos
públicos por URL, upload aberto, senha do administrador em texto puro, autorização só no app,
pontuação gravada pelo cliente e vinculação de conta feita no app.

**O que está pronto:** as Fases 0 a 6 do plano, os 9 módulos de telas e a Fase 7 automática
(testes, lint, advisors, revisão de segurança, README e este relatório).
**O que falta e depende de você:** QA manual no celular, os passos nas contas
(Cloudinary, Firebase, token da CLI) e decidir sobre o Pull Request (Seção 7).

## 2. O que mudou

| Antes | Depois |
|---|---|
| Firestore, regras só no app | Postgres com 13 tabelas, **RLS em todas**, 20 funções RPC para as regras de negócio |
| FirebaseAuth e conferência de e-mail no app | Supabase Auth só com Google; `vincular_conta()` liga a conta ao cadastro pelo e-mail verificado |
| Cloudinary com preset aberto e URLs públicas | 3 buckets privados, 3 MB, MIME no servidor, link assinado de 10 minutos |
| Senha do admin no Firestore | Sem senha: o admin é um perfil com `admin = true` no banco |
| Pontuação gravada pelo app | Calculada no servidor (notas e faltas); o aluno não escreve nada disso |
| Ranking calculado aluno por aluno no app | `ranking_turma()` no servidor |
| `addSnapshotListener` (tempo real) | Recarrega ao abrir a tela e depois de cada gravação (decisão N2) |
| Notificações varriam o Firestore | `novidades_desde()` com um marco por usuário |
| Telas falavam direto com o Firestore | Telas → repositórios (`app/.../dados/`) → API REST do Supabase; fakes nos testes |

Telas migradas por módulo: 1 login e sessão; 2 perfis e admin; 3 estrutura escolar; 4 aulas e
frequência; 5 trilhas, entregas e arquivos; 6 diário; 7 desempenho e ranking; 8 notificações;
9 telas iniciais e disciplinas. Saíram `CloudinaryUtil`, `PontuacaoUtil`, `PenalidadeFaltaUtil`,
`AnoLetivoUtil`, `BimestreUtil`, `DisciplinaUtil`, `FotoUtil` e `PonteFirebase`.

## 3. Decisões que moldaram o resultado

D1 a D15 estão em `docs/migracao/inventario.md`. As que mais mudam o uso:

- **D15:** o app aceita **só** login com Google; e-mail e senha desligados em todo projeto.
- **D4:** entrega atrasada é aceita e marcada, com os dias de atraso visíveis.
- **D3:** nota de 0 até os pontos da trilha, validada no servidor.
- **D5:** trilhas e entregas só PDF e imagens.
- **D6:** excluir uma trilha apaga entregas e arquivos, depois de avisar quantas entregas.
- **D7, D12, D13, D14:** exclusão definitiva só pelo admin; o usuário pede dentro do app;
  professor com disciplinas precisa transferi-las antes; nunca se exclui o último admin ativo.
- **D8:** o diário só é visto pelo próprio aluno.
- **D9:** matrícula automática por gatilho ao criar disciplina ou cadastrar aluno.

## 4. Testes e qualidade (números reais, 30/10/2026)

| Verificação | Resultado |
|---|---|
| pgTAP (`supabase test db --local`, 13 arquivos) | **333 testes, todos passando** |
| Testes JVM (`testDebugUnitTest`) | **154 testes, 0 falhas** |
| Testes instrumentados no emulador (Espresso, fakes e ponta a ponta) | **116 testes, todos passando** (16 de ponta a ponta no banco local) |
| `lintDebug` | **0 erros** |
| `supabase db lint --local` | sem erros de esquema |
| `supabase db advisors --local` | nenhum aviso |
| `supabase db advisors --linked` (remoto) | **21 avisos WARN, nenhum ERROR** (ver abaixo) |
| Migrações local × remoto | 8 de 8 iguais |
| APK de debug | monta; 27,7 MB |

**Cobertura JaCoCo dos testes JVM** (linhas): camada de dados `dados/` **94,7%** (1.010 de 1.067),
acima da meta de 80%. Abaixo da meta só a `CifraKeystore` (2,7%), porque o Android Keystore existe
apenas em aparelho; ela é exercitada pelo `CifraKeystoreTest` instrumentado. As telas (pacote
raiz) aparecem com 2,7% nos testes JVM porque são cobertas pelos testes instrumentados, cuja
cobertura não foi medida. O total geral dos testes JVM é 19,7% (1.136 de 5.752).

**Avisos do advisor do remoto.** São 20 avisos "função SECURITY DEFINER executável por usuário
logado", um por RPC (`avaliar_entrega`, `ranking_turma`, `excluir_usuario` etc.), e 1 de "proteção
contra senha vazada desligada". Os 20 são o desenho escolhido: cada função confere o perfil de quem
chama dentro dela (e `anon` não tem permissão em nenhuma; pgTAP prova os casos negativos). O de
senha vazada não se aplica, porque o login por e-mail e senha está desligado. Nenhum merece ação.

## 5. Revisão de segurança, item por item (Seção 1.1 do plano)

| # | Problema original | Como foi resolvido | Evidência |
|---|---|---|---|
| 1 | Arquivos públicos por URL | Buckets `atividades`, `entregas` e `fotos-perfil` **privados**; leitura por política de Storage (dono, professor da disciplina, admin); link assinado de 10 minutos (`Arquivos.VALIDADE_LINK_SEGUNDOS`) | `select id, public from storage.buckets` → todos `false`; pgTAP `100_storage`; E2E `trilhaComArquivos...` (professor de outra disciplina não lê entrega nem anexo); uma leitura sem login responde "Bucket not found" |
| 2 | Upload aberto (preset unsigned) | Cloudinary removido do código; envio ao Storage exige login e passa pelas políticas, com limite de 3 MB e MIME no bucket | `grep` sem nenhuma ocorrência de Cloudinary; bucket com `file_size_limit = 3145728` e MIME permitidos; E2E recusa um tipo `application/x-msdownload`. **Falta você desativar o preset na conta** (Seção 7) |
| 3 | Senha do admin em texto puro | Não existe senha: o admin é `perfis.admin = true`, definido no banco; trocar o ano letivo é a RPC `definir_ano_letivo`, só para admin (decisão N5) | `grep` por senha no app sem resultado; pgTAP `080_ano_letivo` (quem não é admin é recusado); testes de tela do módulo 3 |
| 4 | Autorização só no cliente | RLS ligado em **13 de 13 tabelas** (0 sem RLS), 30 políticas; `anon` sem nenhum privilégio; o app não decide acesso | `pg_class.relrowsecurity` e `pg_policies` no banco local; sem login: tabela → 401, RPC → 401; pgTAP `010_privilegios` a `120` |
| 5 | Pontuação e ranking gravados pelo cliente | Pontuação e ranking calculados no servidor a partir de notas e frequência; `authenticated` só tem `SELECT` em entregas, frequências e perfis (a foto é grant por coluna); nota e frequência só por RPC | `role_table_grants`; pgTAP `050`, `060`, `070`; E2E `notaViraPontos...` (a professora avalia, os pontos sobem; refazer tira) |
| 6 | Vinculação de conta por e-mail no cliente | `vincular_conta()` usa o e-mail **verificado** do token, grava `usuario_id` e devolve o perfil, ou recusa | pgTAP `020_vincular_conta`; teste de tela `TelaEntrar`; conta sem cadastro não fica com sessão |

Outros pontos conferidos:

- **Login só com Google no remoto** (reexecutado hoje): `external.email = false`, `google = true`;
  `POST /token?grant_type=password` → 422 `email_provider_disabled`; `POST /signup` → 400
  `email_provider_disabled`.
- **Sessão fora do backup:** `data_extraction_rules.xml` e `backup_rules.xml` excluem
  `sessao_supabase.xml`; o token de renovação é cifrado com o Android Keystore.
- **Componentes exportados:** só a tela de abertura (`Splash`) é `exported = true`.
- **Segredos no repositório:** nada versionado com `eyJ…`, `sb_secret_` ou e-mails reais.
  `credenciais.txt`, `admins.local.txt`, `local.properties`, `*.local.md` e `*.local.sql` estão
  no `.gitignore`. No app vai só a chave pública, e o build recusa uma chave secreta.

## 6. Limitações e riscos conhecidos

- **QA manual não feito.** Os roteiros dos módulos 2 a 9 e os de rede ruim (modo avião durante
  um envio) e sessão expirada estão em `qa.md` como "pendente". Os testes automáticos cobrem a
  lógica e as telas com dados falsos e reais no banco local, mas não substituem usar o app no
  celular (seletor de arquivos do Android, calendário do prazo, notificação na barra).
- **Login com Google no celular depois de desligar o e-mail** (caso S.4) ainda não foi visto de
  verdade; se falhar, é a primeira coisa a investigar.
- **Sem tempo real** (N2): o diário e as listas atualizam ao abrir a tela e depois de cada gravação.
- **Notificação de avaliação** deixou de mostrar a nota; a função `novidades_desde` não a devolve.
  Mostrar a nota de volta exige uma migração nova.
- **Notificações:** a função devolve no máximo 100 itens por consulta, e o marco usa a hora do
  servidor; na primeira vez de cada usuário o app só marca o ponto de partida.
- **Arquivo órfão no Storage:** se a gravação da entrega falhar depois do envio do arquivo, o aluno
  não tem permissão de apagá-lo. Ele sai quando a trilha for excluída.
- **Rede:** a CLI do Supabase usa as portas 5432/6543, bloqueadas em algumas redes (use o roteador do
  celular para `db push` e `migration list`).
- **Ordem dos testes:** o pgTAP e os de ponta a ponta precisam de banco local recém-zerado; um teste
  de ponta a ponta exclui o usuário "Rui" do seed, então nenhum outro pode depender dele.
- **Histórico do Git:** o `google-services.json` antigo continua no histórico do repositório público
  (só identificadores e a chave de API do Firebase). Limpar exige reescrever o histórico.
- **Produção:** existe um só projeto Supabase (desenvolvimento). Para produção, criar outro projeto,
  aplicar as migrações, desligar o e-mail nele e cadastrar o Web Client ID (ver `passos-do-usuario.md`).

## 7. Pendências suas

Detalhes em `docs/migracao/passos-do-usuario.md`.

1. Reinstalar o app no celular (`adb install -r --user 0`) e fazer o QA manual de `qa.md`.
2. Testar o login com Google no celular (S.4).
3. Cloudinary: desativar o preset `meufuturoagora_unsigned` e apagar os arquivos de teste.
4. Firebase: desativar Authentication, Firestore e Storage. **Não excluir** o projeto do Google
   Cloud `meufuturoagora-79b76` (os clientes OAuth do login estão nele).
5. Revogar o token de acesso da CLI do Supabase.
6. Decidir se o `PROMPT_MIGRACAO_SUPABASE.md` (alteração sem commit) entra no repositório.
7. Decidir sobre o Pull Request de `migracao-supabase` para `main` (nada foi enviado ao GitHub).
