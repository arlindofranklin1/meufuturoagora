-- Histórico do administrador (HistoricoAnoLetivoActivity): consulta por ano
-- letivo e bimestre a pontuação dos alunos, as trilhas e as entregas.
--
-- Antes o app baixava 8 coleções inteiras e calculava no celular. Agora são
-- três funções que devolvem só o período pedido, calculadas no servidor com a
-- mesma regra da pontuação (soma das notas avaliadas, menos 5 por dia com
-- falta sem justificativa, arredondada como o Math.round do Java).
--
-- p_bimestre nulo ou 0 quer dizer o ano inteiro. Registros fora de qualquer
-- bimestre (bimestre nulo) só aparecem no ano inteiro, como no app antigo.
-- Só administradores chamam (privado.exigir_admin).

-- Anos que têm algum registro, mais os cadastrados em anos_letivos
create function public.historico_anos()
returns table (ano smallint)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform privado.exigir_admin();

  return query
  select u.ano
  from (
    select a.ano from public.anos_letivos a
    union select d.ano_letivo from public.disciplinas d
    union select t.ano_letivo from public.atividades t
    union select e.ano_letivo from public.entregas e
    union select au.ano_letivo from public.aulas au
  ) u
  order by u.ano desc;
end;
$$;

-- Aluno que teve entrega ou falta no período, do maior para o menor número de pontos
create function public.historico_alunos(p_ano integer, p_bimestre integer default null)
returns table (aluno_id uuid, nome text, turma_nome text, entregas integer, dias_falta integer, pontos integer)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform privado.exigir_admin();

  return query
  with entregas_do_periodo as (
    select e.aluno_id, count(*)::integer as qtd, coalesce(sum(e.nota), 0) as soma_notas
    from public.entregas e
    where e.ano_letivo = p_ano
      and (coalesce(p_bimestre, 0) = 0 or e.bimestre = p_bimestre)
    group by e.aluno_id
  ),
  faltas_do_periodo as (
    select f.aluno_id, count(distinct au.data)::integer as dias
    from public.frequencias f
    join public.aulas au on au.id = f.aula_id
    where f.status = 'falta'
      and au.ano_letivo = p_ano
      and (coalesce(p_bimestre, 0) = 0 or au.bimestre = p_bimestre)
    group by f.aluno_id
  )
  select p.id, p.nome, t.nome,
         coalesce(en.qtd, 0),
         coalesce(fa.dias, 0),
         floor(coalesce(en.soma_notas, 0) - 5 * coalesce(fa.dias, 0) + 0.5)::integer
  from public.perfis p
  left join entregas_do_periodo en on en.aluno_id = p.id
  left join faltas_do_periodo fa on fa.aluno_id = p.id
  left join public.turmas t on t.id = p.turma_id
  where p.tipo = 'aluno'
    and (en.aluno_id is not null or fa.aluno_id is not null)
  order by 6 desc, lower(p.nome), p.id;
end;
$$;

-- Trilhas criadas no período, as mais recentes primeiro. "entregas" conta todas
-- as entregas da trilha, não só as do período.
create function public.historico_trilhas(p_ano integer, p_bimestre integer default null)
returns table (atividade_id uuid, nome text, disciplina_nome text, professor_nome text,
               turma_nome text, prazo date, pontos integer, entregas integer, criado_em timestamptz)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform privado.exigir_admin();

  return query
  select a.id, a.nome, d.nome, pr.nome, t.nome, a.prazo, a.pontos::integer,
         (select count(*)::integer from public.entregas e where e.atividade_id = a.id),
         a.criado_em
  from public.atividades a
  join public.disciplinas d on d.id = a.disciplina_id
  join public.perfis pr on pr.id = d.professor_id
  left join public.turmas t on t.id = d.turma_id
  where a.ano_letivo = p_ano
    and (coalesce(p_bimestre, 0) = 0 or a.bimestre = p_bimestre)
  order by a.criado_em desc, a.id;
end;
$$;

-- Entregas enviadas no período, as mais recentes primeiro. A turma é a do aluno
-- (ou a da disciplina, se o aluno ficou sem turma); nota nula = pendente.
create function public.historico_entregas(p_ano integer, p_bimestre integer default null)
returns table (entrega_id uuid, aluno_nome text, atividade_nome text, disciplina_nome text,
               turma_nome text, enviado_em timestamptz, dias_atraso integer, nota numeric)
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  perform privado.exigir_admin();

  return query
  select e.id, al.nome, a.nome, d.nome, coalesce(ta.nome, td.nome), e.enviado_em,
         e.dias_atraso::integer, e.nota
  from public.entregas e
  join public.perfis al on al.id = e.aluno_id
  join public.atividades a on a.id = e.atividade_id
  join public.disciplinas d on d.id = a.disciplina_id
  left join public.turmas ta on ta.id = al.turma_id
  left join public.turmas td on td.id = d.turma_id
  where e.ano_letivo = p_ano
    and (coalesce(p_bimestre, 0) = 0 or e.bimestre = p_bimestre)
  order by e.enviado_em desc, e.id;
end;
$$;

revoke all on function
  public.historico_anos(),
  public.historico_alunos(integer, integer),
  public.historico_trilhas(integer, integer),
  public.historico_entregas(integer, integer)
from public, anon;

grant execute on function
  public.historico_anos(),
  public.historico_alunos(integer, integer),
  public.historico_trilhas(integer, integer),
  public.historico_entregas(integer, integer)
to authenticated;
