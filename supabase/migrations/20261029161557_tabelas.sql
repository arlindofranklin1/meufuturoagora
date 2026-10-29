-- Tabelas do app. O desenho está em docs/migracao/inventario.md (Seção 4),
-- aprovado no Portão 1. RLS é habilitado aqui, na criação: tabela sem política
-- não é acessível por ninguém pela API.

create extension if not exists citext with schema extensions;

-- =========================================================
-- TURMAS
-- =========================================================

create table public.turmas (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null check (btrim(nome) <> ''),
  serie         smallint check (serie between 1 and 20),
  ativo         boolean not null default true,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);

-- "6º ano A" e "6º Ano A " são a mesma turma (CadastrarUsuarioActivity.java:171)
create unique index turmas_nome_unico on public.turmas (lower(btrim(nome)));

alter table public.turmas enable row level security;

-- =========================================================
-- PERFIS (alunos e professores)
-- =========================================================

create table public.perfis (
  id              uuid primary key default gen_random_uuid(),
  -- Nulo até o primeiro login; preenchido por vincular_conta()
  usuario_id      uuid unique references auth.users (id) on delete set null,
  email           extensions.citext not null unique check (email ~ '^[^@\s]+@[^@\s]+\.[^@\s]+$'),
  nome            text not null check (btrim(nome) <> ''),
  tipo            text not null check (tipo in ('aluno', 'professor')),
  admin           boolean not null default false,
  ativo           boolean not null default true,
  turma_id        uuid references public.turmas (id) on delete set null,
  -- Foto da conta Google (só URLs do Google, ver vincular_conta)
  foto_google_url text,
  -- Foto escolhida no app, no bucket fotos-perfil, sempre dentro da pasta do perfil
  foto_path       text,
  criado_em       timestamptz not null default now(),
  atualizado_em   timestamptz not null default now(),

  constraint perfis_turma_so_de_aluno check (tipo = 'aluno' or turma_id is null),
  -- Decisão D11: só professores podem ser administradores
  constraint perfis_admin_so_professor check (not admin or tipo = 'professor'),
  constraint perfis_foto_na_pasta_do_perfil check (foto_path is null or foto_path like id::text || '/%')
);

create index perfis_turma_id_idx on public.perfis (turma_id) where turma_id is not null;

alter table public.perfis enable row level security;

-- =========================================================
-- ANO LETIVO E BIMESTRES
-- =========================================================

create table public.anos_letivos (
  ano          smallint primary key check (ano between 2000 and 2100),
  atual        boolean not null default false,
  iniciado_em  timestamptz not null default now(),
  encerrado_em timestamptz
);

-- No máximo um ano letivo atual
create unique index anos_letivos_um_atual on public.anos_letivos (atual) where atual;

alter table public.anos_letivos enable row level security;

create table public.bimestres (
  ano    smallint not null references public.anos_letivos (ano) on delete cascade,
  numero smallint not null check (numero between 1 and 4),
  inicio date not null,
  fim    date not null,
  primary key (ano, numero),
  check (fim >= inicio)
);

alter table public.bimestres enable row level security;

-- =========================================================
-- DISCIPLINAS E MATRÍCULAS
-- =========================================================

create table public.disciplinas (
  id            uuid primary key default gen_random_uuid(),
  nome          text not null check (btrim(nome) <> ''),
  -- Excluir a turma deixa a disciplina sem turma (ListaUsuariosActivity.java:314)
  turma_id      uuid references public.turmas (id) on delete set null,
  -- Professor com disciplinas não pode ser excluído (decisão D13)
  professor_id  uuid not null references public.perfis (id) on delete restrict,
  -- Preenchidos pelo servidor na criação (gatilho preencher_periodo)
  ano_letivo    smallint not null,
  bimestre      smallint check (bimestre between 1 and 4),
  -- Exclusão lógica pelo professor (TelaDisciplinaProfessor.java:730)
  ativo         boolean not null default true,
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);

create index disciplinas_professor_id_idx on public.disciplinas (professor_id);
create index disciplinas_turma_id_idx on public.disciplinas (turma_id);

alter table public.disciplinas enable row level security;

-- Mantidas pelos gatilhos de matrícula automática (decisão D9)
create table public.matriculas (
  disciplina_id uuid not null references public.disciplinas (id) on delete cascade,
  aluno_id      uuid not null references public.perfis (id) on delete cascade,
  criado_em     timestamptz not null default now(),
  primary key (disciplina_id, aluno_id)
);

create index matriculas_aluno_id_idx on public.matriculas (aluno_id);

alter table public.matriculas enable row level security;

-- =========================================================
-- ATIVIDADES ("trilhas") E ENTREGAS
-- =========================================================

create table public.atividades (
  -- O app pode gerar o id antes, para enviar o anexo para a pasta da atividade
  id            uuid primary key default gen_random_uuid(),
  disciplina_id uuid not null references public.disciplinas (id) on delete cascade,
  nome          text not null check (btrim(nome) <> ''),
  descricao     text not null default '',
  pontos        smallint not null check (pontos between 1 and 10),
  prazo         date not null,
  link          text,
  arquivo_path  text,
  arquivo_nome  text,
  ano_letivo    smallint not null,
  bimestre      smallint check (bimestre between 1 and 4),
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now(),

  constraint atividades_arquivo_completo check ((arquivo_path is null) = (arquivo_nome is null)),
  constraint atividades_arquivo_na_pasta check (
    arquivo_path is null or arquivo_path like disciplina_id::text || '/' || id::text || '/%'
  )
);

