-- Funções chamadas pelo app via /rest/v1/rpc. Cada uma é uma transação:
-- se falhar no meio, nada é gravado.
--
-- São "security definer" porque gravam em tabelas que a API não deixa gravar
-- (frequências, notas, ano letivo, exclusões). Por isso cada uma confere,
-- logo no início, se o usuário logado pode fazer aquilo.
--
-- Erros: 42501 = sem permissão (a API devolve 403); P0001 = regra de negócio
-- violada (400), com a mensagem pronta para mostrar ao usuário.
--
-- Atenção ao citext: com search_path = '' o operador "=" do citext (que fica
-- no esquema extensions) não é encontrado, e a comparação viraria texto
-- sensível a maiúsculas. Por isso os e-mails são comparados com lower().

-- =========================================================
-- INTERNAS (sem grant para a API)
-- =========================================================

create function privado.exigir_admin()
returns void
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not privado.eh_admin() then
    raise exception 'Apenas administradores podem fazer isto.' using errcode = '42501';
  end if;
end;
$$;

-- Regra do ranking (PontuacaoUtil.java:21): soma das notas das trilhas avaliadas
-- em disciplinas ativas, menos 5 pontos por dia com ao menos uma falta sem
-- justificativa, só no ano letivo informado. floor(x + 0.5) reproduz o
-- Math.round do Java (-2,5 vira -2), diferente do round() do Postgres.
create function privado.pontuacao_aluno(p_aluno uuid, p_ano smallint)
returns integer
language sql
stable
set search_path = ''
as $$
  select floor(
    coalesce((
      select sum(e.nota)
      from public.entregas e
      join public.atividades a on a.id = e.atividade_id
      join public.disciplinas d on d.id = a.disciplina_id
      where e.aluno_id = p_aluno
        and e.avaliado_em is not null
        and d.ativo
        and e.ano_letivo = p_ano
    ), 0)
    - 5 * (
      select count(distinct au.data)
      from public.frequencias f
      join public.aulas au on au.id = f.aula_id
      where f.aluno_id = p_aluno
        and f.status = 'falta'
        and au.ano_letivo = p_ano
    )
    + 0.5
  )::integer
$$;

-- Ranking completo da turma: alunos ativos que já entraram no app, por pontos
-- e, no empate, por nome sem diferenciar maiúsculas (RankingActivity.java:459)
create function privado.ranking_completo(p_turma uuid, p_ano smallint)
returns table (perfil_id uuid, nome text, foto_google_url text, foto_path text, pontos integer, posicao integer)
language sql
stable
set search_path = ''
as $$
  with base as (
    select p.id, p.nome, p.foto_google_url, p.foto_path,
           privado.pontuacao_aluno(p.id, p_ano) as pontos
    from public.perfis p
    where p.turma_id = p_turma
      and p.tipo = 'aluno'
      and p.ativo
      and p.usuario_id is not null
  )
  select b.id, b.nome, b.foto_google_url, b.foto_path, b.pontos,
         (row_number() over (order by b.pontos desc, lower(b.nome), b.id))::integer
  from base b
$$;

