-- Dados FICTÍCIOS para desenvolvimento local (supabase db reset).
-- Nenhum dado real: e-mails no domínio reservado .test, que não existe.
--
-- O admin real não fica aqui (o repositório é público): ele vem de
-- seed_admin.local.sql, gerado a partir de admins.local.txt e fora do Git.
-- Os cadastros abaixo não têm conta de login; ganham uma no primeiro login.

insert into public.anos_letivos (ano, atual) values (2026, true);

insert into public.bimestres (ano, numero, inicio, fim) values
  (2026, 1, '2026-02-02', '2026-04-30'),
  (2026, 2, '2026-05-04', '2026-07-10'),
  (2026, 3, '2026-08-03', '2026-09-30'),
  (2026, 4, '2026-10-01', '2026-12-18');

insert into public.turmas (id, nome, serie) values
  ('00000000-0000-4000-8000-000000000a01', '1º ano Informática', 1),
  ('00000000-0000-4000-8000-000000000a02', '2º ano Informática', 2);

insert into public.perfis (id, email, nome, tipo, admin, turma_id) values
  ('00000000-0000-4000-8000-000000000001', 'coordenacao@exemplo.test', 'Coordenação (fictícia)', 'professor', true, null),
  ('00000000-0000-4000-8000-000000000002', 'professora.marta@exemplo.test', 'Marta Fictícia', 'professor', false, null),
  ('00000000-0000-4000-8000-000000000003', 'professor.joao@exemplo.test', 'João Fictício', 'professor', false, null),
  ('00000000-0000-4000-8000-000000000011', 'aluna.lia@exemplo.test', 'Lia Fictícia', 'aluno', false, '00000000-0000-4000-8000-000000000a01'),
  ('00000000-0000-4000-8000-000000000012', 'aluno.rui@exemplo.test', 'Rui Fictício', 'aluno', false, '00000000-0000-4000-8000-000000000a01'),
  ('00000000-0000-4000-8000-000000000013', 'aluna.ines@exemplo.test', 'Inês Fictícia', 'aluno', false, '00000000-0000-4000-8000-000000000a02');

insert into public.disciplinas (id, nome, turma_id, professor_id) values
  ('00000000-0000-4000-8000-000000000d01', 'Lógica de Programação', '00000000-0000-4000-8000-000000000a01', '00000000-0000-4000-8000-000000000002'),
  ('00000000-0000-4000-8000-000000000d02', 'Matemática', '00000000-0000-4000-8000-000000000a01', '00000000-0000-4000-8000-000000000003'),
  ('00000000-0000-4000-8000-000000000d03', 'Banco de Dados', '00000000-0000-4000-8000-000000000a02', '00000000-0000-4000-8000-000000000002');

insert into public.atividades (disciplina_id, nome, descricao, pontos, prazo) values
  ('00000000-0000-4000-8000-000000000d01', 'Algoritmos do dia a dia', 'Descreva um algoritmo para fazer café.', 5, '2026-11-20'),
  ('00000000-0000-4000-8000-000000000d02', 'Frações', 'Resolva os exercícios 1 a 10.', 10, '2026-11-27');

insert into public.aulas (disciplina_id, data, quantidade, descricao) values
  ('00000000-0000-4000-8000-000000000d01', '2026-10-26', 2, 'Introdução a algoritmos'),
  ('00000000-0000-4000-8000-000000000d02', '2026-10-27', 1, 'Frações equivalentes');
