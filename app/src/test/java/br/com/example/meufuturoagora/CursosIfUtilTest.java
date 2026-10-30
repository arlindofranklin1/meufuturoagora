package br.com.example.meufuturoagora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

// Conteúdo fixo, que não vai para o banco: os testes só garantem que os
// três cursos continuam completos e encontráveis pelo id.
public class CursosIfUtilTest {

    @Test
    public void listaOsTresCursosNaOrdemDaTela() {
        List<CursosIfUtil.Curso> cursos = CursosIfUtil.todos();

        assertEquals(3, cursos.size());
        assertEquals(CursosIfUtil.INFORMATICA, cursos.get(0).id);
        assertEquals(CursosIfUtil.ENERGIA, cursos.get(1).id);
        assertEquals(CursosIfUtil.ELETRONICA, cursos.get(2).id);
    }

    @Test
    public void encontraCadaCursoPeloId() {
        for (CursosIfUtil.Curso curso : CursosIfUtil.todos()) {
            assertEquals(curso.nome, CursosIfUtil.porId(curso.id).nome);
        }
    }

    @Test
    public void idDesconhecidoDevolveNulo() {
        assertNull(CursosIfUtil.porId("medicina"));
    }

    @Test
    public void cadaCursoTemOsCamposDaTelaPreenchidos() {
        Set<String> ids = new HashSet<>();

        for (CursosIfUtil.Curso curso : CursosIfUtil.todos()) {

            assertTrue("id repetido: " + curso.id, ids.add(curso.id));
            assertFalse(curso.nome.isEmpty());
            assertFalse(curso.nomeCompleto.isEmpty());
            assertFalse(curso.descricao.isEmpty());
            assertNotNull(curso.infos);
            assertFalse(curso.anos.isEmpty());

            for (CursosIfUtil.Ano ano : curso.anos) {
                assertFalse(curso.id + " / " + ano.titulo, ano.disciplinas.isEmpty());

                for (CursosIfUtil.Disciplina disciplina : ano.disciplinas) {
                    assertFalse(disciplina.nome.isEmpty());
                }
            }
        }
    }
}
