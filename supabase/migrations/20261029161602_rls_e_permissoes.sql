-- Grants e políticas RLS, conforme a matriz de permissões aprovada
-- (docs/migracao/inventario.md, Seção 5).
--
-- Duas camadas:
-- 1. GRANT por coluna: o que a API deixa gravar (ex.: ninguém grava nota,
--    ano_letivo ou dias_atraso direto; isso é do servidor).
-- 2. RLS: quais linhas cada perfil vê e altera.
--
-- anon não recebe nada. Escritas com regra de negócio (frequência, avaliação,
-- ano letivo, exclusão) são feitas só pelas funções de 20261029161605_funcoes_rpc.

-- =========================================================
-- AUXILIARES DESTA MIGRAÇÃO
-- =========================================================

create function privado.turma_atual_id()
returns uuid
language sql
stable
security definer
set search_path = ''
as $$
  select p.turma_id
  from public.perfis p
  where p.usuario_id = (select auth.uid())
    and p.ativo
$$;

-- Professor logado dá aula numa disciplina em que o aluno está matriculado
create function privado.eh_professor_do_aluno(p_aluno uuid)
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
    where m.aluno_id = p_aluno
      and d.professor_id = (select privado.perfil_atual_id())
  )
$$;

grant execute on function privado.turma_atual_id(), privado.eh_professor_do_aluno(uuid)
to authenticated;

-- O dono do registro é o perfil logado, sem o app precisar mandar o id
alter table public.disciplinas alter column professor_id set default privado.perfil_atual_id();
alter table public.entregas alter column aluno_id set default privado.perfil_atual_id();
alter table public.diario_entradas alter column aluno_id set default privado.perfil_atual_id();

-- =========================================================
-- TURMAS
-- =========================================================

grant select, delete on public.turmas to authenticated;
grant insert (nome, serie, ativo), update (nome, serie, ativo) on public.turmas to authenticated;

-- Professor vê todas (para criar disciplina); aluno, só a própria
create policy turmas_select on public.turmas for select to authenticated
  using (
    (select privado.eh_admin())
    or (select privado.eh_professor())
    or id = (select privado.turma_atual_id())
  );

create policy turmas_insert on public.turmas for insert to authenticated
  with check ((select privado.eh_admin()));

create policy turmas_update on public.turmas for update to authenticated
  using ((select privado.eh_admin()))
  with check ((select privado.eh_admin()));

create policy turmas_delete on public.turmas for delete to authenticated
  using ((select privado.eh_admin()));

-- =========================================================
-- PERFIS
-- =========================================================

grant select on public.perfis to authenticated;
grant insert (email, nome, tipo, admin, ativo, turma_id) on public.perfis to authenticated;
-- Quem não é admin só troca foto_path (gatilho proteger_perfil)
grant update (email, nome, tipo, admin, ativo, turma_id, foto_path) on public.perfis to authenticated;

create policy perfis_select on public.perfis for select to authenticated
  using (
    id = (select privado.perfil_atual_id())
    or (select privado.eh_admin())
    or privado.eh_professor_do_aluno(id)
  );

create policy perfis_insert on public.perfis for insert to authenticated
  with check ((select privado.eh_admin()));

create policy perfis_update on public.perfis for update to authenticated
  using (id = (select privado.perfil_atual_id()) or (select privado.eh_admin()))
  with check (id = (select privado.perfil_atual_id()) or (select privado.eh_admin()));

-- Sem delete pela API: exclusão só por excluir_usuario / decidir_exclusao

-- =========================================================
-- ANO LETIVO E BIMESTRES (escrita só por RPC)
-- =========================================================

grant select on public.anos_letivos, public.bimestres to authenticated;

create policy anos_letivos_select on public.anos_letivos for select to authenticated
  using ((select privado.perfil_atual_id()) is not null);

create policy bimestres_select on public.bimestres for select to authenticated
  using ((select privado.perfil_atual_id()) is not null);

-- =========================================================
-- DISCIPLINAS
-- =========================================================

grant select on public.disciplinas to authenticated;
grant insert (nome, turma_id, professor_id) on public.disciplinas to authenticated;
grant update (nome, ativo, turma_id, professor_id) on public.disciplinas to authenticated;

create policy disciplinas_select on public.disciplinas for select to authenticated
  using (
    (select privado.eh_admin())
    or professor_id = (select privado.perfil_atual_id())
    or privado.aluno_matriculado(id)
  );

create policy disciplinas_insert on public.disciplinas for insert to authenticated
  with check (
    (select privado.eh_admin())
    or ((select privado.eh_professor()) and professor_id = (select privado.perfil_atual_id()))
  );

create policy disciplinas_update on public.disciplinas for update to authenticated
  using ((select privado.eh_admin()) or professor_id = (select privado.perfil_atual_id()))
  with check ((select privado.eh_admin()) or professor_id = (select privado.perfil_atual_id()));

-- Sem delete: o professor desativa (ativo = false)

-- =========================================================
-- MATRÍCULAS (escrita só pelos gatilhos)
-- =========================================================

