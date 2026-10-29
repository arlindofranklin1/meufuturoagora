-- Funções auxiliares (usadas pelas políticas RLS) e gatilhos.
--
-- As auxiliares são "security definer" porque consultam perfis, disciplinas e
-- matrículas sem passar pelo RLS dessas tabelas (senão as políticas entrariam
-- em recursão). Todas fixam search_path = '' e só olham para o usuário logado
-- (auth.uid()), nunca para um id recebido como "quem sou eu".

-- =========================================================
-- QUEM É O USUÁRIO LOGADO
-- =========================================================

-- Perfil ativo vinculado à conta logada, ou nulo
create function privado.perfil_atual_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select p.id
  from public.perfis p
  where p.usuario_id = (select auth.uid())
    and p.ativo
$$;

create function privado.eh_admin()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.perfis p
    where p.usuario_id = (select auth.uid())
      and p.ativo
      and p.admin
  )
$$;

create function privado.eh_professor()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.perfis p
    where p.usuario_id = (select auth.uid())
      and p.ativo
      and p.tipo = 'professor'
  )
$$;

create function privado.eh_aluno()
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.perfis p
    where p.usuario_id = (select auth.uid())
      and p.ativo
      and p.tipo = 'aluno'
  )
$$;

-- =========================================================
-- VÍNCULOS COM DISCIPLINAS
-- =========================================================

create function privado.eh_professor_da_disciplina(p_disciplina uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.disciplinas d
    where d.id = p_disciplina
      and d.professor_id = (select privado.perfil_atual_id())
  )
$$;

create function privado.eh_professor_da_atividade(p_atividade uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.atividades a
    join public.disciplinas d on d.id = a.disciplina_id
    where a.id = p_atividade
      and d.professor_id = (select privado.perfil_atual_id())
  )
$$;

create function privado.eh_professor_da_aula(p_aula uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.aulas a
    join public.disciplinas d on d.id = a.disciplina_id
    where a.id = p_aula
      and d.professor_id = (select privado.perfil_atual_id())
  )
$$;

-- Aluno logado matriculado numa disciplina ativa (disciplina excluída some para o aluno)
create function privado.aluno_matriculado(p_disciplina uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.matriculas m
    join public.disciplinas d on d.id = m.disciplina_id
    where m.disciplina_id = p_disciplina
      and m.aluno_id = (select privado.perfil_atual_id())
      and d.ativo
  )
$$;

create function privado.aluno_matriculado_na_atividade(p_atividade uuid)
returns boolean
language sql
stable
security definer
set search_path = ''
as $$
  select exists (
    select 1
    from public.atividades a
    join public.matriculas m on m.disciplina_id = a.disciplina_id
    join public.disciplinas d on d.id = a.disciplina_id
    where a.id = p_atividade
      and m.aluno_id = (select privado.perfil_atual_id())
      and d.ativo
  )
$$;

-- Pastas do Storage vêm como texto; um texto que não é uuid vira nulo em vez de erro
create function privado.uuid_ou_nulo(p_texto text)
returns uuid
language plpgsql
immutable
set search_path = ''
as $$
begin
  return p_texto::uuid;
exception
  when invalid_text_representation then
    return null;
end;
$$;

grant execute on function
  privado.perfil_atual_id(),
  privado.eh_admin(),
  privado.eh_professor(),
  privado.eh_aluno(),
  privado.eh_professor_da_disciplina(uuid),
  privado.eh_professor_da_atividade(uuid),
  privado.eh_professor_da_aula(uuid),
  privado.aluno_matriculado(uuid),
  privado.aluno_matriculado_na_atividade(uuid),
  privado.uuid_ou_nulo(text)
to authenticated;

-- =========================================================
-- DATAS, ANO LETIVO E BIMESTRE
-- =========================================================

-- A escola fica no Brasil: "hoje" e os prazos seguem o horário de Brasília
create function privado.hoje()
returns date
language sql
stable
set search_path = ''
as $$
  select (now() at time zone 'America/Sao_Paulo')::date
$$;

-- Sem ano letivo definido vale o ano do calendário (AnoLetivoUtil.atual)
create function privado.ano_letivo_atual()
returns smallint
language sql
stable
security definer
set search_path = ''
as $$
  select coalesce(
    (select a.ano from public.anos_letivos a where a.atual),
    extract(year from privado.hoje())::smallint
  )
