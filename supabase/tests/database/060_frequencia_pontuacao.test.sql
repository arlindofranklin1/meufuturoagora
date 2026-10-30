-- Frequência e pontuação. Os casos de pontuação repetem os testes JVM de
-- caracterização (PontuacaoUtilTest, PenalidadeFaltaUtilTest), agora contra
-- privado.pontuacao_aluno, que substitui PontuacaoUtil.
begin;
select * from no_plan();
select testes.cenario();

-- Duas aulas no mesmo dia e uma em outro dia, todas em d1
insert into public.aulas (id, disciplina_id, data, quantidade) values
  (testes.guardar('aula1', gen_random_uuid()), testes.id('d1'), '2026-10-20', 2),
  (testes.guardar('aula2', gen_random_uuid()), testes.id('d1'), '2026-10-20', 1),
  (testes.guardar('aula3', gen_random_uuid()), testes.id('d1'), '2026-10-21', 1);

-- ===== Aulas =====

select testes.entrar_como('p1');
select lives_ok(
  format($$insert into public.aulas (disciplina_id, data, quantidade) values (%L, %L, 2)$$,
         testes.id('d1'), (now() at time zone 'America/Sao_Paulo')::date),
  'professor cadastra aula na própria disciplina');
set local role postgres;
select is((select bimestre from public.aulas where disciplina_id = testes.id('d1') and quantidade = 2
           and data = (now() at time zone 'America/Sao_Paulo')::date), 4::smallint,
  'bimestre da aula vem da data da aula');

select testes.entrar_como('p2');
select throws_ok(
  format($$insert into public.aulas (disciplina_id, data, quantidade) values (%L, '2026-10-22', 1)$$, testes.id('d1')),
  '42501', null, 'outro professor não cadastra aula');
select testes.entrar_como('a1');
select is((select count(*)::int from public.aulas), 4, 'aluno vê as aulas das suas disciplinas');
select throws_ok(
  format($$insert into public.aulas (disciplina_id, data, quantidade) values (%L, '2026-10-22', 1)$$, testes.id('d1')),
  '42501', null, 'aluno não cadastra aula');

-- ===== registrar_frequencia =====

select testes.entrar_como('a1');
select throws_ok(
  format($$select public.registrar_frequencia(%L, '[]')$$, testes.id('aula1')),
  '42501', null, 'aluno não registra frequência');
select throws_ok(
  format($$insert into public.frequencias (aula_id, aluno_id, status) values (%L, %L, 'presente')$$,
         testes.id('aula1'), testes.id('a1')),
  '42501', null, 'aluno não grava frequência direto');

select testes.entrar_como('p2');
select throws_ok(
  format($$select public.registrar_frequencia(%L, '[]')$$, testes.id('aula1')),
  '42501', null, 'outro professor não registra frequência');

select testes.entrar_como('p1');
select is(
  public.registrar_frequencia(testes.id('aula1'), jsonb_build_array(
    jsonb_build_object('aluno_id', testes.id('a1'), 'status', 'falta'),
    jsonb_build_object('aluno_id', testes.id('a2'), 'status', 'presente'))),
  2, 'professor registra a frequência da aula');

-- Atomicidade: um registro inválido e nada da lista é gravado
select throws_ok(
  format($$select public.registrar_frequencia(%L, %L)$$, testes.id('aula2'), jsonb_build_array(
    jsonb_build_object('aluno_id', testes.id('a1'), 'status', 'presente'),
    jsonb_build_object('aluno_id', testes.id('a3'), 'status', 'presente'))),
  'P0001', null, 'aluno não matriculado na lista é recusado');
select throws_ok(
  format($$select public.registrar_frequencia(%L, %L)$$, testes.id('aula2'), jsonb_build_array(
    jsonb_build_object('aluno_id', testes.id('a1'), 'status', 'presente'),
    jsonb_build_object('aluno_id', testes.id('a2'), 'status', 'Falta'))),
  '23514', null, 'status fora da lista é recusado');
select throws_ok(
  format($$select public.registrar_frequencia(%L, %L)$$, testes.id('aula2'), jsonb_build_array(
    jsonb_build_object('aluno_id', testes.id('a1'), 'status', 'presente'),
    jsonb_build_object('aluno_id', testes.id('a1'), 'status', 'falta'))),
  'P0001', null, 'aluno repetido é recusado');
select throws_ok(
  format($$select public.registrar_frequencia(%L, '{"a": 1}')$$, testes.id('aula2')),
  'P0001', null, 'lista que não é array é recusada');
select is((select count(*)::int from public.frequencias where aula_id = testes.id('aula2')), 0,
  'nada foi gravado nas tentativas que falharam');

select is(
  public.registrar_frequencia(testes.id('aula1'), jsonb_build_array(
    jsonb_build_object('aluno_id', testes.id('a1'), 'status', 'falta_justificada'))),
  1, 'registrar de novo atualiza o status');
select is((select status from public.frequencias where aula_id = testes.id('aula1') and aluno_id = testes.id('a1')),
  'falta_justificada', 'o status novo vale');

-- Quem vê a frequência
select testes.entrar_como('a2');
select results_eq('select aluno_id from public.frequencias', array[testes.id('a2')],
  'aluno vê só a própria frequência');

