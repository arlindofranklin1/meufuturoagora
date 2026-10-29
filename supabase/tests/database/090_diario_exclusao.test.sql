-- Diário (D8), pedidos de exclusão (D12) e exclusão definitiva (D7, D13, D14).
begin;
select * from no_plan();
select testes.cenario();

-- ===== Diário: só o próprio aluno =====

select testes.entrar_como('a1');
select lives_ok($$insert into public.diario_entradas (texto) values ('Hoje aprendi frações')$$,
  'aluno escreve no diário');
select throws_ok($$insert into public.diario_entradas (texto) values ('   ')$$, '23514', null,
  'entrada vazia é recusada');
select throws_ok(
  format($$insert into public.diario_entradas (aluno_id, texto) values (%L, 'no diário de outro')$$, testes.id('a2')),
  '42501', null, 'aluno não escreve no diário de outro');
select lives_ok($$update public.diario_entradas set texto = 'Hoje aprendi frações e decimais'$$, 'aluno edita');
select is((select texto from public.diario_entradas), 'Hoje aprendi frações e decimais', 'a edição vale');

select testes.entrar_como('a2');
select is((select count(*)::int from public.diario_entradas), 0, 'outro aluno não lê o diário');
update public.diario_entradas set texto = 'invadido';
delete from public.diario_entradas;

select testes.entrar_como('p1');
select is((select count(*)::int from public.diario_entradas), 0, 'D8: o professor não lê o diário');
select throws_ok($$insert into public.diario_entradas (texto) values ('professor')$$, '42501', null,
  'professor não tem diário');

select testes.entrar_como('adm');
select is((select count(*)::int from public.diario_entradas), 0, 'D8: nem o admin lê o diário');

select testes.entrar_como('a1');
select is((select texto from public.diario_entradas), 'Hoje aprendi frações e decimais',
  'o diário não foi alterado por outros');
select lives_ok('delete from public.diario_entradas', 'aluno apaga a própria entrada');
select is((select count(*)::int from public.diario_entradas), 0, 'apagada');

-- ===== Pedido de exclusão (D12) =====

select testes.entrar_como('a1');
select lives_ok($$select public.solicitar_exclusao('Vou mudar de escola')$$, 'aluno pede a exclusão dos dados');
select throws_ok('select public.solicitar_exclusao()', 'P0001', null, 'só um pedido aberto por vez');
select results_eq('select status, motivo from public.solicitacoes_exclusao',
  $$values ('pendente', 'Vou mudar de escola')$$, 'aluno vê o próprio pedido');
select throws_ok(
  format($$update public.solicitacoes_exclusao set status = 'aprovada'$$),
  '42501', null, 'aluno não aprova o próprio pedido');
select throws_ok('select public.decidir_exclusao(id, true) from public.solicitacoes_exclusao', '42501', null,
  'aluno não decide pedidos');

select testes.entrar_como('a2');
select is((select count(*)::int from public.solicitacoes_exclusao), 0, 'outro aluno não vê o pedido');
select lives_ok('select public.solicitar_exclusao()', 'outro aluno também pede');

select testes.entrar_como('adm');
select is((select count(*)::int from public.solicitacoes_exclusao), 2, 'admin vê todos os pedidos');
select testes.guardar('pedido_a1', (select id from public.solicitacoes_exclusao where perfil_id = testes.id('a1')));
select testes.guardar('pedido_a2', (select id from public.solicitacoes_exclusao where perfil_id = testes.id('a2')));

select lives_ok(format('select public.decidir_exclusao(%L, false)', testes.id('pedido_a2')), 'admin recusa um pedido');
select results_eq(format('select status, decidido_por from public.solicitacoes_exclusao where id = %L', testes.id('pedido_a2')),
  format($$values ('recusada', %L::uuid)$$, testes.id('adm')), 'o pedido fica recusado, com quem decidiu');
select throws_ok(format('select public.decidir_exclusao(%L, true)', testes.id('pedido_a2')), 'P0002', null,
  'pedido já decidido não é decidido de novo');

-- Aprovar apaga tudo do aluno (D7)
set local role postgres;
insert into public.entregas (atividade_id, aluno_id, comentario_aluno) values (testes.id('t1'), testes.id('a1'), 'ok');
insert into public.diario_entradas (aluno_id, texto) values (testes.id('a1'), 'segredo');

select testes.entrar_como('adm');
select lives_ok(format('select public.decidir_exclusao(%L, true)', testes.id('pedido_a1')), 'admin aprova a exclusão');

set local role postgres;
select is((select count(*)::int from public.perfis where id = testes.id('a1')), 0, 'o perfil foi apagado');
select is((select count(*)::int from auth.users where id = testes.id('u_a1')), 0, 'a conta de login foi apagada');
select is((select count(*)::int from public.entregas where aluno_id = testes.id('a1')), 0, 'as entregas foram apagadas');
select is((select count(*)::int from public.matriculas where aluno_id = testes.id('a1')), 0, 'as matrículas foram apagadas');
select is((select count(*)::int from public.diario_entradas where aluno_id = testes.id('a1')), 0, 'o diário foi apagado');
select results_eq(
  format('select solicitacao_id, tipo_perfil, decidido_por from public.exclusoes_realizadas'),
  format($$values (%L::uuid, 'aluno', %L::uuid)$$, testes.id('pedido_a1'), testes.id('adm')),
  'fica só o comprovante, sem dados pessoais');

-- ===== Exclusão direta pelo admin =====

select testes.entrar_como('p1');
select throws_ok(format('select public.excluir_usuario(%L)', testes.id('a2')), '42501', null,
  'professor não exclui usuários');

select testes.entrar_como('adm');
select throws_ok(format('select public.excluir_usuario(%L)', testes.id('p1')), 'P0001', null,
  'D13: professor com disciplinas não é excluído');
select throws_ok(format('select public.transferir_disciplinas(%L, %L)', testes.id('p1'), testes.id('a2')), 'P0001', null,
  'disciplinas não vão para um aluno');
select throws_ok(format('select public.transferir_disciplinas(%L, %L)', testes.id('p1'), testes.id('p1')), 'P0001', null,
  'disciplinas não vão para o mesmo professor');
select is(public.transferir_disciplinas(testes.id('p1'), testes.id('p2')), 2, 'admin transfere as 2 disciplinas');
select lives_ok(format('select public.excluir_usuario(%L)', testes.id('p1')), 'D13: depois de transferir, exclui');
set local role postgres;
select is((select count(*)::int from public.disciplinas where professor_id = testes.id('p2')), 3,
  'as disciplinas continuam, com o novo professor');
select is((select count(*)::int from public.atividades where id = testes.id('t1')), 1,
  'as trilhas continuam');

select testes.entrar_como('adm');
select throws_ok(format('select public.excluir_usuario(%L)', testes.id('adm')), 'P0001', null,
  'D14: o último admin não se exclui');
select throws_ok(format('select public.excluir_usuario(%L)', gen_random_uuid()), 'P0002', null,
  'usuário inexistente');

select testes.anonimo();
select throws_ok('select public.solicitar_exclusao()', '42501', null, 'anon não pede exclusão');

select * from finish();
rollback;