$$;

-- 1 a 4, ou nulo fora dos bimestres; com sobreposição vale o primeiro (BimestreUtil.numeroDaData)
create function privado.bimestre_da_data(p_ano smallint, p_data date)
returns smallint
language sql
stable
security definer
set search_path = ''
as $$
  select b.numero
  from public.bimestres b
  where b.ano = p_ano
    and p_data between b.inicio and b.fim
  order by b.numero
  limit 1
$$;

-- =========================================================
-- GATILHOS GENÉRICOS
-- =========================================================

create function privado.tocar_atualizado_em()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  new.atualizado_em := now();
  return new;
end;
$$;

create trigger turmas_atualizado_em before update on public.turmas
  for each row execute function privado.tocar_atualizado_em();
create trigger perfis_atualizado_em before update on public.perfis
  for each row execute function privado.tocar_atualizado_em();
create trigger disciplinas_atualizado_em before update on public.disciplinas
  for each row execute function privado.tocar_atualizado_em();
create trigger atividades_atualizado_em before update on public.atividades
  for each row execute function privado.tocar_atualizado_em();
create trigger aulas_atualizado_em before update on public.aulas
  for each row execute function privado.tocar_atualizado_em();
create trigger diario_entradas_atualizado_em before update on public.diario_entradas
  for each row execute function privado.tocar_atualizado_em();

-- Ano letivo e bimestre são do servidor, não do celular (AnoLetivoUtil.marcar).
-- A aula usa a própria data; os demais registros, a data de hoje.
create function privado.preencher_periodo()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'INSERT' then
    new.ano_letivo := privado.ano_letivo_atual();
  end if;

  if tg_table_name = 'aulas' then
    new.bimestre := privado.bimestre_da_data(new.ano_letivo, new.data);
  else
    new.bimestre := privado.bimestre_da_data(new.ano_letivo, privado.hoje());
  end if;

  return new;
end;
$$;

create trigger disciplinas_periodo before insert on public.disciplinas
  for each row execute function privado.preencher_periodo();
create trigger atividades_periodo before insert on public.atividades
  for each row execute function privado.preencher_periodo();
create trigger entregas_periodo before insert on public.entregas
  for each row execute function privado.preencher_periodo();
create trigger aulas_periodo before insert or update of data on public.aulas
  for each row execute function privado.preencher_periodo();

-- =========================================================
-- ENTREGAS
-- =========================================================

-- Horário de envio e atraso são do servidor (decisão D4); a avaliação começa vazia
create function privado.preparar_entrega()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_prazo date;
begin
  select a.prazo into v_prazo
  from public.atividades a
  where a.id = new.atividade_id;

  new.enviado_em := now();
  new.criado_em := now();
  new.dias_atraso := greatest(0, privado.hoje() - v_prazo);
  new.nota := null;
  new.avaliado_em := null;
  new.comentario_professor := null;

  return new;
end;
$$;

create trigger entregas_preparar before insert on public.entregas
  for each row execute function privado.preparar_entrega();

-- =========================================================
-- PERFIS
-- =========================================================

-- Pela API, quem não é admin só troca a própria foto. As funções do servidor
-- (vincular_conta etc.) rodam como "postgres" e não passam por esta regra.
create function privado.proteger_perfil()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if current_user = 'authenticated' and not privado.eh_admin() then

    if (new.email, new.nome, new.tipo, new.admin, new.ativo, new.turma_id,
        new.usuario_id, new.foto_google_url)
       is distinct from
       (old.email, old.nome, old.tipo, old.admin, old.ativo, old.turma_id,
        old.usuario_id, old.foto_google_url) then

      raise exception 'Só o administrador pode alterar estes dados do perfil.'
        using errcode = '42501';
    end if;
  end if;

  return new;
end;
$$;

create trigger perfis_proteger before update on public.perfis
  for each row execute function privado.proteger_perfil();

-- E-mail trocado pelo admin: a conta ligada ao e-mail antigo perde o acesso na
-- hora. A conta do e-mail novo se liga no próximo login (vincular_conta).
create function privado.desvincular_ao_trocar_email()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if lower(new.email::text) <> lower(old.email::text) then
    new.usuario_id := null;
  end if;

  return new;
