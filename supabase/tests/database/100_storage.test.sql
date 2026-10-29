-- Storage: buckets privados e políticas por caminho (D5, D6, D7, N6).
--
-- A API do Storage grava em storage.objects com o papel do usuário logado,
-- então inserir ali como "authenticated" testa as mesmas políticas. Apagar por
-- SQL é bloqueado pelo Storage; para testar as políticas de delete, o teste
-- liga storage.allow_delete_query só nesta transação.
begin;
select * from no_plan();
select testes.cenario();

-- ===== Buckets =====

select results_eq(
  'select id, public, file_size_limit, allowed_mime_types from storage.buckets order by id',
  $$values
    ('atividades'::text, false, 3145728::bigint, array['application/pdf', 'image/jpeg', 'image/png', 'image/webp']),
    ('entregas', false, 3145728, array['application/pdf', 'image/jpeg', 'image/png', 'image/webp']),
    ('fotos-perfil', false, 3145728, array['image/jpeg'])$$,
  'três buckets privados, 3 MB, só PDF e imagens (D5)');

-- ===== Entregas =====


select testes.entrar_como('a1');
select lives_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'entregas',
         concat_ws('/', testes.id('t1')::text, testes.id('a1')::text, 'resposta.pdf')),
  'aluno envia o arquivo da própria entrega');
select throws_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'entregas',
         concat_ws('/', testes.id('t1')::text, testes.id('a2')::text, 'falsa.pdf')),
  '42501', null, 'aluno não envia na pasta de outro aluno');

select testes.entrar_como('a3');
select throws_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'entregas',
         concat_ws('/', testes.id('t1')::text, testes.id('a3')::text, 'intrusa.pdf')),
  '42501', null, 'aluno não matriculado não envia');

select testes.entrar_como('p1');
select throws_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'entregas',
         concat_ws('/', testes.id('t1')::text, testes.id('p1')::text, 'prof.pdf')),
  '42501', null, 'professor não envia arquivo de entrega');

-- Depois de entregue, não envia outro arquivo
set local role postgres;
insert into public.entregas (atividade_id, aluno_id, comentario_aluno, arquivo_path, arquivo_nome)
values (testes.id('t1'), testes.id('a1'), '', concat_ws('/', testes.id('t1')::text, testes.id('a1')::text, 'resposta.pdf'), 'resposta.pdf');
select testes.entrar_como('a1');
select throws_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'entregas',
         concat_ws('/', testes.id('t1')::text, testes.id('a1')::text, 'troca.pdf')),
  '42501', null, 'entrega feita: o aluno não troca o arquivo');

-- Quem lê
select is((select count(*)::int from storage.objects where bucket_id = 'entregas'), 1, 'o aluno lê o próprio arquivo');
select testes.entrar_como('a2');
select is((select count(*)::int from storage.objects where bucket_id = 'entregas'), 0, 'outro aluno não lê');
select testes.entrar_como('p1');
select is((select count(*)::int from storage.objects where bucket_id = 'entregas'), 1, 'o professor da trilha lê');
select testes.entrar_como('p2');
select is((select count(*)::int from storage.objects where bucket_id = 'entregas'), 0, 'outro professor não lê');
select testes.entrar_como('adm');
select is((select count(*)::int from storage.objects where bucket_id = 'entregas'), 1, 'o admin lê');

-- ===== Anexos das trilhas =====

select testes.entrar_como('p1');
select lives_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'atividades',
         concat_ws('/', testes.id('d1')::text, testes.id('t1')::text, 'enunciado.pdf')),
  'professor anexa arquivo na própria disciplina');
select testes.entrar_como('p2');
select throws_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'atividades',
         concat_ws('/', testes.id('d1')::text, testes.id('t1')::text, 'invasao.pdf')),
  '42501', null, 'outro professor não anexa');
select is((select count(*)::int from storage.objects where bucket_id = 'atividades'), 0, 'outro professor não lê o anexo');
select testes.entrar_como('a1');
select is((select count(*)::int from storage.objects where bucket_id = 'atividades'), 1, 'aluno matriculado lê o anexo');
select throws_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'atividades',
         concat_ws('/', testes.id('d1')::text, testes.id('t1')::text, 'aluno.pdf')),
  '42501', null, 'aluno não anexa em trilha');
select testes.entrar_como('a3');
select is((select count(*)::int from storage.objects where bucket_id = 'atividades'), 0, 'aluno de outra turma não lê');

select throws_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'atividades', 'nao-e-uuid/x/y.pdf'),
  '42501', null, 'pasta que não é um id é recusada, sem erro de conversão');

-- ===== Fotos de perfil (N6) =====

select testes.entrar_como('a1');
select lives_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'fotos-perfil', concat_ws('/', testes.id('a1')::text, 'foto.jpg')),
  'aluno envia a própria foto');
select throws_ok(
  format('insert into storage.objects (bucket_id, name) values (%L, %L)', 'fotos-perfil', concat_ws('/', testes.id('a2')::text, 'foto.jpg')),
  '42501', null, 'aluno não envia foto de outro');
select testes.entrar_como('a3');
select is((select count(*)::int from storage.objects where bucket_id = 'fotos-perfil'), 1,
  'N6: qualquer usuário autenticado vê as fotos');
select testes.anonimo();
select is((select count(*)::int from storage.objects), 0, 'anon não vê nenhum arquivo');

-- ===== Exclusão de arquivos (D6, D7) =====

set local role postgres;
set local storage.allow_delete_query = 'true';

select testes.entrar_como('p2');
delete from storage.objects where bucket_id = 'entregas';
select testes.entrar_como('a1');
delete from storage.objects where bucket_id = 'entregas';
set local role postgres;
select is((select count(*)::int from storage.objects where bucket_id = 'entregas'), 1,
  'nem outro professor nem o próprio aluno apagam o arquivo da entrega');

select testes.entrar_como('p1');
delete from storage.objects where bucket_id = 'entregas';
set local role postgres;
select is((select count(*)::int from storage.objects where bucket_id = 'entregas'), 0,
  'D6: o professor da trilha apaga os arquivos das entregas');

select testes.entrar_como('a2');
delete from storage.objects where bucket_id = 'fotos-perfil';
set local role postgres;
select is((select count(*)::int from storage.objects where bucket_id = 'fotos-perfil'), 1,
  'outro aluno não apaga a foto');

select testes.entrar_como('adm');
delete from storage.objects where bucket_id = 'fotos-perfil';
set local role postgres;
select is((select count(*)::int from storage.objects where bucket_id = 'fotos-perfil'), 0,
  'D7: o admin apaga a foto ao excluir o usuário');

select * from finish();
rollback;
