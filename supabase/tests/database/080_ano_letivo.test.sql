-- Ano letivo e bimestres (DefinirAnoLetivoActivity, DefinirBimestresActivity, N5).
begin;
select * from no_plan();
select testes.cenario();

-- ===== Quem pode =====

select testes.entrar_como('p1');
select throws_ok('select public.definir_ano_letivo(2027)', '42501', null, 'professor não troca o ano letivo');
select throws_ok($$select public.definir_bimestres(2026, '[]')$$, '42501', null, 'professor não define bimestres');
select throws_ok('insert into public.anos_letivos (ano) values (2030)', '42501', null,
  'ninguém grava ano letivo direto pela API');
select is((select count(*)::int from public.anos_letivos), 1, 'professor lê o ano letivo');
select is((select count(*)::int from public.bimestres), 4, 'professor lê os bimestres');

select testes.entrar_como('a1');
select throws_ok('select public.definir_ano_letivo(2027)', '42501', null, 'aluno não troca o ano letivo');
select is((select count(*)::int from public.bimestres), 4, 'aluno lê os bimestres');

-- ===== Troca de ano (sem senha, decisão N5) =====

set local role postgres;
insert into public.entregas (atividade_id, aluno_id, comentario_aluno) values (testes.id('t1'), testes.id('a1'), 'ok');
update public.entregas set nota = 6, avaliado_em = now() where atividade_id = testes.id('t1');

select testes.entrar_como('adm');
select throws_ok('select public.definir_ano_letivo(1999)', 'P0001', null, 'ano fora do intervalo é recusado');
select lives_ok('select public.definir_ano_letivo(2027)', 'admin troca para 2027');
select results_eq('select ano, atual, encerrado_em is not null from public.anos_letivos order by ano',
  $$values (2026::smallint, false, true), (2027::smallint, true, false)$$,
  'o ano anterior fica encerrado e o novo vira o atual');
select is((select count(*)::int from public.bimestres where ano = 2027), 0, 'o ano novo começa sem bimestres');
select is((select count(*)::int from public.bimestres where ano = 2026), 4, 'os bimestres de 2026 ficam guardados');
select is(public.pontuacao(testes.id('a1')), 0, 'a pontuação recomeça no ano novo');
select is(public.pontuacao(testes.id('a1'), 2026), 6, 'e as notas de 2026 continuam no histórico');

select lives_ok('select public.definir_ano_letivo(2026)', 'admin volta para 2026');
select results_eq('select ano, atual, encerrado_em is null from public.anos_letivos where atual',
  $$values (2026::smallint, true, true)$$, 'voltar reabre o ano');
select is(public.pontuacao(testes.id('a1')), 6, 'as notas de 2026 voltam a contar');
select is((select count(*)::int from public.anos_letivos where atual), 1, 'sempre um só ano atual');

-- ===== Bimestres =====

select lives_ok($$select public.definir_bimestres(2026, '[
    {"numero": 1, "inicio": "2026-02-02", "fim": "2026-04-30"},
    {"numero": 2, "inicio": "2026-05-04", "fim": "2026-07-10"}
  ]')$$, 'admin define dois bimestres');
select results_eq('select numero, inicio, fim from public.bimestres where ano = 2026 order by numero',
  $$values (1::smallint, '2026-02-02'::date, '2026-04-30'::date), (2::smallint, '2026-05-04'::date, '2026-07-10'::date)$$,
  'os bimestres antigos são substituídos');

select throws_ok($$select public.definir_bimestres(2026, '[
    {"numero": 1, "inicio": "2026-02-02", "fim": "2026-05-10"},
    {"numero": 2, "inicio": "2026-05-04", "fim": "2026-07-10"}
  ]')$$, 'P0001', null, 'bimestres sobrepostos são recusados');
select throws_ok($$select public.definir_bimestres(2026, '[
    {"numero": 1, "inicio": "2026-08-01", "fim": "2026-09-30"},
    {"numero": 2, "inicio": "2026-02-02", "fim": "2026-04-30"}
  ]')$$, 'P0001', null, 'bimestres fora de ordem são recusados');
select throws_ok($$select public.definir_bimestres(2026, '[{"numero": 1, "inicio": "2026-04-30", "fim": "2026-02-02"}]')$$,
  '23514', null, 'fim antes do início é recusado');
select throws_ok($$select public.definir_bimestres(2026, '[{"numero": 1, "inicio": "2026-02-02"}]')$$,
  'P0001', null, 'bimestre sem fim é recusado');
select throws_ok($$select public.definir_bimestres(2026, '[{"numero": 5, "inicio": "2026-02-02", "fim": "2026-03-02"}]')$$,
  '23514', null, 'só existem os bimestres 1 a 4');
select throws_ok($$select public.definir_bimestres(2026, '[
    {"numero": 1, "inicio": "2026-02-02", "fim": "2026-03-01"},
    {"numero": 1, "inicio": "2026-03-02", "fim": "2026-04-01"}
  ]')$$, 'P0001', null, 'bimestre repetido é recusado');
select throws_ok($$select public.definir_bimestres(2031, '[]')$$, 'P0001', null,
  'bimestres de um ano não cadastrado são recusados');
select is((select count(*)::int from public.bimestres where ano = 2026), 2,
  'as tentativas recusadas não mudaram nada');

select lives_ok($$select public.definir_bimestres(2026, '[]')$$, 'lista vazia deixa o ano sem bimestres');
select is((select count(*)::int from public.bimestres where ano = 2026), 0, 'sem bimestres');

select * from finish();
rollback;
