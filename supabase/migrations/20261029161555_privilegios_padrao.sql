-- Privilégios padrão: nada novo fica acessível pela API sem um grant explícito.
--
-- Em projetos Supabase existentes, toda tabela, sequência e função criada em
-- "public" recebe privilégios para anon, authenticated e service_role, e toda
-- função recebe EXECUTE para PUBLIC. Não dá para confiar só na opção
-- "Automatically expose new tables" do painel, que pode mudar; por isso a
-- revogação é feita aqui, antes de qualquer tabela.
-- Referência: https://supabase.com/docs/guides/api/securing-your-api#revoke-default-privileges
--
-- As migrações rodam como "postgres", dono de todos os objetos do app. Os
-- privilégios padrão de "supabase_admin" valem só para objetos criados por ele
-- (extensões e esquemas internos do Supabase) e não são alterados.

alter default privileges for role postgres in schema public
  revoke all on tables from anon, authenticated, service_role;

alter default privileges for role postgres in schema public
  revoke all on sequences from anon, authenticated, service_role;

alter default privileges for role postgres in schema public
  revoke all on functions from anon, authenticated, service_role;

-- Global: também vale para as funções do esquema "privado". Cada função que as
-- políticas usam recebe grant explícito.
alter default privileges for role postgres
  revoke execute on functions from public;

-- Funções internas (usadas por políticas, gatilhos e RPCs) ficam num esquema
-- que a Data API não expõe (ver [api].schemas em config.toml).
create schema privado;

revoke all on schema privado from public;
grant usage on schema privado to authenticated;

comment on schema privado is
  'Funções internas do app. Não exposto pela Data API; as políticas RLS e as RPCs chamam estas funções.';
