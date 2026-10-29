-- Arquivos: substituem o Cloudinary, cujas URLs eram públicas.
--
-- Os três buckets são privados: o app lê com URL assinada de curta duração.
-- O limite de 3 MB é o mesmo de ArquivoUtil.LIMITE_TAMANHO_BYTES, e só PDF e
-- imagens são aceitos (decisão D5). A foto de perfil já sai do app em JPEG
-- (PerfilFotoUtil.java:189).
--
-- Caminhos (a primeira pasta é que as políticas conferem):
--   atividades/<disciplina_id>/<atividade_id>/<uuid>.<ext>
--   entregas/<atividade_id>/<aluno_id>/<uuid>.<ext>
--   fotos-perfil/<perfil_id>/<uuid>.jpg
-- Os ids são de public.perfis, não de auth.users. O nome original do arquivo
-- fica só no banco (arquivo_nome), nunca no caminho.
--
-- Apagar objetos por SQL é bloqueado pelo Storage (storage.protect_delete), por
-- isso as exclusões de arquivos são feitas pelo app, pela API do Storage.

insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values
  ('atividades', 'atividades', false, 3145728,
   array['application/pdf', 'image/jpeg', 'image/png', 'image/webp']),
  ('entregas', 'entregas', false, 3145728,
   array['application/pdf', 'image/jpeg', 'image/png', 'image/webp']),
  ('fotos-perfil', 'fotos-perfil', false, 3145728,
   array['image/jpeg']);

-- =========================================================
-- FOTOS DE PERFIL (decisão N6: todos os autenticados veem; só o dono grava)
-- =========================================================

create policy fotos_select on storage.objects for select to authenticated
  using (
    bucket_id = 'fotos-perfil'
    and (select privado.perfil_atual_id()) is not null
  );

create policy fotos_insert on storage.objects for insert to authenticated
  with check (
    bucket_id = 'fotos-perfil'
    and (storage.foldername(name))[1] = (select privado.perfil_atual_id())::text
  );

create policy fotos_update on storage.objects for update to authenticated
  using (
    bucket_id = 'fotos-perfil'
    and (storage.foldername(name))[1] = (select privado.perfil_atual_id())::text
  )
  with check (
    bucket_id = 'fotos-perfil'
    and (storage.foldername(name))[1] = (select privado.perfil_atual_id())::text
  );

-- O admin apaga ao excluir o usuário (decisão D7)
create policy fotos_delete on storage.objects for delete to authenticated
  using (
    bucket_id = 'fotos-perfil'
    and ((storage.foldername(name))[1] = (select privado.perfil_atual_id())::text
         or (select privado.eh_admin()))
  );

-- =========================================================
-- ANEXOS DAS ATIVIDADES (professor grava; professor e alunos matriculados leem)
-- =========================================================

create policy atividades_arquivos_select on storage.objects for select to authenticated
  using (
    bucket_id = 'atividades'
    and ((select privado.eh_admin())
         or privado.eh_professor_da_disciplina(privado.uuid_ou_nulo((storage.foldername(name))[1]))
         or privado.aluno_matriculado(privado.uuid_ou_nulo((storage.foldername(name))[1])))
  );

create policy atividades_arquivos_insert on storage.objects for insert to authenticated
  with check (
    bucket_id = 'atividades'
    and ((select privado.eh_admin())
         or privado.eh_professor_da_disciplina(privado.uuid_ou_nulo((storage.foldername(name))[1])))
  );

create policy atividades_arquivos_update on storage.objects for update to authenticated
  using (
    bucket_id = 'atividades'
    and ((select privado.eh_admin())
         or privado.eh_professor_da_disciplina(privado.uuid_ou_nulo((storage.foldername(name))[1])))
  )
  with check (
    bucket_id = 'atividades'
    and ((select privado.eh_admin())
         or privado.eh_professor_da_disciplina(privado.uuid_ou_nulo((storage.foldername(name))[1])))
  );

create policy atividades_arquivos_delete on storage.objects for delete to authenticated
  using (
    bucket_id = 'atividades'
    and ((select privado.eh_admin())
         or privado.eh_professor_da_disciplina(privado.uuid_ou_nulo((storage.foldername(name))[1])))
  );

-- =========================================================
-- ARQUIVOS DAS ENTREGAS (só o próprio aluno grava, uma vez; leem ele e o professor)
-- =========================================================

create policy entregas_arquivos_select on storage.objects for select to authenticated
  using (
    bucket_id = 'entregas'
    and ((select privado.eh_admin())
         or (storage.foldername(name))[2] = (select privado.perfil_atual_id())::text
         or privado.eh_professor_da_atividade(privado.uuid_ou_nulo((storage.foldername(name))[1])))
  );

-- Depois de entregue não dá para trocar o arquivo (TelaAtividadeAluno.java:252)
create policy entregas_arquivos_insert on storage.objects for insert to authenticated
  with check (
    bucket_id = 'entregas'
    and (storage.foldername(name))[2] = (select privado.perfil_atual_id())::text
    and (select privado.eh_aluno())
    and privado.aluno_matriculado_na_atividade(privado.uuid_ou_nulo((storage.foldername(name))[1]))
    and not exists (
      select 1 from public.entregas e
      where e.atividade_id = privado.uuid_ou_nulo((storage.foldername(name))[1])
        and e.aluno_id = (select privado.perfil_atual_id())
    )
  );

-- Sem update: o arquivo de uma entrega não é substituído

-- O professor apaga ao excluir a trilha (D6); o admin, ao excluir o aluno (D7)
create policy entregas_arquivos_delete on storage.objects for delete to authenticated
  using (
    bucket_id = 'entregas'
    and ((select privado.eh_admin())
         or privado.eh_professor_da_atividade(privado.uuid_ou_nulo((storage.foldername(name))[1])))
  );
