package br.com.example.meufuturoagora.dados;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Desempenho por bimestre, calculado no aparelho a partir do que o RLS já deixa
// o usuário ler. Os pontos vêm das entregas avaliadas e entram no bimestre do
// PRAZO da atividade; as faltas entram no bimestre da DATA da aula. Os períodos
// são os que o administrador definiu (fora deles, bimestre 0, nada é contado).
public final class Desempenho {

    // Pontos e faltas de um aluno num bimestre (opcionalmente numa disciplina)
    public static final class Resumo {

        public final double pontos;
        public final int faltas;
        public final int justificadas;
        // Atividades da disciplina com prazo no bimestre e quantas já foram avaliadas
        public final int atividades;
        public final int avaliadas;

        Resumo(double pontos, int faltas, int justificadas, int atividades, int avaliadas) {
            this.pontos = pontos;
            this.faltas = faltas;
            this.justificadas = justificadas;
            this.atividades = atividades;
            this.avaliadas = avaliadas;
        }
    }

    private final List<Bimestre> bimestres;
    private final Map<String, Atividade> atividades = new HashMap<>();
    private final List<Entrega> entregas;
    private final Map<String, Aula> aulas = new HashMap<>();
    private final List<Frequencia> frequencias;

    // Só entram atividades, aulas e frequências das disciplinas que a tela mostra
    public Desempenho(List<Bimestre> bimestres, List<Atividade> atividades, List<Entrega> entregas,
                      List<Aula> aulas, List<Frequencia> frequencias) {
        this.bimestres = bimestres;
        this.entregas = entregas;
        this.frequencias = frequencias;

        for (Atividade a : atividades) {
            this.atividades.put(a.id, a);
        }

        for (Aula a : aulas) {
            this.aulas.put(a.id, a);
        }
    }

    // 0 = fora de qualquer bimestre definido
    public int bimestreDa(LocalDate data) {

        for (Bimestre b : bimestres) {

            if (b.contem(data)) {
                return b.numero;
            }
        }

        return 0;
    }

    // Todas as disciplinas da tela somadas (linha do aluno na visão do professor)
    public Resumo doAluno(String alunoId, int bimestre) {
        return resumir(alunoId, null, bimestre);
    }

    // Uma disciplina só (linha da disciplina na visão do aluno)
    public Resumo daDisciplina(String alunoId, String disciplinaId, int bimestre) {
        return resumir(alunoId, disciplinaId, bimestre);
    }

    private Resumo resumir(String alunoId, String disciplinaId, int bimestre) {

        double pontos = 0;
        int avaliadas = 0;
        int totalAtividades = 0;
        int faltas = 0;
        int justificadas = 0;

        if (disciplinaId != null) {

            for (Atividade a : atividades.values()) {

                if (disciplinaId.equals(a.disciplinaId) && bimestreDa(a.prazo) == bimestre) {
                    totalAtividades++;
                }
            }
        }

        for (Entrega e : entregas) {

            Atividade atividade = atividades.get(e.atividadeId);

            if (atividade == null || !e.avaliada() || e.nota == null || !alunoId.equals(e.alunoId)
                    || (disciplinaId != null && !disciplinaId.equals(atividade.disciplinaId))
                    || bimestreDa(atividade.prazo) != bimestre) {
                continue;
            }

            pontos += e.nota;
            avaliadas++;
        }

        for (Frequencia f : frequencias) {

            Aula aula = aulas.get(f.aulaId);

            if (aula == null || !alunoId.equals(f.alunoId)
                    || (disciplinaId != null && !disciplinaId.equals(aula.disciplinaId))
                    || bimestreDa(aula.data) != bimestre) {
                continue;
            }

            if (Frequencia.FALTA.equals(f.status)) {
                faltas++;
            } else if (Frequencia.FALTA_JUSTIFICADA.equals(f.status)) {
                justificadas++;
            }
        }

        return new Resumo(pontos, faltas, justificadas, totalAtividades, avaliadas);
    }
}
