package br.com.example.meufuturoagora;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Lista de acesso do aplicativo. Só entra quem tiver o e-mail (da conta
 * Google) cadastrado aqui — como professor ou como aluno. Para liberar
 * alguém, adicione o e-mail na lista correspondente.
 *
 * O aluno já entra com a turma definida aqui, então as disciplinas
 * daquela turma aparecem para ele automaticamente, sem precisar escolher.
 */
final class UsuariosAutorizados {

    private UsuariosAutorizados() {
    }

    // =====================================================
    // PROFESSORES
    // =====================================================

    static final List<String> PROFESSORES = Arrays.asList(
            "professor.exemplo@gmail.com",
            "maria.professora@gmail.com",
            "arlindo.franklim@gmail.com"
    );

    // =====================================================
    // ALUNOS (e-mail -> turma)
    // =====================================================

    static final Map<String, String> ALUNOS = new LinkedHashMap<>();

    static {
        ALUNOS.put("aluno.exemplo@gmail.com", "9º ano");
        ALUNOS.put("arlindo.franklim@gmail.com", "9º ano");
        ALUNOS.put("barbara.aluna@gmail.com", "6º ano");
        ALUNOS.put("hosanadiniz71@gmail.com", "8º ano");
    }

    static boolean ehProfessor(String email) {

        if (email == null) {
            return false;
        }

        for (String professorEmail : PROFESSORES) {

            if (professorEmail.equalsIgnoreCase(email)) {
                return true;
            }
        }

        return false;
    }

    static String turmaDoAluno(String email) {

        if (email == null) {
            return null;
        }

        for (Map.Entry<String, String> entrada : ALUNOS.entrySet()) {

            if (entrada.getKey().equalsIgnoreCase(email)) {
                return entrada.getValue();
            }
        }

        return null;
    }
}
