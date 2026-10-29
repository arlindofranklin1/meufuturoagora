-- vincular_conta(): liga a conta Google ao cadastro do admin pelo e-mail
-- verificado (substitui TelaEntrar.verificarUsuario).
begin;
select * from no_plan();
select testes.cenario();

-- Cadastros feitos pelo admin, ainda sem login
insert into public.perfis (id, email, nome, tipo, turma_id) values
  (testes.guardar('novo', gen_random_uuid()), 'Nova.Aluna@Escola.test', 'Nova Aluna', 'aluno', testes.id('turma_a')),
  (testes.guardar('sem_turma', gen_random_uuid()), 'semturma@escola.test', 'Sem Turma', 'aluno', null),
  (testes.guardar('inativo', gen_random_uuid()), 'inativo@escola.test', 'Inativo', 'aluno', testes.id('turma_a')),
  (testes.guardar('prof_novo', gen_random_uuid()), 'prof.novo@escola.test', 'Prof Novo', 'professor', null);
update public.perfis set ativo = false where id = testes.id('inativo');

select testes.criar_usuario('novo', 'nova.aluna@escola.test',
  '{"avatar_url": "https://lh3.googleusercontent.com/a/foto123"}');
select testes.criar_usuario('estranho', 'ninguem@fora.test');
select testes.criar_usuario('sem_turma', 'semturma@escola.test');
select testes.criar_usuario('inativo', 'inativo@escola.test');
select testes.criar_usuario('nao_confirmado', 'prof.novo@escola.test', '{}', false);
select testes.criar_usuario('foto_ruim', 'prof.novo@escola.test.br', '{"avatar_url": "https://exemplo.test/rastreador.png"}');

-- Caminho feliz, com e-mail em maiúsculas diferentes do cadastro
select testes.entrar_como('novo');
select is((select (public.vincular_conta()).id), testes.id('novo'),
  'liga a conta ao cadastro com o mesmo e-mail, sem diferenciar maiúsculas');
select is((select usuario_id from public.perfis where id = testes.id('novo')), testes.id('u_novo'),
  'grava o usuario_id no perfil');
select is((select foto_google_url from public.perfis where id = testes.id('novo')),
  'https://lh3.googleusercontent.com/a/foto123', 'guarda a foto do Google');
select is((select nome from public.perfis where id = testes.id('novo')), 'Nova Aluna',
  'mantém o nome cadastrado pelo admin');
select is((select (public.vincular_conta()).id), testes.id('novo'), 'chamar de novo é seguro');

-- Recusas
select testes.entrar_como('estranho');
select throws_ok('select public.vincular_conta()', '42501', null, 'e-mail não cadastrado é recusado');

select testes.entrar_como('sem_turma');
select throws_ok('select public.vincular_conta()', '42501', null, 'aluno sem turma é recusado');

select testes.entrar_como('inativo');
select throws_ok('select public.vincular_conta()', '42501', null, 'cadastro inativo é recusado');

select testes.entrar_como('nao_confirmado');
select throws_ok('select public.vincular_conta()', '42501', null, 'e-mail não verificado é recusado');
set local role postgres;
select is((select usuario_id from public.perfis where id = testes.id('prof_novo')), null,
  'conta com e-mail não verificado não fica ligada ao cadastro');

-- Não vincula a um e-mail diferente do e-mail do token
select is((select count(*)::int from public.perfis where usuario_id = testes.id('u_estranho')), 0,
  'conta estranha não fica ligada a nenhum perfil');

-- Foto que não é do Google é descartada (user_metadata é editável pelo usuário)
update public.perfis set email = 'prof.novo@escola.test.br' where id = testes.id('prof_novo');
select testes.entrar_como('foto_ruim');
select is((select (public.vincular_conta()).foto_google_url), null, 'foto fora do Google é descartada');

-- Conta Google recriada: o perfil, com tudo o que é dele, passa para a conta nova
set local role postgres;
delete from auth.users where id = testes.id('u_a1');
select is((select usuario_id from public.perfis where id = testes.id('a1')), null,
  'apagar a conta antiga deixa o perfil sem conta, mas com os dados');
select testes.criar_usuario('a1_nova', 'a1@escola.test');
select testes.entrar_como('a1_nova');
select is((select (public.vincular_conta()).id), testes.id('a1'), 'conta recriada recupera o mesmo perfil');
set local role postgres;
select is((select usuario_id from public.perfis where id = testes.id('a1')), testes.id('u_a1_nova'),
  'o perfil passa a apontar para a conta nova');
select is((select count(*)::int from public.matriculas where aluno_id = testes.id('a1')), 2,
  'as matrículas continuam com o perfil');

-- Admin trocou o e-mail de um perfil: a conta antiga deixa de valer para ele
update public.perfis set email = 'a2.novo@escola.test' where id = testes.id('a2');
select is((select usuario_id from public.perfis where id = testes.id('a2')), null,
  'trocar o e-mail desliga a conta antiga do perfil');
update public.perfis set email = 'A1@ESCOLA.TEST' where id = testes.id('a1');
select is((select usuario_id from public.perfis where id = testes.id('a1')), testes.id('u_a1_nova'),
  'mudar só maiúsculas do e-mail não desliga a conta');
select testes.entrar_como('a2');
select is((select count(*)::int from public.perfis), 0, 'a conta antiga não vê mais o perfil');
select throws_ok('select public.vincular_conta()', '42501', null,
  'depois de trocar o e-mail no cadastro, a conta do e-mail antigo é recusada');

select * from finish();
rollback;