create index atividades_disciplina_id_idx on public.atividades (disciplina_id, criado_em);
create index atividades_criado_em_idx on public.atividades (criado_em);

alter table public.atividades enable row level security;

create table public.entregas (
  id                   uuid primary key default gen_random_uuid(),
  atividade_id         uuid not null references public.atividades (id) on delete cascade,
  aluno_id             uuid not null references public.perfis (id) on delete cascade,
  comentario_aluno     text not null default '',
  arquivo_path         text,
  arquivo_nome         text,
  enviado_em           timestamptz not null default now(),
  -- Decisão D4: entrega atrasada é aceita e marcada
  dias_atraso          smallint not null default 0 check (dias_atraso >= 0),
  -- Decisão D3: de 0 até os pontos da trilha (o máximo é conferido em avaliar_entrega)
  nota                 numeric(4, 2) check (nota >= 0),
  comentario_professor text,
  avaliado_em          timestamptz,
  ano_letivo           smallint not null,
  bimestre             smallint check (bimestre between 1 and 4),
  criado_em            timestamptz not null default now(),

  -- Uma entrega por aluno e trilha (TelaAtividadeAluno.java:149)
  unique (atividade_id, aluno_id),
  -- Não pode ser vazia (TelaAtividadeAluno.java:288)
  constraint entregas_nao_vazia check (btrim(comentario_aluno) <> '' or arquivo_path is not null),
  constraint entregas_arquivo_completo check ((arquivo_path is null) = (arquivo_nome is null)),
  constraint entregas_arquivo_na_pasta check (
    arquivo_path is null or arquivo_path like atividade_id::text || '/' || aluno_id::text || '/%'
  ),
  constraint entregas_avaliacao_completa check ((nota is null) = (avaliado_em is null))
);

create index entregas_aluno_id_idx on public.entregas (aluno_id);
create index entregas_enviado_em_idx on public.entregas (enviado_em);
create index entregas_avaliado_em_idx on public.entregas (avaliado_em) where avaliado_em is not null;

alter table public.entregas enable row level security;

-- =========================================================
-- AULAS E FREQUÊNCIA
-- =========================================================

create table public.aulas (
  id            uuid primary key default gen_random_uuid(),
  disciplina_id uuid not null references public.disciplinas (id) on delete cascade,
  data          date not null,
  quantidade    smallint not null check (quantidade between 1 and 20),
  descricao     text not null default '',
  ano_letivo    smallint not null,
  bimestre      smallint check (bimestre between 1 and 4),
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);

create index aulas_disciplina_id_idx on public.aulas (disciplina_id, data);

alter table public.aulas enable row level security;

create table public.frequencias (
  aula_id       uuid not null references public.aulas (id) on delete cascade,
  aluno_id      uuid not null references public.perfis (id) on delete cascade,
  status        text not null check (status in ('presente', 'falta', 'falta_justificada')),
  registrado_em timestamptz not null default now(),
  primary key (aula_id, aluno_id)
);

create index frequencias_aluno_id_idx on public.frequencias (aluno_id);

alter table public.frequencias enable row level security;

-- =========================================================
-- DIÁRIO
-- =========================================================

create table public.diario_entradas (
  id            uuid primary key default gen_random_uuid(),
  aluno_id      uuid not null references public.perfis (id) on delete cascade,
  texto         text not null check (btrim(texto) <> ''),
  criado_em     timestamptz not null default now(),
  atualizado_em timestamptz not null default now()
);

create index diario_entradas_aluno_idx on public.diario_entradas (aluno_id, criado_em desc);

alter table public.diario_entradas enable row level security;

-- =========================================================
-- PEDIDOS DE EXCLUSÃO DE DADOS (decisão D12)
-- =========================================================

create table public.solicitacoes_exclusao (
  id           uuid primary key default gen_random_uuid(),
  perfil_id    uuid not null references public.perfis (id) on delete cascade,
  motivo       text,
  status       text not null default 'pendente' check (status in ('pendente', 'aprovada', 'recusada')),
  decidido_por uuid references public.perfis (id) on delete set null,
  decidido_em  timestamptz,
  criado_em    timestamptz not null default now(),

  constraint solicitacoes_decisao_completa check ((status = 'pendente') = (decidido_em is null))
);

-- No máximo um pedido aberto por usuário
create unique index solicitacoes_uma_pendente on public.solicitacoes_exclusao (perfil_id)
  where status = 'pendente';
create index solicitacoes_decidido_por_idx on public.solicitacoes_exclusao (decidido_por);

alter table public.solicitacoes_exclusao enable row level security;

-- Comprovante de que a exclusão foi feita, sem nenhum dado pessoal (LGPD)
create table public.exclusoes_realizadas (
  id             uuid primary key default gen_random_uuid(),
  solicitacao_id uuid,
  tipo_perfil    text not null check (tipo_perfil in ('aluno', 'professor')),
  decidido_por   uuid references public.perfis (id) on delete set null,
  excluido_em    timestamptz not null default now()
);

create index exclusoes_decidido_por_idx on public.exclusoes_realizadas (decidido_por);

alter table public.exclusoes_realizadas enable row level security;
