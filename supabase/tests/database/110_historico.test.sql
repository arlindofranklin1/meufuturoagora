-- Histórico do administrador: só admin chama, e os números batem com a regra
-- da pontuação (nota - 5 por dia de falta, arredondada para cima no meio).
begin;
select * from no_plan();
select testes.cenario();

-- Duas aulas de d1: uma no 4º bimestre (hoje - 5 dias) e uma no 2º (hoje - 120)
insert into public.aulas (id, disciplina_id, data, quantidade) values
  (testes.guardar('aula4', gen_random_uuid()), testes.id('d1'), privado.hoje() - 5, 1),
  (testes.guardar('aula2', gen_random_uuid()), testes.id('d1'), privado.hoje() - 120, 1);

select is((select bimestre from public.aulas where id = testes.id('aula4')), 4::smallint, 'aula no 4º bimestre');
select is((select bimestre from public.aulas where id = testes.id('aula2')), 2::smallint, 'aula no 2º bimestre');

-- a1 faltou nos dois dias; a2 não faltou
insert into public.frequencias (aula_id, aluno_id, status) values
  (testes.id('aula4'), testes.id('a1'), 'falta'),
  (testes.id('aula2'), testes.id('a1'), 'falta'),
  (testes.id('aula4'), testes.id('a2'), 'presente');

-- Entregas avaliadas (4º bimestre, pelo gatilho): a1 tirou 8 e a2 tirou 4,5
insert into public.entregas (atividade_id, aluno_id, comentario_aluno) values
  (testes.id('t1'), testes.id('a1'), 'ok'),
  (testes.id('t1'), testes.id('a2'), 'ok');
update public.entregas set nota = 8, avaliado_em = now() where aluno_id = testes.id('a1');
update public.entregas set nota = 4.5, avaliado_em = now() where aluno_id = testes.id('a2');

-- ===== Quem pode =====

select testes.entrar_como('p1');
select throws_ok('select * from public.historico_anos()', '42501', null, 'professor não vê o histórico (anos)');
select throws_ok('select * from public.historico_alunos(2026)', '42501', null, 'professor não vê o histórico (alunos)');
select testes.entrar_como('a1');
select throws_ok('select * from public.historico_trilhas(2026)', '42501', null, 'aluno não vê o histórico (trilhas)');
select throws_ok('select * from public.historico_entregas(2026, 4)', '42501', null, 'aluno não vê o histórico (entregas)');
select testes.anonimo();
select throws_ok('select * from public.historico_alunos(2026)', '42501', null, 'anon não vê o histórico');

-- ===== Anos =====

select testes.entrar_como('adm');
select results_eq('select ano from public.historico_anos()', $$values (2026::smallint)$$,
  'anos com registro, do mais novo para o mais antigo');

-- ===== Alunos =====

select results_eq(
  'select nome, entregas, dias_falta, pontos from public.historico_alunos(2026)',
  $$values ('Bruno', 1, 0, 5), ('Ana', 1, 2, -2)$$,
  'ano inteiro: Bruno 4,5 arredonda para 5; Ana 8 - 2 dias de falta x 5 = -2; Carla sem registro fica de fora');
select results_eq(
  'select nome, entregas, dias_falta, pontos from public.historico_alunos(2026, 4)',
  $$values ('Bruno', 1, 0, 5), ('Ana', 1, 1, 3)$$,
  '4º bimestre: só a falta e a entrega desse período');
select results_eq(
  'select nome, entregas, dias_falta, pontos from public.historico_alunos(2026, 2)',
  $$values ('Ana', 0, 1, -5)$$,
  '2º bimestre: só a falta; quem não tem registro no período não aparece');
select is((select count(*)::int from public.historico_alunos(2026, 0)), 2, 'bimestre 0 = ano inteiro');
select is((select count(*)::int from public.historico_alunos(2025)), 0, 'outro ano: vazio');
select is((select turma_nome from public.historico_alunos(2026) where nome = 'Ana'), '6º ano A', 'traz a turma do aluno');

-- Falta justificada não conta
set local role postgres;
update public.frequencias set status = 'falta_justificada'
where aula_id = testes.id('aula2') and aluno_id = testes.id('a1');
select testes.entrar_como('adm');
select is((select pontos from public.historico_alunos(2026) where nome = 'Ana'), 3, 'falta justificada não desconta');
select is((select count(*)::int from public.historico_alunos(2026, 2)), 0, 'e não faz o aluno entrar no período');

-- Coerência com a pontuação do ranking (função interna: só o dono do banco chama)
select pontos as pontos_historico into temp table comparacao from public.historico_alunos(2026) where nome = 'Bruno';
set local role postgres;
select is((select pontos_historico from comparacao),
  privado.pontuacao_aluno(testes.id('a2'), 2026::smallint), 'bate com pontuacao_aluno');

-- ===== Trilhas =====

select results_eq(
  'select nome, disciplina_nome, professor_nome, turma_nome, pontos, entregas from public.historico_trilhas(2026)',
  $$values ('Frações', 'Matemática', 'Paula', '6º ano A', 10, 2)$$,
  'trilha com disciplina, professor, turma e total de entregas');
select is((select count(*)::int from public.historico_trilhas(2026, 4)), 1, '4º bimestre: a trilha criada hoje');
select is((select count(*)::int from public.historico_trilhas(2026, 1)), 0, '1º bimestre: nenhuma');

-- ===== Entregas =====

select results_eq(
  'select aluno_nome, atividade_nome, disciplina_nome, turma_nome, nota from public.historico_entregas(2026, 4) order by aluno_nome',
  $$values ('Ana', 'Frações', 'Matemática', '6º ano A', 8.00::numeric),
           ('Bruno', 'Frações', 'Matemática', '6º ano A', 4.50::numeric)$$,
  'entregas do 4º bimestre com as notas');

-- Sem turma: usa a turma da disciplina; entrega pendente tem nota nula
set local role postgres;
insert into public.entregas (atividade_id, aluno_id, comentario_aluno)
values (testes.id('t1'), testes.id('a3'), 'pendente');
update public.perfis set turma_id = null where id = testes.id('a3');
select testes.entrar_como('adm');
select results_eq(
  $$select turma_nome, nota from public.historico_entregas(2026) where aluno_nome = 'Carla'$$,
  $$values ('6º ano A', null::numeric)$$,
  'aluno sem turma: vale a turma da disciplina; pendente tem nota nula');

select * from finish();
rollback;
