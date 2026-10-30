# QA manual da migração

Roteiro da Seção 5.5 do plano. Cada módulo da Fase 5 acrescenta as suas linhas.
Resultado: **ok**, **falha** (com o que aconteceu) ou **pendente**.

Ambiente: projeto Supabase de desenvolvimento (São Paulo), app em debug.

## Módulo 1: login e sessão

Durante a Fase 5, o app também entra no Firebase com o mesmo token do Google
(`PonteFirebase`), para as telas ainda não migradas continuarem funcionando.
Quem decide o acesso é só o Supabase.

| # | Perfil | Ação | Esperado | Obtido | Resultado |
|---|---|---|---|---|---|
| 1.1 | Admin (e-mail do `admins.local.txt`) | Abrir o app pela primeira vez e entrar com o Google | Vai para a tela inicial do professor; no Supabase, o perfil fica com `usuario_id` preenchido | Entrou na tela do professor (Galaxy S25+, Android 17); perfil ligado e foto do Google gravada | ok |
| 1.2 | Admin | Fechar e abrir o app de novo | Entra direto, sem pedir login | Continuou logado depois de fechar o app | ok |
| 1.3 | Conta Google sem cadastro | Entrar com o Google | Mensagem "Seu e-mail não está autorizado…" e continua na tela de login | | pendente |
| 1.4 | Qualquer | Tocar em "Entrar com o Google" e fechar a janela de contas | Nada acontece, sem mensagem de erro; botão volta ao normal | | pendente |
| 1.5 | Qualquer | Tocar duas vezes rápido em "Entrar com o Google" | Abre uma janela só ("Entrando...") | | pendente |
| 1.6 | Admin | Configurações → Sair da conta → Sair | Volta para a tela de login; ao reabrir o app, pede login de novo | Voltou para o login e entrou de novo normalmente | ok |
| 1.7 | Admin | Modo avião, abrir o app com sessão salva | Mensagem de sem conexão e tela de login; ao voltar a rede e reabrir, entra direto | Como esperado | ok |
| 1.8 | Aluno cadastrado pelo admin | Entrar com o Google | Vai para a tela inicial do aluno | | pendente (depende do módulo 2 para cadastrar) |
| 1.9 | Admin desativado no painel | Reabrir o app | Volta para o login com a mensagem de não autorizado | | pendente |

Testes automáticos que cobrem o roteamento com dados falsos: `LoginTest` e
`TelaEntrarTest` (instrumentados), e a camada de dados nos testes JVM.

### Achados do QA do módulo 1

- **Texto do botão do Google quebrava em duas linhas** no Galaxy S25+ (22sp e
  65dp de margem não cabiam em ~384dp). Corrigido: 18sp, margens de 40dp e uma
  linha só.
- **App instalado duas vezes** em aparelhos Samsung com Dual Messenger: o
  `installDebug` instala também no perfil DUAL_APP (usuário 95). Para testar,
  instalar só no perfil principal: `adb install -r --user 0 app-debug.apk`.
- **Ranking demora ~3 s e mostra o pódio vazio com 0 pontos enquanto carrega**
  (tela ainda no Firestore). Causa: a tela recalcula a pontuação de cada aluno
  da turma no celular, aluno por aluno, antes de mostrar. Fica para o módulo 7
  (desempenho e ranking): uma única chamada a `ranking_turma` calculada no
  servidor, com indicador de carregamento e o pódio escondido até os dados
  chegarem.

## Módulo 2: perfis e área do admin

