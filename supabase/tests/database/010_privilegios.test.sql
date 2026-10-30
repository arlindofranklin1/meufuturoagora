-- Privilégios gerais: RLS em tudo, anon sem acesso, e objetos novos sem grant
-- automático (passo 0 da Fase 3 do plano).
begin;
select * from no_plan();

-- RLS habilitado em todas as tabelas de public
select is(
  (select count(*)::int from pg_class c
   where c.relnamespace = 'public'::regnamespace and c.relkind in ('r', 'p')
     and not c.relrowsecurity),
  0,
  'todas as tabelas de public têm RLS habilitado'
);

-- anon não tem nenhum privilégio em nenhuma tabela de public
select is(
  (select count(*)::int from pg_class c
   cross join unnest(array['SELECT', 'INSERT', 'UPDATE', 'DELETE', 'TRUNCATE', 'REFERENCES', 'TRIGGER']) as p (privilegio)
   where c.relnamespace = 'public'::regnamespace and c.relkind in ('r', 'p', 'v', 'm')
     and has_table_privilege('anon', c.oid, p.privilegio)),
  0,
  'anon não tem privilégio em tabelas de public'
);

-- Ninguém da API tem TRUNCATE, REFERENCES ou TRIGGER
select is(
  (select count(*)::int from pg_class c
   cross join unnest(array['TRUNCATE', 'REFERENCES', 'TRIGGER']) as p (privilegio)
   where c.relnamespace = 'public'::regnamespace and c.relkind = 'r'
     and has_table_privilege('authenticated', c.oid, p.privilegio)),
  0,
  'authenticated não tem truncate, references nem trigger'
);

-- anon não executa nenhuma função de public nem de privado
select is(
  (select count(*)::int from pg_proc p
   where p.pronamespace in ('public'::regnamespace, 'privado'::regnamespace)
     and has_function_privilege('anon', p.oid, 'execute')),
  0,
  'anon não executa funções de public nem de privado'
);

select ok(not has_schema_privilege('anon', 'privado', 'usage'), 'anon não usa o esquema privado');

-- Funções internas (pontuação, exclusão) não são chamáveis pela API
select ok(not has_function_privilege('authenticated', 'privado.excluir_perfil(uuid, uuid)', 'execute'),
  'authenticated não chama privado.excluir_perfil');
select ok(not has_function_privilege('authenticated', 'privado.pontuacao_aluno(uuid, smallint)', 'execute'),
  'authenticated não chama privado.pontuacao_aluno');
select ok(not has_function_privilege('authenticated', 'privado.exigir_admin()', 'execute'),
  'authenticated não chama privado.exigir_admin');

-- Toda função security definer fixa o search_path
select is(
  (select count(*)::int from pg_proc p
   where p.pronamespace in ('public'::regnamespace, 'privado'::regnamespace)
     and p.prosecdef
     and not exists (select 1 from unnest(p.proconfig) c where c like 'search_path=%')),
  0,
  'funções security definer têm search_path fixo'
);

-- Objeto criado depois das migrações, sem grant, não fica acessível
create table public.tabela_nova_sem_grant (id int);
create function public.funcao_nova_sem_grant() returns int language sql as 'select 1';

select ok(not has_table_privilege('anon', 'public.tabela_nova_sem_grant', 'select'),
  'tabela nova: anon não lê');
select ok(not has_table_privilege('authenticated', 'public.tabela_nova_sem_grant', 'select'),
  'tabela nova: authenticated não lê');
select ok(not has_table_privilege('authenticated', 'public.tabela_nova_sem_grant', 'insert'),
  'tabela nova: authenticated não grava');
select ok(not has_function_privilege('anon', 'public.funcao_nova_sem_grant()', 'execute'),
  'função nova: anon não executa');
select ok(not has_function_privilege('authenticated', 'public.funcao_nova_sem_grant()', 'execute'),
  'função nova: authenticated não executa');

-- E na prática, pela "API"
select testes.cenario();
select testes.entrar_como('a1');
select throws_ok('select * from public.tabela_nova_sem_grant', '42501', null,
  'tabela nova: leitura pela API é negada');
select throws_ok('select public.funcao_nova_sem_grant()', '42501', null,
  'função nova: chamada pela API é negada');

-- anon não lê nada, nem com o cenário populado
select testes.anonimo();
select throws_ok('select * from public.perfis', '42501', null, 'anon não lê perfis');
select throws_ok('select * from public.entregas', '42501', null, 'anon não lê entregas');
select throws_ok('select public.vincular_conta()', '42501', null, 'anon não chama vincular_conta');
select throws_ok('select * from public.ranking_turma(null)', '42501', null, 'anon não chama ranking_turma');
select is((select count(*)::int from storage.objects), 0, 'anon não vê arquivos');

select * from finish();
rollback;
