-- Turmas, disciplinas e matrícula automática (D1, D9).
begin;
select * from no_plan();
select testes.cenario();

-- ===== Turmas =====

select testes.entrar_como('a1');
select results_eq('select id from public.turmas', array[testes.id('turma_a')], 'aluno vê só a própria turma');
select throws_ok($$insert into public.turmas (nome) values ('7º ano')$$, '42501', null,
  'aluno não cria turma');

select testes.entrar_como('p2');
select is((select count(*)::int from public.turmas), 2, 'professor vê todas as turmas (para criar disciplina)');
select throws_ok($$insert into public.turmas (nome) values ('7º ano')$$, '42501', null,
  'professor não cria turma');

select testes.entrar_como('adm');
select lives_ok($$insert into public.turmas (nome, serie) values ('7º ano A', 7)$$, 'admin cria turma');
select throws_ok($$insert into public.turmas (nome) values (' 7º ANO a ')$$, '23505', null,
  'turma com o mesmo nome (maiúsculas e espaços) é recusada');

-- ===== Disciplinas: quem vê =====

select testes.entrar_como('p1');
select set_eq('select id from public.disciplinas', array[testes.id('d1'), testes.id('d3')],
  'professor vê só as próprias disciplinas');
select is((select count(*)::int from public.disciplinas where id = testes.id('d2')), 0,
  'professor não acessa disciplina de outro professor');

select testes.entrar_como('a1');
select set_eq('select id from public.disciplinas', array[testes.id('d1'), testes.id('d2')],
  'aluno vê as disciplinas em que está matriculado');
select testes.entrar_como('a3');
select results_eq('select id from public.disciplinas', array[testes.id('d3')],
  'aluno de outra turma não vê as disciplinas da turma A');

-- ===== Disciplinas: criar (D1) =====

select testes.entrar_como('p2');
select lives_ok(
  format('insert into public.disciplinas (nome, turma_id) values (%L, %L)', 'História', testes.id('turma_a')),
  'professor cria a própria disciplina');
set local role postgres;
select is((select professor_id from public.disciplinas where nome = 'História'), testes.id('p2'),
  'o responsável é o professor logado');
select is((select ano_letivo from public.disciplinas where nome = 'História'), 2026::smallint,
  'ano letivo preenchido pelo servidor');
select is((select bimestre from public.disciplinas where nome = 'História'), 4::smallint,
  'bimestre preenchido pelo servidor');
select set_eq(
  $$select aluno_id from public.matriculas m join public.disciplinas d on d.id = m.disciplina_id where d.nome = 'História'$$,
  array[testes.id('a1'), testes.id('a2')],
  'D9: alunos da turma são matriculados na hora');

-- Sai do caminho dos próximos testes
delete from public.disciplinas where nome = 'História';

select testes.entrar_como('p2');
select throws_ok(
  format('insert into public.disciplinas (nome, turma_id, professor_id) values (%L, %L, %L)',
         'Geografia', testes.id('turma_a'), testes.id('p1')),
  '42501', null, 'professor não cria disciplina em nome de outro');
select throws_ok(
  format('insert into public.disciplinas (nome, turma_id, ano_letivo) values (%L, %L, 2020)',
         'Geografia', testes.id('turma_a')),
  '42501', null, 'o app não escolhe o ano letivo');

-- O gatilho que exige professor como responsável roda antes do RLS, por isso o
-- código é 23514; o importante é que a disciplina não é criada
select testes.entrar_como('a1');
select throws_ok(
  format('insert into public.disciplinas (nome, turma_id) values (%L, %L)', 'Artes', testes.id('turma_a')),
  '23514', null, 'aluno não cria disciplina');

select testes.entrar_como('adm');
select throws_ok(
  format('insert into public.disciplinas (nome, turma_id, professor_id) values (%L, %L, %L)',
         'Artes', testes.id('turma_a'), testes.id('a1')),
  '23514', null, 'o responsável pela disciplina precisa ser professor');

-- ===== Disciplinas: alterar =====

select testes.entrar_como('p1');
select lives_ok(format('update public.disciplinas set nome = %L where id = %L', 'Matemática I', testes.id('d1')),
  'professor renomeia a própria disciplina');
update public.disciplinas set nome = 'Invadida' where id = testes.id('d2');
set local role postgres;
select is((select nome from public.disciplinas where id = testes.id('d2')), 'Português',
  'professor não renomeia disciplina de outro');

select testes.entrar_como('p1');
select throws_ok(
  format('update public.disciplinas set professor_id = %L where id = %L', testes.id('p2'), testes.id('d1')),
  '42501', null, 'professor não passa a disciplina para outro (só o admin)');