| # | Perfil | Ação | Esperado | Obtido | Resultado |
|---|---|---|---|---|---|
| 2.1 | Admin | Perfil do professor | Nome do cadastro, e-mail e o botão "Área do administrador" | | pendente |
| 2.2 | Admin | Tocar na foto e escolher outra imagem | "Foto de perfil atualizada!"; a foto nova continua depois de reabrir o app | | pendente |
| 2.3 | Admin | Área do admin → Cadastrar usuário → Turma "6º ano A", ano 6 | "Turma cadastrada!" | | pendente |
| 2.4 | Admin | Cadastrar a mesma turma de novo ("6º ANO A") | "Já existe uma turma com esse nome." | | pendente |
| 2.5 | Admin | Cadastrar aluno com uma segunda conta Google, na turma 6º ano A | "Cadastrado!" | | pendente |
| 2.6 | Admin | Cadastrar o mesmo e-mail de novo, com outro nome | Pergunta se quer atualizar; ao confirmar, "Cadastro atualizado!" | | pendente |
| 2.7 | Admin | Lista de usuários | Professores e a turma com o aluno, marcado "(ainda não entrou)" | | pendente |
| 2.8 | Aluno (segunda conta) | Entrar com o Google (caso 1.8) | Vai para a tela inicial do aluno; o perfil mostra o nome e a turma | | pendente |
| 2.9 | Aluno | Configurações → Solicitar exclusão dos meus dados → Enviar | "Pedido enviado…"; mandar de novo dá "Você já tem um pedido…" | | pendente |
| 2.10 | Admin | Lista de usuários → Pedidos de exclusão → Recusar | O pedido some da lista | | pendente |
| 2.11 | Admin | Lista de usuários → aluno → Excluir → Excluir | O aluno some; ao tentar entrar com a conta dele, acesso recusado | | pendente |
| 2.12 | Admin | Excluir a si mesmo (único admin) | "É preciso haver ao menos um administrador ativo." | | pendente |

## Testes de ponta a ponta no Supabase local

`E2eSupabaseLocalTest`: telas de verdade, camada de dados de verdade e o banco
local (RLS, gatilhos, funções), com as contas fictícias do `supabase/seed.sql`
(senha `senha-local-de-teste`, só no banco local). Entram com e-mail e senha
pelo código de teste; o app continua só com Google. Cada teste salva capturas
de tela em `/sdcard/Android/data/br.com.example.meufuturoagora/files/qa/`.

Como rodar (emulador ligado, Supabase local no ar):

```
supabase db reset --local
ANDROID_SERIAL=emulator-5554 ./gradlew :app:installDebug :app:installDebugAndroidTest
adb shell am instrument -w -r -e supabaseChaveLocal <PUBLISHABLE_KEY do supabase status> \
  br.com.example.meufuturoagora.test/androidx.test.runner.AndroidJUnitRunner
adb pull /sdcard/Android/data/br.com.example.meufuturoagora/files/qa/ .
```

Pelo `adb` e não pelo `connectedAndroidTest`, que desinstala o app no fim e
apaga as capturas. Sem a chave local, esses testes são pulados.

### Resultado em 29/10/2026 (módulos 1 e 2)

31 testes de tela passando (7 de ponta a ponta). Capturas conferidas: perfil
da professora, da aluna (turma e matrícula automática), do admin, área do
admin (ano e bimestre), cadastro, pedido de exclusão e lista antes e depois
da exclusão. Achados, já corrigidos:

- Indicador padrão da lista expansível desenhado por cima do título dos
  grupos em "Usuários" (já existia antes da migração).
- "(1 alunos)" no título da turma.

## Módulo 3: estrutura escolar

Telas: Definir ano letivo, Definir bimestres, Criar disciplina (professor) e
Histórico dos anos letivos (admin). Telas que ainda leem o Firestore (ranking,
telas iniciais, trilhas) continuam com a configuração de ano antiga até o
módulo delas: o ano definido aqui vale para o Supabase.

| # | Perfil | Ação | Esperado | Obtido | Resultado |
|---|---|---|---|---|---|
| 3.1 | Admin | Área do admin → Definir ano letivo; digitar o ano atual e salvar | "Esse já é o ano letivo atual." | | pendente |
| 3.2 | Admin | Digitar um ano novo (ex.: 2027) e salvar | Janela de confirmação **sem campo de senha**; ao confirmar, "Ano letivo alterado!" | | pendente |
| 3.3 | Admin | Voltar para a área do admin | Mostra "Ano letivo 2027" e "Fora de bimestre / não definido" | | pendente |
| 3.4 | Admin | Definir bimestres: preencher só o início do 1º e salvar | "Informe o início e o fim do 1º bimestre." | | pendente |
| 3.5 | Admin | Preencher 1º e 2º bimestres sobrepostos e salvar | Aviso de ordem e sobreposição; nada é gravado | | pendente |
| 3.6 | Admin | Preencher os 4 bimestres válidos e salvar | "Bimestres salvos!"; ao reabrir a tela, as datas voltam | | pendente |
| 3.7 | Admin | Definir o ano letivo de volta para 2026 | Os bimestres de 2026 voltam; a pontuação do ano atual volta a contar | | pendente |
| 3.8 | Admin | Histórico → conferir ano, bimestre e turma | Lista de anos com "(atual)"; trocar o bimestre mostra "(dd/mm/aaaa a dd/mm/aaaa)" | | pendente |
| 3.9 | Admin | Histórico → abas Alunos, Trilhas e Entregas | Entregas atrasadas mostram "N dia(s) de atraso"; pendentes mostram "Pendente" | | pendente |
| 3.10 | Admin | Histórico → pesquisar um nome | A lista filtra sem recarregar | | pendente |
| 3.11 | Professor | Perfil → Disciplinas → criar disciplina numa turma | "Disciplina criada!"; os alunos da turma já enxergam (sem novo login) | | pendente |
| 3.12 | Professor comum | Tentar abrir o histórico do admin | Não há botão da área do admin; o servidor também recusa (403) | | pendente |

