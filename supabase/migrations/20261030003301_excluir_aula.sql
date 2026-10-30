-- Excluir aula (RegistrarFrequenciaActivity.excluirAula).
--
-- O inventário dizia que não havia tela para excluir aula, mas há: o professor
-- exclui a aula pela lista de frequência. A migração de permissões não previa
-- isso. Agora o professor da disciplina e o admin apagam a aula.
--
-- A frequência da aula vai junto (frequencias.aula_id on delete cascade), e
-- como o desconto de 5 pontos por dia de falta é calculado a partir da
-- frequência (privado.pontuacao_aluno), o desconto daquele dia some sozinho.
-- No app antigo a frequência ficava órfã e os pontos continuavam descontados.

grant delete on public.aulas to authenticated;

create policy aulas_delete on public.aulas for delete to authenticated
  using ((select privado.eh_admin()) or privado.eh_professor_da_disciplina(disciplina_id));