end;
$$;

create trigger perfis_desvincular_email before update of email on public.perfis
  for each row execute function privado.desvincular_ao_trocar_email();

-- Decisão D14: sempre precisa haver ao menos um admin ativo
create function privado.garantir_um_admin()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if exists (select 1 from public.perfis)
     and not exists (select 1 from public.perfis p where p.admin and p.ativo) then

    raise exception 'É preciso haver ao menos um administrador ativo.'
      using errcode = 'P0001';
  end if;

  return null;
end;
$$;

create trigger perfis_um_admin after update or delete on public.perfis
  for each statement execute function privado.garantir_um_admin();

-- =========================================================
-- DISCIPLINAS
-- =========================================================

-- O responsável precisa ser professor
create function privado.validar_professor_da_disciplina()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if not exists (
    select 1 from public.perfis p
    where p.id = new.professor_id and p.tipo = 'professor'
  ) then
    raise exception 'O responsável pela disciplina precisa ser um professor.'
      using errcode = '23514';
  end if;

  return new;
end;
$$;

create trigger disciplinas_validar_professor before insert or update of professor_id on public.disciplinas
  for each row execute function privado.validar_professor_da_disciplina();

-- O professor renomeia e desativa as suas (decisão D1); trocar o responsável ou a
-- turma é com o admin (transferir_disciplinas, D13)
create function privado.proteger_disciplina()
returns trigger
language plpgsql
set search_path = ''
as $$
begin
  if current_user = 'authenticated' and not privado.eh_admin()
     and (new.professor_id, new.turma_id) is distinct from (old.professor_id, old.turma_id) then

    raise exception 'Só o administrador pode trocar o professor ou a turma da disciplina.'
      using errcode = '42501';
  end if;

  return new;
end;
$$;

create trigger disciplinas_proteger before update on public.disciplinas
  for each row execute function privado.proteger_disciplina();

-- =========================================================
-- MATRÍCULA AUTOMÁTICA (decisão D9)
-- =========================================================

-- Disciplina criada, reativada ou trocada de turma: matrícula = alunos ativos da turma
create function privado.sincronizar_matriculas_da_disciplina()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if tg_op = 'UPDATE' and old.turma_id is distinct from new.turma_id then
    delete from public.matriculas m
    where m.disciplina_id = new.id
      and not exists (
        select 1 from public.perfis p
        where p.id = m.aluno_id and p.turma_id = new.turma_id
      );
  end if;

  if new.turma_id is not null and new.ativo then
    insert into public.matriculas (disciplina_id, aluno_id)
    select new.id, p.id
    from public.perfis p
    where p.turma_id = new.turma_id
      and p.tipo = 'aluno'
      and p.ativo
    on conflict do nothing;
  end if;

  return null;
end;
$$;

create trigger disciplinas_matriculas after insert or update of turma_id, ativo on public.disciplinas
  for each row execute function privado.sincronizar_matriculas_da_disciplina();

-- Aluno cadastrado, reativado ou trocado de turma: sai das disciplinas da turma
-- antiga e entra nas da nova. Entregas e frequências antigas continuam salvas.
create function privado.sincronizar_matriculas_do_aluno()
returns trigger
language plpgsql
security definer
set search_path = ''
as $$
begin
  if new.tipo <> 'aluno' then
    delete from public.matriculas m where m.aluno_id = new.id;
    return null;
  end if;

  if tg_op = 'UPDATE' and old.turma_id is distinct from new.turma_id then
    delete from public.matriculas m
    using public.disciplinas d
    where m.aluno_id = new.id
      and d.id = m.disciplina_id
      and d.turma_id is distinct from new.turma_id;
  end if;

  if new.turma_id is not null and new.ativo then
    insert into public.matriculas (disciplina_id, aluno_id)
    select d.id, new.id
    from public.disciplinas d
    where d.turma_id = new.turma_id
      and d.ativo
    on conflict do nothing;
  end if;

  return null;
end;
$$;

create trigger perfis_matriculas after insert or update of turma_id, tipo, ativo on public.perfis
  for each row execute function privado.sincronizar_matriculas_do_aluno();