-- ===== Pontuação (mesmos casos dos testes JVM) =====

set local role postgres;
delete from public.frequencias;

-- Atividades e entregas avaliadas da Ana (a1) em d1
insert into public.atividades (id, disciplina_id, nome, pontos, prazo) values
  (testes.guardar('t2', gen_random_uuid()), testes.id('d1'), 'T2', 10, '2026-12-01'),
  (testes.guardar('t3', gen_random_uuid()), testes.id('d2'), 'T3', 10, '2026-12-01');

create function pg_temp.avaliar(p_atividade text, p_aluno text, p_nota numeric) returns void
language sql as $$
  insert into public.entregas (atividade_id, aluno_id, comentario_aluno)
  values (testes.id(p_atividade), testes.id(p_aluno), 'ok')
  on conflict (atividade_id, aluno_id) do nothing;
  update public.entregas set nota = p_nota, avaliado_em = now()
  where atividade_id = testes.id(p_atividade) and aluno_id = testes.id(p_aluno);
$$;

create function pg_temp.frequencia(p_aula text, p_aluno text, p_status text) returns void
language sql as $$
  insert into public.frequencias (aula_id, aluno_id, status)
  values (testes.id(p_aula), testes.id(p_aluno), p_status)
  on conflict (aula_id, aluno_id) do update set status = excluded.status;
$$;

create function pg_temp.pontos(p_aluno text) returns integer
language sql as $$ select privado.pontuacao_aluno(testes.id(p_aluno), 2026::smallint) $$;

select is(pg_temp.pontos('a1'), 0, 'sem notas nem faltas: 0');

select pg_temp.avaliar('t1', 'a1', 8);
select pg_temp.avaliar('t2', 'a1', 6);
select pg_temp.frequencia('aula1', 'a1', 'falta');
select is(pg_temp.pontos('a1'), 9, 'soma as notas e desconta 5 por dia de falta');

select pg_temp.frequencia('aula2', 'a1', 'falta');
select is(pg_temp.pontos('a1'), 9, 'duas faltas no mesmo dia descontam uma vez só');

select pg_temp.frequencia('aula3', 'a1', 'falta');
select is(pg_temp.pontos('a1'), 4, 'falta em outro dia desconta de novo');

select pg_temp.frequencia('aula3', 'a1', 'falta_justificada');
select is(pg_temp.pontos('a1'), 9, 'falta justificada não desconta');

select pg_temp.frequencia('aula1', 'a1', 'presente');
select pg_temp.frequencia('aula2', 'a1', 'presente');
select is(pg_temp.pontos('a1'), 14, 'corrigir a falta para presença devolve os pontos');

update public.entregas set nota = null, avaliado_em = null where atividade_id = testes.id('t2');
select is(pg_temp.pontos('a1'), 8, 'entrega não avaliada não conta');

-- Arredondamento igual ao Math.round do Java
select pg_temp.avaliar('t1', 'a2', 7.5);
select is(pg_temp.pontos('a2'), 8, 'arredonda meio para cima (7,5 -> 8)');
select pg_temp.avaliar('t1', 'a2', 2.5);
select pg_temp.frequencia('aula1', 'a2', 'falta');
select is(pg_temp.pontos('a2'), -2, 'total negativo arredonda para cima (-2,5 -> -2)');
delete from public.entregas where aluno_id = testes.id('a2');
select is(pg_temp.pontos('a2'), -5, 'só falta deixa a pontuação negativa');

-- Disciplina desativada e outro ano letivo
select pg_temp.avaliar('t3', 'a1', 5);
select is(pg_temp.pontos('a1'), 13, 'nota de outra disciplina ativa conta');
update public.disciplinas set ativo = false where id = testes.id('d2');
select is(pg_temp.pontos('a1'), 8, 'nota de disciplina desativada não conta');
update public.entregas set ano_letivo = 2025 where atividade_id = testes.id('t1') and aluno_id = testes.id('a1');
select is(pg_temp.pontos('a1'), 0, 'nota de outro ano letivo não conta');
select is(privado.pontuacao_aluno(testes.id('a1'), 2025::smallint), 8, 'mas conta no próprio ano');
update public.aulas set ano_letivo = 2025 where id = testes.id('aula1');
select is(pg_temp.pontos('a2'), 0, 'falta de outro ano letivo não desconta');

-- ===== public.pontuacao: quem pode ver =====

select testes.entrar_como('a1');
select is(public.pontuacao(testes.id('a1')), 0, 'aluno vê a própria pontuação');
select throws_ok(format('select public.pontuacao(%L)', testes.id('a2')), '42501', null,
  'aluno não vê a pontuação de outro aluno');
select testes.entrar_como('p1');
select lives_ok(format('select public.pontuacao(%L)', testes.id('a1')), 'professor vê a pontuação dos seus alunos');
select testes.entrar_como('p2');
select throws_ok(format('select public.pontuacao(%L)', testes.id('a3')), '42501', null,
  'professor não vê a pontuação de aluno que não é dele');
select testes.entrar_como('adm');
select is(public.pontuacao(testes.id('a1'), 2025), 8, 'admin vê a pontuação de qualquer ano');

select * from finish();
rollback;
