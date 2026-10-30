-- Atividades (trilhas), entregas e avaliação (D3, D4, D6).
begin;
select * from no_plan();
select testes.cenario();

-- ===== Atividades =====

select testes.entrar_como('p1');
select lives_ok(
  format($$insert into public.atividades (id, disciplina_id, nome, pontos, prazo) values (%L, %L, 'Equações', 5, %L)$$,
         testes.guardar('t2', gen_random_uuid()), testes.id('d1'), (now() at time zone 'America/Sao_Paulo')::date - 3),
  'professor cria trilha na própria disciplina (prazo já vencido, para o teste de atraso)');
select throws_ok(
  format($$insert into public.atividades (disciplina_id, nome, pontos, prazo) values (%L, 'Invasão', 5, %L)$$,
         testes.id('d2'), (now() at time zone 'America/Sao_Paulo')::date),
  '42501', null, 'professor não cria trilha em disciplina de outro');
select throws_ok(
  format($$insert into public.atividades (disciplina_id, nome, pontos, prazo) values (%L, 'Demais', 11, %L)$$,
         testes.id('d1'), (now() at time zone 'America/Sao_Paulo')::date),
  '23514', null, 'trilha vale de 1 a 10 pontos');
select throws_ok(
  format($$insert into public.atividades (disciplina_id, nome, pontos, prazo, arquivo_path, arquivo_nome)
           values (%L, 'Anexo', 5, %L, 'outra/pasta/x.pdf', 'x.pdf')$$,
         testes.id('d1'), (now() at time zone 'America/Sao_Paulo')::date),
  '23514', null, 'anexo fora da pasta da trilha é recusado');

select testes.entrar_como('p2');
select is((select count(*)::int from public.atividades where disciplina_id = testes.id('d1')), 0,
  'outro professor não vê as trilhas');
update public.atividades set nome = 'Invadida' where id = testes.id('t1');

select testes.entrar_como('a1');
select set_eq('select id from public.atividades', array[testes.id('t1'), testes.id('t2')],
  'aluno matriculado vê as trilhas da disciplina');
select is((select nome from public.atividades where id = testes.id('t1')), 'Frações',
  'outro professor não alterou a trilha');
select throws_ok(
  format($$insert into public.atividades (disciplina_id, nome, pontos, prazo) values (%L, 'Aluno', 5, %L)$$,
         testes.id('d1'), (now() at time zone 'America/Sao_Paulo')::date),
  '42501', null, 'aluno não cria trilha');

select testes.entrar_como('a3');
select is((select count(*)::int from public.atividades), 0, 'aluno de outra turma não vê as trilhas');

-- ===== Entregas =====

select testes.entrar_como('a1');
select lives_ok(
  format($$insert into public.entregas (atividade_id, comentario_aluno) values (%L, 'Minha resposta')$$, testes.id('t1')),
  'aluno entrega a trilha');
set local role postgres;
select is((select aluno_id from public.entregas where atividade_id = testes.id('t1')), testes.id('a1'),
  'a entrega é do aluno logado');
select is((select dias_atraso from public.entregas where atividade_id = testes.id('t1')), 0::smallint,
  'no prazo: 0 dias de atraso');
select is((select ano_letivo from public.entregas where atividade_id = testes.id('t1')), 2026::smallint,
  'ano letivo preenchido pelo servidor');

select testes.entrar_como('a1');
select throws_ok(
  format($$insert into public.entregas (atividade_id, comentario_aluno) values (%L, 'De novo')$$, testes.id('t1')),
  '23505', null, 'só uma entrega por trilha');
select lives_ok(
  format($$insert into public.entregas (atividade_id, comentario_aluno) values (%L, 'Atrasada')$$, testes.id('t2')),
  'D4: entrega depois do prazo é aceita');
set local role postgres;
select is((select dias_atraso from public.entregas where atividade_id = testes.id('t2')), 3::smallint,
  'D4: e fica marcada com 3 dias de atraso');

select testes.entrar_como('a2');
select throws_ok(
  format($$insert into public.entregas (atividade_id, comentario_aluno) values (%L, '   ')$$, testes.id('t1')),
  '23514', null, 'entrega vazia é recusada');
select throws_ok(
  format($$insert into public.entregas (atividade_id, aluno_id, comentario_aluno) values (%L, %L, 'Em nome da Ana')$$,
         testes.id('t2'), testes.id('a1')),
  '42501', null, 'aluno não entrega em nome de outro');
select throws_ok(
  format($$insert into public.entregas (atividade_id, comentario_aluno, nota) values (%L, 'Com nota', 10)$$, testes.id('t1')),
  '42501', null, 'aluno não manda a nota junto');
