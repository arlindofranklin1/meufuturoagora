-- Excluir aula: só o professor da disciplina e o admin; a frequência vai junto
-- e o desconto por falta daquele dia some da pontuação.
begin;
select * from no_plan();
select testes.cenario();

insert into public.aulas (id, disciplina_id, data, quantidade) values
  (testes.guardar('aula1', gen_random_uuid()), testes.id('d1'), privado.hoje() - 3, 1),
  (testes.guardar('aula2', gen_random_uuid()), testes.id('d1'), privado.hoje() - 2, 1);

insert into public.frequencias (aula_id, aluno_id, status) values
  (testes.id('aula1'), testes.id('a1'), 'falta'),
  (testes.id('aula2'), testes.id('a1'), 'falta'),
  (testes.id('aula1'), testes.id('a2'), 'presente');

select is(privado.pontuacao_aluno(testes.id('a1'), 2026::smallint), -10, 'antes: dois dias de falta = -10');

-- ===== Quem não pode =====

select testes.entrar_como('a1');
select is((select count(*)::int from public.aulas), 2, 'aluno vê as aulas');
delete from public.aulas where id = testes.id('aula1');
set local role postgres;
select is((select count(*)::int from public.aulas where id = testes.id('aula1')), 1, 'aluno não apaga aula');

select testes.entrar_como('p2');
delete from public.aulas where id = testes.id('aula1');
set local role postgres;
select is((select count(*)::int from public.aulas where id = testes.id('aula1')), 1,
  'outro professor não apaga aula de uma disciplina que não é dele');

select testes.anonimo();
select throws_ok(format('delete from public.aulas where id = %L', testes.id('aula1')), '42501', null,
  'anon não apaga aula');

-- ===== Professor da disciplina =====

select testes.entrar_como('p1');
select lives_ok(format('delete from public.aulas where id = %L', testes.id('aula1')),
  'professor da disciplina apaga a aula');
set local role postgres;
select is((select count(*)::int from public.aulas where id = testes.id('aula1')), 0, 'a aula saiu');
select is((select count(*)::int from public.frequencias where aula_id = testes.id('aula1')), 0,
  'a frequência dela saiu junto');
select is((select count(*)::int from public.frequencias where aula_id = testes.id('aula2')), 1,
  'a frequência das outras aulas continua');
select is(privado.pontuacao_aluno(testes.id('a1'), 2026::smallint), -5,
  'o desconto do dia apagado some: só resta uma falta');

-- ===== Admin =====

select testes.entrar_como('adm');
select lives_ok(format('delete from public.aulas where id = %L', testes.id('aula2')), 'admin apaga qualquer aula');
set local role postgres;
select is(privado.pontuacao_aluno(testes.id('a1'), 2026::smallint), 0, 'sem aulas, sem desconto');

select * from finish();
rollback;