-- Exclusão definitiva (decisão D7). Os arquivos do Storage são removidos pelo
-- app antes, pela API do Storage, porque o Storage não deixa apagar por SQL.
create function privado.excluir_perfil(p_perfil uuid, p_solicitacao uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_perfil public.perfis;
begin
  select * into v_perfil from public.perfis p where p.id = p_perfil for update;

  if not found then
    raise exception 'Usuário não encontrado.' using errcode = 'P0002';
  end if;

  -- Decisão D13
  if v_perfil.tipo = 'professor'
     and exists (select 1 from public.disciplinas d where d.professor_id = p_perfil) then
    raise exception 'Transfira as disciplinas deste professor para outro antes de excluir.'
      using errcode = 'P0001';
  end if;

  insert into public.exclusoes_realizadas (solicitacao_id, tipo_perfil, decidido_por)
  values (p_solicitacao, v_perfil.tipo, privado.perfil_atual_id());

  -- Em cascata: matrículas, entregas, frequências, diário e pedidos.
  -- O gatilho garantir_um_admin impede apagar o último admin (D14).
  delete from public.perfis p where p.id = p_perfil;

  -- A conta de login também sai; as sessões e tokens de renovação vão junto
  if v_perfil.usuario_id is not null then
    delete from auth.users u where u.id = v_perfil.usuario_id;
  end if;
end;
$$;

-- =========================================================
-- LOGIN
-- =========================================================

-- Liga a conta logada ao cadastro feito pelo admin, pelo e-mail verificado da
-- conta (nunca por um e-mail enviado pelo app). Substitui a lógica de
-- TelaEntrar.verificarUsuario e DisciplinaUtil.vincularAoProfessor: se a conta
-- Google for recriada, o perfil (com tudo o que é dele) é religado à nova.
create function public.vincular_conta()
returns public.perfis
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_usuario_id uuid := (select auth.uid());
  v_email text;
  v_confirmado timestamptz;
  v_meta jsonb;
  v_foto text;
  v_perfil public.perfis;
begin
  select u.email, u.email_confirmed_at, u.raw_user_meta_data
    into v_email, v_confirmado, v_meta
  from auth.users u
  where u.id = v_usuario_id;

  if v_email is null or v_confirmado is null then
    raise exception 'Conta sem e-mail verificado.' using errcode = '42501';
  end if;

  select * into v_perfil
  from public.perfis p
  where lower(p.email::text) = lower(v_email);

  if not found or not v_perfil.ativo
     or (v_perfil.tipo = 'aluno' and v_perfil.turma_id is null) then
    raise exception 'Seu e-mail não está autorizado a acessar o aplicativo. Fale com a coordenação da escola.'
      using errcode = '42501';
  end if;

  -- user_metadata é editável pelo usuário: serve só para a foto, e só se for do Google
  v_foto := coalesce(v_meta ->> 'avatar_url', v_meta ->> 'picture');

  if v_foto !~ '^https://[a-z0-9-]+\.googleusercontent\.com/' then
    v_foto := null;
  end if;

  -- Se o admin trocou o e-mail de outro perfil, a conta deixa de ser daquele
  update public.perfis p
  set usuario_id = null
  where p.usuario_id = v_usuario_id
    and p.id <> v_perfil.id;

  update public.perfis p
  set usuario_id = v_usuario_id,
      foto_google_url = v_foto
  where p.id = v_perfil.id
  returning * into v_perfil;

  return v_perfil;
end;
$$;

-- =========================================================
-- ANO LETIVO E BIMESTRES (admin)
-- =========================================================

-- Substitui DefinirAnoLetivoActivity.salvarAnoLetivo, sem senha (decisão N5).
-- O ano anterior fica arquivado com os próprios bimestres; a pontuação não
-- precisa ser recalculada porque é sempre calculada na hora.
create function public.definir_ano_letivo(p_ano integer)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform privado.exigir_admin();

  if p_ano is null or p_ano not between 2000 and 2100 then
    raise exception 'Ano letivo inválido.' using errcode = 'P0001';
  end if;

  update public.anos_letivos a
  set atual = false,
      encerrado_em = now()
  where a.atual
    and a.ano <> p_ano;

  insert into public.anos_letivos (ano, atual)
  values (p_ano, true)
  on conflict (ano) do update
    set atual = true,
        encerrado_em = null;
end;
$$;

-- p_bimestres: [{"numero": 1, "inicio": "2026-02-02", "fim": "2026-04-30"}, ...]
-- Bimestres não enviados ficam indefinidos, como hoje.
create function public.definir_bimestres(p_ano integer, p_bimestres jsonb)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform privado.exigir_admin();

  if not exists (select 1 from public.anos_letivos a where a.ano = p_ano) then
    raise exception 'Defina o ano letivo antes dos bimestres.' using errcode = 'P0001';
  end if;

  if jsonb_typeof(p_bimestres) is distinct from 'array' then
    raise exception 'Lista de bimestres inválida.' using errcode = 'P0001';
  end if;

  if exists (
    select 1 from jsonb_to_recordset(p_bimestres) as n (numero smallint, inicio date, fim date)
    where n.inicio is null or n.fim is null
  ) then
    raise exception 'Informe o início e o fim de cada bimestre.' using errcode = 'P0001';
  end if;

  if exists (
    select 1 from jsonb_to_recordset(p_bimestres) as n (numero smallint)
    group by n.numero having count(*) > 1
  ) then
    raise exception 'Bimestre repetido.' using errcode = 'P0001';
  end if;

  if exists (
    select 1
    from (
      select n.inicio, lag(n.fim) over (order by n.numero) as fim_anterior
      from jsonb_to_recordset(p_bimestres) as n (numero smallint, inicio date, fim date)
    ) t
    where t.inicio <= t.fim_anterior
  ) then
    raise exception 'Os bimestres precisam estar em ordem e sem sobreposição.' using errcode = 'P0001';
  end if;

  delete from public.bimestres b where b.ano = p_ano;

  insert into public.bimestres (ano, numero, inicio, fim)
  select p_ano, n.numero, n.inicio, n.fim
  from jsonb_to_recordset(p_bimestres) as n (numero smallint, inicio date, fim date);
end;
$$;

-- =========================================================
-- FREQUÊNCIA (professor da disciplina)
-- =========================================================

-- p_registros: [{"aluno_id": "...", "status": "presente" | "falta" | "falta_justificada"}]
-- Substitui o laço de FrequenciaAulaActivity e o PenalidadeFaltaUtil: o desconto
-- de 5 pontos por dia sai da própria frequência (pontuacao_aluno).
create function public.registrar_frequencia(p_aula uuid, p_registros jsonb)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_disciplina uuid;
  v_total integer;
begin
  select a.disciplina_id into v_disciplina from public.aulas a where a.id = p_aula;

  if v_disciplina is null
     or not (privado.eh_admin() or privado.eh_professor_da_disciplina(v_disciplina)) then
    raise exception 'Sem permissão para registrar a frequência desta aula.' using errcode = '42501';
  end if;

  if jsonb_typeof(p_registros) is distinct from 'array' then
    raise exception 'Lista de frequência inválida.' using errcode = 'P0001';
  end if;

  if exists (
    select 1 from jsonb_to_recordset(p_registros) as n (aluno_id uuid)
    group by n.aluno_id having count(*) > 1
  ) then
    raise exception 'Aluno repetido na lista de frequência.' using errcode = 'P0001';
  end if;

  if exists (
    select 1 from jsonb_to_recordset(p_registros) as n (aluno_id uuid)
    where not exists (
      select 1 from public.matriculas m
      where m.disciplina_id = v_disciplina and m.aluno_id = n.aluno_id
    )
  ) then
    raise exception 'A lista tem aluno que não está matriculado nesta disciplina.' using errcode = 'P0001';
  end if;

  insert into public.frequencias (aula_id, aluno_id, status)
  select p_aula, n.aluno_id, n.status
  from jsonb_to_recordset(p_registros) as n (aluno_id uuid, status text)
  on conflict (aula_id, aluno_id) do update
    set status = excluded.status,
        registrado_em = now();

  get diagnostics v_total = row_count;
  return v_total;
end;
$$;

-- =========================================================
-- AVALIAÇÃO (professor da disciplina)
-- =========================================================

-- Decisão D3: nota de 0 até os pontos da trilha. Pode reavaliar (AvaliarEntregaActivity)
create function public.avaliar_entrega(p_entrega uuid, p_nota numeric, p_comentario text default null)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_atividade uuid;
  v_pontos smallint;
begin
  select e.atividade_id, a.pontos into v_atividade, v_pontos
  from public.entregas e
  join public.atividades a on a.id = e.atividade_id
  where e.id = p_entrega;

  if v_atividade is null
     or not (privado.eh_admin() or privado.eh_professor_da_atividade(v_atividade)) then
    raise exception 'Sem permissão para avaliar esta entrega.' using errcode = '42501';
  end if;

  if p_nota is null or p_nota < 0 or p_nota > v_pontos then
    raise exception 'A nota precisa ficar entre 0 e %.', v_pontos using errcode = 'P0001';
  end if;

  update public.entregas e
  set nota = round(p_nota, 2),
      comentario_professor = nullif(btrim(p_comentario), ''),
      avaliado_em = now()
  where e.id = p_entrega;
end;
$$;

-- QuestoesDoLivroActivity.refazerAvaliacao: volta a aguardar avaliação
create function public.refazer_avaliacao(p_entrega uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_atividade uuid;
begin
  select e.atividade_id into v_atividade from public.entregas e where e.id = p_entrega;

  if v_atividade is null
     or not (privado.eh_admin() or privado.eh_professor_da_atividade(v_atividade)) then
    raise exception 'Sem permissão para alterar esta entrega.' using errcode = '42501';
  end if;

  update public.entregas e
  set nota = null,
      comentario_professor = null,
      avaliado_em = null
  where e.id = p_entrega;
end;
$$;

-- =========================================================
-- PONTUAÇÃO E RANKING
-- =========================================================

-- Pontuação de um aluno: o próprio aluno, os professores dele e o admin
create function public.pontuacao(p_aluno uuid, p_ano integer default null)
returns integer
language plpgsql
stable
security definer
set search_path = ''
as $$
begin
  if not (p_aluno = privado.perfil_atual_id()
          or privado.eh_admin()
          or privado.eh_professor_do_aluno(p_aluno)) then
    raise exception 'Sem permissão para ver a pontuação deste aluno.' using errcode = '42501';
  end if;

  return privado.pontuacao_aluno(p_aluno, coalesce(p_ano, privado.ano_letivo_atual())::smallint);
end;
$$;

-- Professor: só turmas em que dá aula (decisão D2), com todos os alunos.
-- Aluno: só a própria turma, e só o pódio mais a própria linha
-- (RankingActivity.java:357). Expõe só nome, foto e pontos (decisão N6).
create function public.ranking_turma(p_turma uuid)
returns table (perfil_id uuid, nome text, foto_google_url text, foto_path text,
               pontos integer, posicao integer, eh_voce boolean)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_eu uuid := privado.perfil_atual_id();
  v_turma_inteira boolean;
begin
  v_turma_inteira := privado.eh_admin()
    or exists (
      select 1 from public.disciplinas d
      where d.turma_id = p_turma and d.professor_id = v_eu and d.ativo
    );

  if v_eu is null or not (v_turma_inteira
                          or (privado.eh_aluno() and privado.turma_atual_id() = p_turma)) then
    raise exception 'Sem permissão para ver o ranking desta turma.' using errcode = '42501';
  end if;

  return query
  select r.perfil_id, r.nome, r.foto_google_url, r.foto_path, r.pontos, r.posicao,
         r.perfil_id = v_eu
  from privado.ranking_completo(p_turma, privado.ano_letivo_atual()) r
  where v_turma_inteira or r.posicao <= 3 or r.perfil_id = v_eu
  order by r.posicao;
end;
$$;

-- Turmas que aparecem no seletor do ranking do professor (decisão D2)
create function public.turmas_do_ranking()
returns table (id uuid, nome text)
language sql
stable
security definer
set search_path = ''
as $$
  select distinct t.id, t.nome
  from public.turmas t
  where privado.eh_admin()
     or exists (
       select 1 from public.disciplinas d
       where d.turma_id = t.id and d.ativo and d.professor_id = privado.perfil_atual_id()
     )
     or (privado.eh_aluno() and t.id = privado.turma_atual_id())
  order by t.nome
$$;

-- Posição do aluno logado na própria turma (tela inicial e perfil)
create function public.minha_posicao_no_ranking()
returns table (posicao integer, pontos integer, total integer)
language sql
stable
security definer
set search_path = ''
as $$
  with r as (
    select * from privado.ranking_completo(privado.turma_atual_id(), privado.ano_letivo_atual())
  )
  select r.posicao, r.pontos, (select count(*) from r)::integer
  from r
  where r.perfil_id = privado.perfil_atual_id()
$$;

-- =========================================================
-- DISCIPLINA: PROFESSOR E COLEGAS (InfoDisciplinaDialog)
-- =========================================================

-- O e-mail dos alunos só aparece para o professor da disciplina e o admin
create function public.participantes_disciplina(p_disciplina uuid)
returns table (perfil_id uuid, nome text, tipo text, email text, foto_google_url text, foto_path text)
language plpgsql
stable
security definer
set search_path = ''
as $$
declare
  v_ve_emails boolean := privado.eh_admin() or privado.eh_professor_da_disciplina(p_disciplina);
begin
  if not (v_ve_emails or privado.aluno_matriculado(p_disciplina)) then
    raise exception 'Sem permissão para ver esta disciplina.' using errcode = '42501';
  end if;

  return query
  select p.id, p.nome, p.tipo, p.email::text, p.foto_google_url, p.foto_path
  from public.disciplinas d
  join public.perfis p on p.id = d.professor_id
  where d.id = p_disciplina
  union all
  select * from (
    select p.id, p.nome, p.tipo, case when v_ve_emails then p.email::text end,
           p.foto_google_url, p.foto_path
    from public.matriculas m
    join public.perfis p on p.id = m.aluno_id
    where m.disciplina_id = p_disciplina
      and p.ativo
    order by lower(p.nome)
  ) alunos;
end;
$$;

-- =========================================================
-- NOTIFICAÇÕES (NotificacaoWorker)
-- =========================================================

-- Aluno: novas disciplinas (pela matrícula), novas atividades e entregas avaliadas.
-- Professor: novas entregas nas suas disciplinas.
create function public.novidades_desde(p_desde timestamptz)
returns table (tipo text, item_id uuid, nome text, disciplina_id uuid, disciplina_nome text,
               atividade_id uuid, aluno_nome text, momento timestamptz)
language sql
stable
security definer
set search_path = ''
as $$
  with eu as (
    select p.id, p.tipo from public.perfis p where p.id = privado.perfil_atual_id()
  )
  select * from (
    select 'disciplina'::text, d.id, d.nome, d.id, d.nome, null::uuid, null::text, m.criado_em
    from eu
    join public.matriculas m on m.aluno_id = eu.id
    join public.disciplinas d on d.id = m.disciplina_id
    where eu.tipo = 'aluno' and d.ativo and m.criado_em > p_desde

    union all

    select 'atividade', a.id, a.nome, d.id, d.nome, a.id, null, a.criado_em
    from eu
    join public.matriculas m on m.aluno_id = eu.id
    join public.disciplinas d on d.id = m.disciplina_id
    join public.atividades a on a.disciplina_id = d.id
    where eu.tipo = 'aluno' and d.ativo and a.criado_em > p_desde

    union all

    select 'avaliacao', e.id, a.nome, d.id, d.nome, a.id, null, e.avaliado_em
    from eu
    join public.entregas e on e.aluno_id = eu.id
    join public.atividades a on a.id = e.atividade_id
    join public.disciplinas d on d.id = a.disciplina_id
    where eu.tipo = 'aluno' and e.avaliado_em > p_desde

    union all

    select 'entrega', e.id, a.nome, d.id, d.nome, a.id, al.nome, e.enviado_em
    from eu
    join public.disciplinas d on d.professor_id = eu.id
    join public.atividades a on a.disciplina_id = d.id
    join public.entregas e on e.atividade_id = a.id
    join public.perfis al on al.id = e.aluno_id
    where eu.tipo = 'professor' and e.enviado_em > p_desde
  ) novidades
  order by 8
  limit 100
$$;

-- =========================================================
-- USUÁRIOS (admin) E PEDIDOS DE EXCLUSÃO (decisões D7, D12, D13)
-- =========================================================

create function public.transferir_disciplinas(p_de uuid, p_para uuid)
returns integer
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_total integer;
begin
  perform privado.exigir_admin();

  if p_de = p_para or not exists (
    select 1 from public.perfis p where p.id = p_para and p.tipo = 'professor' and p.ativo
  ) then
    raise exception 'Escolha outro professor ativo para receber as disciplinas.' using errcode = 'P0001';
  end if;

  update public.disciplinas d set professor_id = p_para where d.professor_id = p_de;

  get diagnostics v_total = row_count;
  return v_total;
end;
$$;

create function public.excluir_usuario(p_perfil uuid)
returns void
language plpgsql
security definer
set search_path = ''
as $$
begin
  perform privado.exigir_admin();
  perform privado.excluir_perfil(p_perfil, null);
end;
$$;

create function public.solicitar_exclusao(p_motivo text default null)
returns uuid
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_eu uuid := privado.perfil_atual_id();
  v_id uuid;
begin
  if v_eu is null then
    raise exception 'Entre no aplicativo para pedir a exclusão.' using errcode = '42501';
  end if;

  insert into public.solicitacoes_exclusao (perfil_id, motivo)
  values (v_eu, nullif(btrim(p_motivo), ''))
  returning id into v_id;

  return v_id;
exception
  when unique_violation then
    raise exception 'Você já tem um pedido de exclusão em análise.' using errcode = 'P0001';
end;
$$;

create function public.decidir_exclusao(p_solicitacao uuid, p_aprovar boolean)
returns void
language plpgsql
security definer
set search_path = ''
as $$
declare
  v_perfil uuid;
begin
  perform privado.exigir_admin();

  select s.perfil_id into v_perfil
  from public.solicitacoes_exclusao s
  where s.id = p_solicitacao and s.status = 'pendente'
  for update;

  if v_perfil is null then
    raise exception 'Pedido não encontrado ou já decidido.' using errcode = 'P0002';
  end if;

  if p_aprovar then
    perform privado.excluir_perfil(v_perfil, p_solicitacao);
  else
    update public.solicitacoes_exclusao s
    set status = 'recusada',
        decidido_por = privado.perfil_atual_id(),
        decidido_em = now()
    where s.id = p_solicitacao;
  end if;
end;
$$;

-- =========================================================
-- QUEM PODE CHAMAR
-- =========================================================

revoke all on function
  public.vincular_conta(),
  public.definir_ano_letivo(integer),
  public.definir_bimestres(integer, jsonb),
  public.registrar_frequencia(uuid, jsonb),
  public.avaliar_entrega(uuid, numeric, text),
  public.refazer_avaliacao(uuid),
  public.pontuacao(uuid, integer),
  public.ranking_turma(uuid),
  public.turmas_do_ranking(),
  public.minha_posicao_no_ranking(),
  public.participantes_disciplina(uuid),
  public.novidades_desde(timestamptz),
  public.transferir_disciplinas(uuid, uuid),
  public.excluir_usuario(uuid),
  public.solicitar_exclusao(text),
  public.decidir_exclusao(uuid, boolean)
from public, anon;

grant execute on function
  public.vincular_conta(),
  public.definir_ano_letivo(integer),
  public.definir_bimestres(integer, jsonb),
  public.registrar_frequencia(uuid, jsonb),
  public.avaliar_entrega(uuid, numeric, text),
  public.refazer_avaliacao(uuid),
  public.pontuacao(uuid, integer),
  public.ranking_turma(uuid),
  public.turmas_do_ranking(),
  public.minha_posicao_no_ranking(),
  public.participantes_disciplina(uuid),
  public.novidades_desde(timestamptz),
  public.transferir_disciplinas(uuid, uuid),
  public.excluir_usuario(uuid),
  public.solicitar_exclusao(text),
  public.decidir_exclusao(uuid, boolean)
to authenticated;