select throws_ok(
  format($$insert into public.entregas (atividade_id, comentario_aluno, arquivo_path, arquivo_nome) values (%L, 'x', %L, 'x.pdf')$$,
         testes.id('t1'), testes.id('t1')::text || '/' || testes.id('a1')::text || '/x.pdf'),
  '23514', null, 'arquivo na pasta de outro aluno é recusado');
select is((select count(*)::int from public.entregas), 0, 'aluno não lê a entrega de outro aluno');

select testes.entrar_como('a3');
select throws_ok(
  format($$insert into public.entregas (atividade_id, comentario_aluno) values (%L, 'Intrusa')$$, testes.id('t1')),
  '42501', null, 'aluno não matriculado não entrega');

-- Aluno não altera nem apaga a entrega, nem a nota
select testes.entrar_como('a1');
select throws_ok(
  format($$update public.entregas set comentario_aluno = 'Mudei' where atividade_id = %L$$, testes.id('t1')),
  '42501', null, 'entrega não muda depois de enviada');
select throws_ok(
  format('update public.entregas set nota = 10 where atividade_id = %L', testes.id('t1')),
  '42501', null, 'aluno não altera a própria nota');
select throws_ok(format('delete from public.entregas where atividade_id = %L', testes.id('t1')), '42501', null,
  'aluno não apaga a entrega');
select throws_ok(
  format('select public.avaliar_entrega(id, 10) from public.entregas where atividade_id = %L', testes.id('t1')),
  '42501', null, 'aluno não se avalia');

select testes.guardar('e1', (select id from public.entregas where atividade_id = testes.id('t1')));

-- ===== Avaliação (D3) =====

select testes.entrar_como('p2');
select is((select count(*)::int from public.entregas), 0, 'outro professor não vê as entregas');
select throws_ok(format('select public.avaliar_entrega(%L, 8)', testes.id('e1')), '42501', null,
  'outro professor não avalia');

select testes.entrar_como('p1');
select is((select count(*)::int from public.entregas), 2, 'professor vê as entregas das suas trilhas');
select throws_ok(format('select public.avaliar_entrega(%L, 10.5)', testes.id('e1')), 'P0001', null,
  'D3: nota acima dos pontos da trilha é recusada');
select throws_ok(format('select public.avaliar_entrega(%L, -1)', testes.id('e1')), 'P0001', null,
  'D3: nota negativa é recusada');
select throws_ok(format('select public.avaliar_entrega(%L, null)', testes.id('e1')), 'P0001', null,
  'nota vazia é recusada');
select throws_ok(format('select public.avaliar_entrega(%L, 8)', gen_random_uuid()), '42501', null,
  'entrega inexistente: sem permissão (não revela se existe)');
select lives_ok(format($$select public.avaliar_entrega(%L, 8.5, '  Muito bem  ')$$, testes.id('e1')),
  'professor avalia com nota de 0 a 10');
select results_eq(
  format('select nota, comentario_professor, avaliado_em is not null from public.entregas where id = %L', testes.id('e1')),
  $$values (8.50::numeric(4,2), 'Muito bem', true)$$,
  'nota, comentário e data da avaliação gravados');
select lives_ok(format('select public.avaliar_entrega(%L, 10)', testes.id('e1')), 'professor pode reavaliar');
select lives_ok(format('select public.avaliar_entrega(%L, 0)', testes.id('e1')), 'nota zero é aceita');

select lives_ok(format('select public.refazer_avaliacao(%L)', testes.id('e1')), 'professor refaz a avaliação');
select results_eq(
  format('select nota, comentario_professor, avaliado_em from public.entregas where id = %L', testes.id('e1')),
  $$values (null::numeric(4,2), null::text, null::timestamptz)$$,
  'refazer apaga nota, comentário e data');

select testes.entrar_como('p2');
select throws_ok(format('select public.refazer_avaliacao(%L)', testes.id('e1')), '42501', null,
  'outro professor não refaz avaliação');

-- ===== Excluir trilha (D6) =====

select testes.entrar_como('p2');
delete from public.atividades where id = testes.id('t1');
set local role postgres;
select is((select count(*)::int from public.atividades where id = testes.id('t1')), 1,
  'outro professor não apaga a trilha');

select testes.entrar_como('p1');
select lives_ok(format('delete from public.atividades where id = %L', testes.id('t1')), 'professor apaga a própria trilha');
set local role postgres;
select is((select count(*)::int from public.entregas where atividade_id = testes.id('t1')), 0,
  'D6: as entregas da trilha são apagadas junto');

select * from finish();
rollback;
