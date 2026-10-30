# Passos que só a pessoa responsável pode fazer

O código e o build do app já não usam Firebase nem Cloudinary (Fase 6). O que sobra
está em contas e painéis, e nada abaixo foi feito por mim.

## 1. Cloudinary

1. Entre no painel do Cloudinary → **Settings → Upload → Upload presets**.
2. Desative (ou apague) o preset `meufuturoagora_unsigned`. Sem isso, qualquer pessoa que
   conheça o nome da conta ainda consegue enviar arquivos.
3. Em **Media Library**, apague os arquivos de teste das pastas `atividades`, `entregas` e das
   fotos de perfil. Eles eram públicos pela URL, então vale apagar mesmo sendo de teste.
4. Se não for usar mais a conta, apague a chave de API e feche a conta.

## 2. Firebase

1. **Não exclua o projeto do Google Cloud `meufuturoagora-79b76`.** Os clientes OAuth do
   login com Google estão nele (o Web Client ID, ligado ao Supabase, e o cliente Android com
   o SHA-1).
2. No console do Firebase desse projeto, dá para desativar com segurança: Authentication,
   Firestore, Storage e Analytics. Antes de apagar o banco do Firestore, confirme que não
   há nada que você queira guardar (a migração partiu do princípio de que tudo era de teste).
3. Só depois de uma semana de uso sem problemas, considere remover o app Firebase do projeto.

## 3. Chaves e tokens

- Revogue o **token de acesso da CLI do Supabase** em
  <https://supabase.com/dashboard/account/tokens> quando terminar as publicações no banco.
- O `google-services.json` saiu do código, mas a cópia antiga continua no histórico do Git
  (o repositório do GitHub é público). Ela só tem identificadores e a chave de API do
  Firebase, que não dão acesso aos dados, e o Firestore agora não é usado. Se quiser
  limpar mesmo assim, é preciso reescrever o histórico, e isso você decide.

## 4. Testes no celular

- Reinstale o app no celular (`adb install -r --user 0`, por causa do Dual Messenger do
  Samsung) para ter os módulos 3 a 9 e faça o QA manual de `docs/migracao/qa.md`.
- Teste o login com Google depois de o e-mail ter sido desligado (caso S.4).