select throws_ok(
  format('update public.disciplinas set turma_id = %L where id = %L', testes.id('turma_b'), testes.id('d1')),
  '42501', null, 'professor não troca a turma da disciplina');
select throws_ok(format('delete from public.disciplinas where id = %L', testes.id('d1')), '42501', null,
  'disciplina não é apagada pela API');

-- Exclusão lógica: some para o aluno
select lives_ok(format('update public.disciplinas set ativo = false where id = %L', testes.id('d1')),
  'professor desativa a própria disciplina');
select testes.entrar_como('a1');
select results_eq('select id from public.disciplinas', array[testes.id('d2')],
  'disciplina desativada some para o aluno');
select testes.entrar_como('p1');
select is((select count(*)::int from public.disciplinas where id = testes.id('d1')), 1,
  'o professor continua vendo a disciplina desativada');

-- ===== Matrícula automática (D9) =====

set local role postgres;
update public.disciplinas set ativo = true where id = testes.id('d1');

select testes.entrar_como('adm');
select lives_ok(
  format('insert into public.perfis (email, nome, tipo, turma_id) values (%L, %L, %L, %L)',
         'a4@escola.test', 'Davi', 'aluno', testes.id('turma_a')),
  'admin cadastra aluno na turma A');
select throws_ok(
  format('insert into public.perfis (id, email, nome, tipo) values (%L, %L, %L, %L)',
         gen_random_uuid(), 'a9@escola.test', 'Com Id', 'aluno'),
  '42501', null, 'o id do perfil é sempre gerado pelo banco');
set local role postgres;
select testes.guardar('a4', (select id from public.perfis where email = 'a4@escola.test'));
select set_eq(format('select disciplina_id from public.matriculas where aluno_id = %L', testes.id('a4')),
  array[testes.id('d1'), testes.id('d2')],
  'aluno novo entra nas disciplinas ativas da turma');

select testes.entrar_como('adm');
select lives_ok(format('update public.perfis set turma_id = %L where id = %L', testes.id('turma_b'), testes.id('a4')),
  'admin muda o aluno de turma');
set local role postgres;
select results_eq(format('select disciplina_id from public.matriculas where aluno_id = %L', testes.id('a4')),
  array[testes.id('d3')],
  'ao mudar de turma, sai das disciplinas antigas e entra nas novas');

update public.disciplinas set ativo = false where id = testes.id('d2');
insert into public.perfis (id, email, nome, tipo, turma_id)
values (testes.guardar('a5', gen_random_uuid()), 'a5@escola.test', 'Elisa', 'aluno', testes.id('turma_a'));
select results_eq(format('select disciplina_id from public.matriculas where aluno_id = %L', testes.id('a5')),
  array[testes.id('d1')], 'disciplina desativada não recebe aluno novo');
update public.disciplinas set ativo = true where id = testes.id('d2');
select is((select count(*)::int from public.matriculas where aluno_id = testes.id('a5')), 2,
  'disciplina reativada recebe os alunos que faltavam');

update public.disciplinas set turma_id = testes.id('turma_b') where id = testes.id('d2');
select set_eq(format('select aluno_id from public.matriculas where disciplina_id = %L', testes.id('d2')),
  array[testes.id('a3'), testes.id('a4')],
  'disciplina trocada de turma passa a ter os alunos da turma nova');

-- Excluir a turma deixa alunos e disciplinas sem turma (ListaUsuariosActivity.java:314)
delete from public.turmas where id = testes.id('turma_b');
select is((select turma_id from public.perfis where id = testes.id('a3')), null, 'aluno fica sem turma');
select is((select turma_id from public.disciplinas where id = testes.id('d3')), null, 'disciplina fica sem turma');
select is((select count(*)::int from public.matriculas where disciplina_id = testes.id('d3')), 0,
  'disciplina sem turma fica sem alunos');

-- ===== Matrículas: só leitura =====

select testes.entrar_como('a1');
select set_eq('select disciplina_id from public.matriculas', array[testes.id('d1')],
  'aluno vê só as próprias matrículas');
select throws_ok(
  format('insert into public.matriculas (disciplina_id, aluno_id) values (%L, %L)', testes.id('d2'), testes.id('a1')),
  '42501', null, 'aluno não se matricula sozinho');

select testes.entrar_como('p1');
select set_eq(format('select aluno_id from public.matriculas where disciplina_id = %L', testes.id('d1')),
  array[testes.id('a1'), testes.id('a2'), testes.id('a5')],
  'professor vê as matrículas da própria disciplina');

select * from finish();
rollback;