Testes automáticos: pgTAP `110_historico` (22 testes), `BimestresTest`,
`HistoricoRepositorioTest`, `EstruturaEscolarTest` (repositórios falsos) e 3
testes novos em `E2eSupabaseLocalTest`. Os de tela ainda **não rodaram**
(emulador desligado por falta de memória).

## Módulo 4: aulas e frequência

| # | Perfil | Ação | Esperado | Obtido | Resultado |
|---|---|---|---|---|---|
| 4.1 | Professor | Disciplina → Cadastrar aula: quantidade 2, data, descrição → Salvar | "Aula cadastrada!" | | pendente |
| 4.2 | Professor | Cadastrar aula com quantidade 0 ou 21 | Erro "Digite de 1 a 20 aulas"; nada é gravado | | pendente |
| 4.3 | Professor | Cadastrar aula com a data 31/02/2026 (digitada) | "Data inválida" | | pendente |
| 4.4 | Professor | Registrar frequência → lista de aulas | Aulas com a descrição e a data em dd/mm/aaaa, as mais recentes primeiro | | pendente |
| 4.5 | Professor | Abrir uma aula | Só os alunos (sem o professor), nome e foto; todos em "Presente" na primeira vez | | pendente |
| 4.6 | Professor | Marcar um aluno como "Falta" e salvar | "Frequência salva!"; o aluno perde 5 pontos no ranking | | pendente |
| 4.7 | Professor | Marcar o mesmo aluno como "Falta justificada" e salvar | Os 5 pontos voltam | | pendente |
| 4.8 | Professor | Duas faltas do mesmo aluno no mesmo dia (duas aulas) | Desconta 5 pontos uma vez só | | pendente |
| 4.9 | Professor | Reabrir uma aula já salva | Os status salvos aparecem, não tudo "Presente" | | pendente |
| 4.10 | Professor | Excluir uma aula com faltas | Aviso de que a frequência também é apagada; depois o aluno recupera os pontos | | pendente |
| 4.11 | Professor | Sem internet, salvar a frequência | Mensagem de erro e o botão volta a funcionar (antes mostrava "salva" mesmo falhando) | | pendente |
| 4.12 | Outro professor | Tentar excluir ou registrar frequência numa aula alheia | Recusado (403) | | pendente |

## Módulo 5: trilhas, entregas e arquivos

Telas: lista de trilhas do professor e do aluno, Criar trilha, Entregas da trilha,
Avaliar entrega e a tela da trilha do aluno. Os arquivos vão para os buckets
privados `atividades` e `entregas`; para abrir, o app pede um link assinado de 10 minutos.
O `CloudinaryUtil` saiu do código.

Automático (29–30/10/2026): 15 testes de tela (`AtividadesEEntregasTest`), 1 de ponta a
ponta no Storage e no RLS locais (anexo, entrega com arquivo, tipo recusado, outro
professor sem acesso, nota fora da escala recusada, exclusão que limpa o Storage) e 6 de
JVM do `ArquivoUtil`. Fora do automático: a escolha do arquivo no seletor do Android e o
calendário do prazo.

