-- Ranking por turma (D2, N6, RankingActivity).
begin;
select * from no_plan();
select testes.cenario();

-- Turma A com seis alunos: a1 (Ana), a2 (Bruno), mais quatro
select testes.criar_usuario('a4', 'a4@escola.test');
select testes.criar_usuario('a5', 'a5@escola.test');
select testes.criar_usuario('a6', 'a6@escola.test');
insert into public.perfis (id, usuario_id, email, nome, tipo, turma_id) values
  (testes.guardar('a4', gen_random_uuid()), testes.id('u_a4'), 'a4@escola.test', 'davi', 'aluno', testes.id('turma_a')),
  (testes.guardar('a5', gen_random_uuid()), testes.id('u_a5'), 'a5@escola.test', 'Elisa', 'aluno', testes.id('turma_a')),
  (testes.guardar('a6', gen_random_uuid()), testes.id('u_a6'), 'a6@escola.test', 'Fábio', 'aluno', testes.id('turma_a')),
  -- Cadastrado mas ainda sem login: fica fora do ranking (RankingActivity.java:321)
  (testes.guardar('a7', gen_random_uuid()), null, 'a7@escola.test', 'Gabi', 'aluno', testes.id('turma_a'));

create function pg_temp.nota(p_aluno text, p_nota numeric) returns void
language sql as $$
  insert into public.entregas (atividade_id, aluno_id, comentario_aluno)
  values (testes.id('t1'), testes.id(p_aluno), 'ok');
  update public.entregas set nota = p_nota, avaliado_em = now()
  where atividade_id = testes.id('t1') and aluno_id = testes.id(p_aluno);
$$;

-- Pontos: Elisa 9, Bruno 7, Ana 7, davi 7, Fábio 2, Gabi 10 (sem login)
select pg_temp.nota('a5', 9);
select pg_temp.nota('a2', 7);
select pg_temp.nota('a1', 7);
select pg_temp.nota('a4', 7);
select pg_temp.nota('a6', 2);
select pg_temp.nota('a7', 10);

-- ===== Professor da turma vê todos =====

select testes.entrar_como('p1');
select results_eq(
  format('select nome, pontos, posicao from public.ranking_turma(%L)', testes.id('turma_a')),
  $$values ('Elisa', 9, 1), ('Ana', 7, 2), ('Bruno', 7, 3), ('davi', 7, 4), ('Fábio', 2, 5)$$,
  'por pontos e, no empate, por nome sem diferenciar maiúsculas; sem quem nunca entrou');

select testes.entrar_como('p2');
select is((select count(*)::int from public.ranking_turma(testes.id('turma_a'))), 5,
  'outro professor da turma também vê');
select throws_ok(format('select * from public.ranking_turma(%L)', testes.id('turma_b')), '42501', null,
  'D2: professor não vê ranking de turma em que não dá aula');
select results_eq('select nome from public.turmas_do_ranking()', $$values ('6º ano A')$$,
  'D2: o seletor do professor só mostra as turmas dele');

select testes.entrar_como('p1');
select set_eq('select nome from public.turmas_do_ranking()', $$values ('6º ano A'), ('6º ano B')$$,
  'professor com disciplinas em duas turmas vê as duas');

-- ===== Aluno vê o pódio e a própria linha =====

select testes.entrar_como('a6');
select results_eq(
  format('select nome, posicao, eh_voce from public.ranking_turma(%L)', testes.id('turma_a')),
  $$values ('Elisa', 1, false), ('Ana', 2, false), ('Bruno', 3, false), ('Fábio', 5, true)$$,
  'aluno fora do pódio vê os 3 primeiros e a própria posição');

select testes.entrar_como('a1');
select results_eq(
  format('select nome, eh_voce from public.ranking_turma(%L)', testes.id('turma_a')),
  $$values ('Elisa', false), ('Ana', true), ('Bruno', false)$$,
  'aluno no pódio vê só o pódio, marcado como "você"');
select results_eq('select posicao, pontos, total from public.minha_posicao_no_ranking()',
  $$values (2, 7, 5)$$, 'posição do aluno na própria turma');
select throws_ok(format('select * from public.ranking_turma(%L)', testes.id('turma_b')), '42501', null,
  'aluno não vê o ranking de outra turma');

-- ===== Participantes da disciplina (InfoDisciplinaDialog) =====

select results_eq(
  format('select nome, email from public.participantes_disciplina(%L) where tipo = %L', testes.id('d1'), 'professor'),
  $$values ('Paula', 'p1@escola.test')$$, 'aluno vê o nome e o e-mail do professor');
select is(
  (select count(*)::int from public.participantes_disciplina(testes.id('d1')) where tipo = 'aluno' and email is not null),
  0, 'aluno não vê o e-mail dos colegas');
select is(
  (select count(*)::int from public.participantes_disciplina(testes.id('d1')) where tipo = 'aluno'), 6,
  'aluno vê a lista de colegas da disciplina');

select testes.entrar_como('a3');
select throws_ok(format('select * from public.participantes_disciplina(%L)', testes.id('d1')), '42501', null,
  'aluno de fora não vê os participantes');

select testes.entrar_como('p1');
select is(
  (select count(*)::int from public.participantes_disciplina(testes.id('d1')) where tipo = 'aluno' and email is not null),
  6, 'o professor vê o e-mail dos alunos');

select * from finish();
rollback;