grant select on public.matriculas to authenticated;

create policy matriculas_select on public.matriculas for select to authenticated
  using (
    (select privado.eh_admin())
    or aluno_id = (select privado.perfil_atual_id())
    or privado.eh_professor_da_disciplina(disciplina_id)
  );

-- =========================================================
-- ATIVIDADES
-- =========================================================

grant select, delete on public.atividades to authenticated;
grant insert (id, disciplina_id, nome, descricao, pontos, prazo, link, arquivo_path, arquivo_nome)
  on public.atividades to authenticated;
grant update (nome, descricao, pontos, prazo, link, arquivo_path, arquivo_nome)
  on public.atividades to authenticated;

create policy atividades_select on public.atividades for select to authenticated
  using (
    (select privado.eh_admin())
    or privado.eh_professor_da_disciplina(disciplina_id)
    or privado.aluno_matriculado(disciplina_id)
  );

create policy atividades_insert on public.atividades for insert to authenticated
  with check ((select privado.eh_admin()) or privado.eh_professor_da_disciplina(disciplina_id));

create policy atividades_update on public.atividades for update to authenticated
  using ((select privado.eh_admin()) or privado.eh_professor_da_disciplina(disciplina_id))
  with check ((select privado.eh_admin()) or privado.eh_professor_da_disciplina(disciplina_id));

-- Decisão D6: apaga a trilha e as entregas (cascata). O app remove os arquivos
-- do Storage antes, porque o Storage não deixa apagar objetos por SQL.
create policy atividades_delete on public.atividades for delete to authenticated
  using ((select privado.eh_admin()) or privado.eh_professor_da_disciplina(disciplina_id));

-- =========================================================
-- ENTREGAS (avaliação só por RPC; o aluno não altera depois de enviar)
-- =========================================================

grant select on public.entregas to authenticated;
grant insert (atividade_id, comentario_aluno, arquivo_path, arquivo_nome)
  on public.entregas to authenticated;

create policy entregas_select on public.entregas for select to authenticated
  using (
    (select privado.eh_admin())
    or aluno_id = (select privado.perfil_atual_id())
    or privado.eh_professor_da_atividade(atividade_id)
  );

create policy entregas_insert on public.entregas for insert to authenticated
  with check (
    aluno_id = (select privado.perfil_atual_id())
    and (select privado.eh_aluno())
    and privado.aluno_matriculado_na_atividade(atividade_id)
  );

-- =========================================================
-- AULAS
-- =========================================================

grant select on public.aulas to authenticated;
grant insert (disciplina_id, data, quantidade, descricao) on public.aulas to authenticated;
grant update (data, quantidade, descricao) on public.aulas to authenticated;

create policy aulas_select on public.aulas for select to authenticated
  using (
    (select privado.eh_admin())
    or privado.eh_professor_da_disciplina(disciplina_id)
    or privado.aluno_matriculado(disciplina_id)
  );

create policy aulas_insert on public.aulas for insert to authenticated
  with check ((select privado.eh_admin()) or privado.eh_professor_da_disciplina(disciplina_id));

create policy aulas_update on public.aulas for update to authenticated
  using ((select privado.eh_admin()) or privado.eh_professor_da_disciplina(disciplina_id))
  with check ((select privado.eh_admin()) or privado.eh_professor_da_disciplina(disciplina_id));

-- =========================================================
-- FREQUÊNCIAS (escrita só por registrar_frequencia)
-- =========================================================

grant select on public.frequencias to authenticated;

create policy frequencias_select on public.frequencias for select to authenticated
  using (
    (select privado.eh_admin())
    or aluno_id = (select privado.perfil_atual_id())
    or privado.eh_professor_da_aula(aula_id)
  );

-- =========================================================
-- DIÁRIO (decisão D8: só o próprio aluno, nem o admin)
-- =========================================================

grant select, delete on public.diario_entradas to authenticated;
grant insert (texto), update (texto) on public.diario_entradas to authenticated;

create policy diario_select on public.diario_entradas for select to authenticated
  using (aluno_id = (select privado.perfil_atual_id()));

create policy diario_insert on public.diario_entradas for insert to authenticated
  with check (aluno_id = (select privado.perfil_atual_id()) and (select privado.eh_aluno()));

create policy diario_update on public.diario_entradas for update to authenticated
  using (aluno_id = (select privado.perfil_atual_id()))
  with check (aluno_id = (select privado.perfil_atual_id()));

create policy diario_delete on public.diario_entradas for delete to authenticated
  using (aluno_id = (select privado.perfil_atual_id()));

-- =========================================================
-- PEDIDOS DE EXCLUSÃO (escrita só por RPC)
-- =========================================================

grant select on public.solicitacoes_exclusao, public.exclusoes_realizadas to authenticated;

create policy solicitacoes_select on public.solicitacoes_exclusao for select to authenticated
  using ((select privado.eh_admin()) or perfil_id = (select privado.perfil_atual_id()));

create policy exclusoes_select on public.exclusoes_realizadas for select to authenticated
  using ((select privado.eh_admin()));
