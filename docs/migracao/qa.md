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
| 1.2 | Admin | Fechar e abrir o app de novo | Entra direto, sem pedir login | | pendente |
| 1.3 | Conta Google sem cadastro | Entrar com o Google | Mensagem "Seu e-mail não está autorizado…" e continua na tela de login | | pendente |
| 1.4 | Qualquer | Tocar em "Entrar com o Google" e fechar a janela de contas | Nada acontece, sem mensagem de erro; botão volta ao normal | | pendente |
| 1.5 | Qualquer | Tocar duas vezes rápido em "Entrar com o Google" | Abre uma janela só ("Entrando...") | | pendente |
| 1.6 | Admin | Configurações → Sair da conta → Sair | Volta para a tela de login; ao reabrir o app, pede login de novo | | pendente |
| 1.7 | Admin | Modo avião, abrir o app com sessão salva | Mensagem de sem conexão e tela de login; ao voltar a rede e reabrir, entra direto | | pendente |
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
