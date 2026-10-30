-- Apoio para os testes pgTAP (supabase test db). Os arquivos rodam em ordem
-- alfabética, e este cria o esquema "testes" no banco LOCAL. Nenhuma migração
-- cria isto, então nada daqui vai para o projeto remoto.
--
-- Cada arquivo de teste chama testes.cenario() dentro de begin/rollback: os
-- dados do cenário somem no fim de cada arquivo.

create extension if not exists pgtap with schema extensions;

create schema if not exists testes;
grant usage on schema testes to anon, authenticated;

-- nome curto -> id, para os testes não repetirem uuids
create table if not exists testes.ids (nome text primary key, id uuid not null);

create or replace function testes.id(p_nome text)
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select i.id from testes.ids i where i.nome = p_nome
$$;

create or replace function testes.guardar(p_nome text, p_id uuid)
returns uuid
language sql
security definer
set search_path = ''
as $$
  insert into testes.ids (nome, id) values (p_nome, p_id)
  on conflict (nome) do update set id = excluded.id
  returning id
$$;

-- Conta de login como o Supabase Auth cria depois do login com Google
create or replace function testes.criar_usuario(
  p_nome text,
  p_email text,
  p_meta jsonb default '{}',
  p_confirmado boolean default true
)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_id uuid := gen_random_uuid();
begin
  insert into auth.users (instance_id, id, aud, role, email, email_confirmed_at,
                          raw_app_meta_data, raw_user_meta_data, created_at, updated_at)
  values ('00000000-0000-0000-0000-000000000000', v_id, 'authenticated', 'authenticated',
          p_email, case when p_confirmado then now() end,
          '{"provider": "google", "providers": ["google"]}', p_meta, now(), now());

  return testes.guardar('u_' || p_nome, v_id);
end;
$$;

-- Simula uma requisição da API com o token da conta (como o PostgREST faz)
create or replace function testes.entrar_como(p_nome text)
returns void
language plpgsql
set search_path = ''
as $$
begin
  perform set_config('request.jwt.claims',
    json_build_object('sub', testes.id('u_' || p_nome), 'role', 'authenticated')::text, true);
  perform set_config('role', 'authenticated', true);
end;
$$;

create or replace function testes.anonimo()
returns void
language plpgsql
set search_path = ''
as $$
begin
  perform set_config('request.jwt.claims', '{"role": "anon"}', true);
  perform set_config('role', 'anon', true);
end;
$$;

-- Escola fictícia:
--   turma A: alunos a1, a2      turma B: aluno a3
--   p1 dá d1 (turma A) e d3 (turma B); p2 dá d2 (turma A); adm é professor e admin
--   t1: trilha de d1, 10 pontos, prazo daqui a 7 dias
--   Ano letivo 2026 atual; hoje cai no 4º bimestre (datas relativas a hoje,
--   para o teste não depender do dia em que roda)
create or replace function testes.cenario()
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_hoje date := privado.hoje();
begin
  -- Os testes contam linhas: começam sem os dados do seed (o rollback do fim
  -- de cada arquivo traz o seed de volta)
  truncate public.anos_letivos, public.turmas, public.perfis, public.exclusoes_realizadas cascade;

  insert into public.anos_letivos (ano, atual) values (2026, true);
  insert into public.bimestres (ano, numero, inicio, fim) values
    (2026, 1, v_hoje - 200, v_hoje - 150),
    (2026, 2, v_hoje - 149, v_hoje - 100),
    (2026, 3, v_hoje - 99, v_hoje - 50),
    (2026, 4, v_hoje - 10, v_hoje + 30);

  perform testes.guardar('turma_a', gen_random_uuid());
  perform testes.guardar('turma_b', gen_random_uuid());
  insert into public.turmas (id, nome, serie) values
    (testes.id('turma_a'), '6º ano A', 6),
    (testes.id('turma_b'), '6º ano B', 6);

  perform testes.criar_usuario('adm', 'adm@escola.test');
  perform testes.criar_usuario('p1', 'p1@escola.test');
  perform testes.criar_usuario('p2', 'p2@escola.test');
  perform testes.criar_usuario('a1', 'a1@escola.test');
  perform testes.criar_usuario('a2', 'a2@escola.test');
  perform testes.criar_usuario('a3', 'a3@escola.test');

  insert into public.perfis (id, usuario_id, email, nome, tipo, admin, turma_id) values
    (testes.guardar('adm', gen_random_uuid()), testes.id('u_adm'), 'adm@escola.test', 'Coordenação', 'professor', true, null),
    (testes.guardar('p1', gen_random_uuid()), testes.id('u_p1'), 'p1@escola.test', 'Paula', 'professor', false, null),
    (testes.guardar('p2', gen_random_uuid()), testes.id('u_p2'), 'p2@escola.test', 'Pedro', 'professor', false, null),
    (testes.guardar('a1', gen_random_uuid()), testes.id('u_a1'), 'a1@escola.test', 'Ana', 'aluno', false, testes.id('turma_a')),
    (testes.guardar('a2', gen_random_uuid()), testes.id('u_a2'), 'a2@escola.test', 'Bruno', 'aluno', false, testes.id('turma_a')),
    (testes.guardar('a3', gen_random_uuid()), testes.id('u_a3'), 'a3@escola.test', 'Carla', 'aluno', false, testes.id('turma_b'));

  insert into public.disciplinas (id, nome, turma_id, professor_id) values
    (testes.guardar('d1', gen_random_uuid()), 'Matemática', testes.id('turma_a'), testes.id('p1')),
    (testes.guardar('d2', gen_random_uuid()), 'Português', testes.id('turma_a'), testes.id('p2')),
    (testes.guardar('d3', gen_random_uuid()), 'Ciências', testes.id('turma_b'), testes.id('p1'));

  insert into public.atividades (id, disciplina_id, nome, pontos, prazo) values
    (testes.guardar('t1', gen_random_uuid()), testes.id('d1'), 'Frações', 10, v_hoje + 7);
end;
$$;

grant execute on function testes.id(text), testes.guardar(text, uuid), testes.entrar_como(text), testes.anonimo()
  to anon, authenticated;

begin;
select plan(1);
select has_function('testes', 'cenario', 'funções de apoio dos testes criadas');
select * from finish();
rollback;