| # | Perfil | Ação | Esperado | Obtido | Resultado |
|---|---|---|---|---|---|
| 5.1 | Professor | Disciplina → "+" → criar trilha com título, 1 a 10 pontos, prazo e um PDF | "Trilha criada!"; a trilha aparece na lista | | pendente |
| 5.2 | Professor | Anexar um arquivo que não é PDF nem imagem (ex.: .zip) ou maior que 3 MB | O seletor só oferece PDF e imagens; arquivo grande é recusado com aviso | | pendente |
| 5.3 | Professor | Editar a trilha e trocar o anexo | "Trilha atualizada!"; o anexo antigo some do Storage; a disciplina fica travada | | pendente |
| 5.4 | Aluno | Abrir a trilha e tocar no anexo do professor | O arquivo abre (link assinado) | | pendente |
| 5.5 | Aluno | Entregar só com comentário; depois com arquivo (outra trilha) | Aviso "não poderá ser modificada"; "Entrega enviada!" | | pendente |
| 5.6 | Aluno | Entregar sem comentário e sem arquivo | Pede comentário ou arquivo; nada é enviado | | pendente |
| 5.7 | Aluno | Reabrir uma trilha já entregue | Campos travados, sem botão de enviar | | pendente |
| 5.8 | Aluno | Entregar depois do prazo | Aceita e mostra "(N dias de atraso)" no status | | pendente |
| 5.9 | Professor | Entregas da trilha → alunos | Só alunos, com foto e "Entregue", "Avaliado" ou "Não entregue" | | pendente |
| 5.10 | Professor | Abrir a entrega, tocar no anexo do aluno | O arquivo abre | | pendente |
| 5.11 | Professor | Avaliar com nota 7,5 (vírgula) numa trilha de 10 pontos; depois com nota 11 | A primeira é salva; a segunda é recusada com a mensagem do servidor | | pendente |
| 5.12 | Aluno | Ver a entrega avaliada | Mostra "Avaliada", a nota e o comentário do professor | | pendente |
| 5.13 | Professor | Menu da entrega avaliada → Refazer avaliação | Nota antiga some do ranking; abre a tela para dar a nova nota | | pendente |
| 5.14 | Professor | Excluir uma trilha com entregas | O aviso diz quantas entregas e arquivos serão apagados; depois some tudo | | pendente |
| 5.15 | Outro professor | Tentar abrir/baixar arquivos de uma trilha alheia | Recusado | | pendente |

## Módulo 6: diário do aluno

Sem tempo real (N2): a lista recarrega ao abrir a tela e depois de cada gravação. Não há
"puxar para atualizar" (a tela não tem esse componente). Automático (30/10/2026): 7 testes
de tela (`DiarioTest`) e 1 de ponta a ponta (`diarioEPrivadoDoDono...`: professor e admin
não leem o diário do aluno). Aluno contra aluno: pgTAP 090.

| # | Perfil | Ação | Esperado | Obtido | Resultado |
|---|---|---|---|---|---|
| 6.1 | Aluno | Abrir o Diário | "Oi, <primeiro nome>!"; páginas da mais nova para a mais antiga, com a data por extenso | | pendente |
| 6.2 | Aluno | Escrever e guardar uma página | "Guardado no seu diário"; o campo limpa e a página aparece | | pendente |
| 6.3 | Aluno | Guardar sem internet | Aviso de erro; o texto continua no campo (antes parecia guardado e se perdia) | | pendente |
| 6.4 | Aluno | Tocar numa página, editar e salvar | O texto novo aparece | | pendente |
| 6.5 | Aluno | Apagar uma página | Pede confirmação; depois a página some | | pendente |
| 6.6 | Aluno | Fechar e reabrir o app | As páginas continuam lá | | pendente |

## Segurança: login só com Google (decisão D15)

Conferir sempre que mexer em autenticação, na revisão final (Fase 7) e ao criar o
projeto de produção. Não precisa de login.

| # | Verificação | Esperado | Resultado |
|---|---|---|---|
| S.1 | `curl -H "apikey: <chave pública>" https://<ref>.supabase.co/auth/v1/settings` | `"external": {"email": false, "google": true, "phone": false, "anonymous_users": false}` | ok em 29/10/2026 |
| S.2 | `POST /auth/v1/token?grant_type=password` com qualquer e-mail e senha | `422 email_provider_disabled` | ok em 29/10/2026 |
| S.3 | `POST /auth/v1/signup` com e-mail e senha | `400 email_provider_disabled` | ok em 29/10/2026 |
| S.4 | No celular: Entrar com o Google com a conta do admin | Entra normalmente (o Google segue ligado) | **pendente** (testar amanhã) |

Nunca rodar `supabase config push`: o `config.toml` local liga e-mail para os testes.
