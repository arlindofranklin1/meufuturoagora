-- RLS de perfis: quem vê quem, quem altera o quê (D10, D11, D14).
begin;
select * from no_plan();
select testes.cenario();

-- ===== Leitura =====

select testes.entrar_como('a1');
select results_eq('select id from public.perfis', array[testes.id('a1')],
  'aluno vê só o próprio perfil');

select testes.entrar_como('p1');
select set_eq('select id from public.perfis',
  array[testes.id('p1'), testes.id('a1'), testes.id('a2'), testes.id('a3')],
  'professor vê o próprio perfil e os alunos das suas disciplinas');

select testes.entrar_como('p2');
select set_eq('select id from public.perfis',
  array[testes.id('p2'), testes.id('a1'), testes.id('a2')],
  'professor não vê alunos de turmas em que não dá aula');

select testes.entrar_como('adm');
select is((select count(*)::int from public.perfis), 6, 'admin vê todos os perfis');

-- ===== Alterações pelo próprio usuário =====

select testes.entrar_como('a1');
select lives_ok(
  format('update public.perfis set foto_path = %L where id = %L',
         testes.id('a1')::text || '/foto.jpg', testes.id('a1')),
  'aluno troca a própria foto');
select throws_ok(
  format('update public.perfis set nome = %L where id = %L', 'Outro Nome', testes.id('a1')),
  '42501', null, 'aluno não troca o próprio nome');
select throws_ok(
  format('update public.perfis set turma_id = %L where id = %L', testes.id('turma_b'), testes.id('a1')),
  '42501', null, 'aluno não troca a própria turma');
select throws_ok(
  format('update public.perfis set foto_path = %L where id = %L', 'outra-pasta/foto.jpg', testes.id('a1')),
  '23514', null, 'a foto precisa ficar na pasta do próprio perfil');

-- Alterar o perfil de outra pessoa não afeta nenhuma linha (RLS)
update public.perfis set foto_path = null where id = testes.id('a2');
set local role postgres;
select is((select count(*)::int from public.perfis where id = testes.id('a2') and foto_path is null), 1,
  'perfil de outro aluno continua igual (a foto já era nula)');

select testes.entrar_como('a2');
update public.perfis set foto_path = testes.id('a1')::text || '/x.jpg' where id = testes.id('a1');
set local role postgres;
select is((select foto_path from public.perfis where id = testes.id('a1')),
  testes.id('a1')::text || '/foto.jpg', 'aluno não altera o perfil de outro aluno');

-- Ninguém vira admin sozinho
select testes.entrar_como('p1');
select throws_ok(
  format('update public.perfis set admin = true where id = %L', testes.id('p1')),
  '42501', null, 'professor não se torna admin alterando o próprio perfil');
select testes.entrar_como('a1');
select throws_ok(
  format('update public.perfis set admin = true where id = %L', testes.id('a1')),
  '42501', null, 'aluno não se torna admin alterando o próprio perfil');
select throws_ok('update public.perfis set usuario_id = null', '42501', null,
  'ninguém altera usuario_id pela API');

-- ===== Cadastro pelo admin =====

select testes.entrar_como('p1');
select throws_ok(
  $$insert into public.perfis (email, nome, tipo) values ('x@escola.test', 'X', 'aluno')$$,
  '42501', null, 'professor não cadastra usuários');

select testes.entrar_como('adm');
select lives_ok(
  format('insert into public.perfis (email, nome, tipo, turma_id) values (%L, %L, %L, %L)',
         'novo@escola.test', 'Novo', 'aluno', testes.id('turma_a')),
  'admin cadastra aluno');
select throws_ok(
  $$insert into public.perfis (email, nome, tipo) values ('NOVO@Escola.TEST', 'Repetido', 'professor')$$,
  '23505', null, 'D10: e-mail repetido (sem diferenciar maiúsculas) é recusado');
select throws_ok(
  format('insert into public.perfis (email, nome, tipo, admin, turma_id) values (%L, %L, %L, true, %L)',
         'aluno.admin@escola.test', 'Aluno Admin', 'aluno', testes.id('turma_a')),
  '23514', null, 'D11: aluno não pode ser admin');
select throws_ok(
  $$insert into public.perfis (email, nome, tipo, turma_id) values ('p9@escola.test', 'P', 'professor', gen_random_uuid())$$,
  '23514', null, 'professor não tem turma');
select throws_ok(
  $$insert into public.perfis (email, nome, tipo) values ('sem-arroba', 'X', 'aluno')$$,
  '23514', null, 'e-mail inválido é recusado');
select lives_ok(
  format('update public.perfis set admin = true where id = %L', testes.id('p1')),
  'admin torna um professor admin');
select throws_ok(format('delete from public.perfis where id = %L', testes.id('a2')),
  '42501', null, 'nem o admin apaga perfil direto pela API (só por excluir_usuario)');

-- ===== D14: sempre um admin ativo =====

set local role postgres;
update public.perfis set admin = false where id = testes.id('p1');
select testes.entrar_como('adm');
select throws_ok(
  format('update public.perfis set admin = false where id = %L', testes.id('adm')),
  'P0001', null, 'D14: o último admin não deixa de ser admin');
select throws_ok(
  format('update public.perfis set ativo = false where id = %L', testes.id('adm')),
  'P0001', null, 'D14: o último admin não é desativado');

-- ===== Perfil desativado perde o acesso =====

set local role postgres;
update public.perfis set ativo = false where id = testes.id('a1');
select testes.entrar_como('a1');
select is((select count(*)::int from public.perfis), 0, 'aluno desativado não vê nem o próprio perfil');
select is((select count(*)::int from public.disciplinas), 0, 'aluno desativado não vê disciplinas');

select * from finish();
rollback;
