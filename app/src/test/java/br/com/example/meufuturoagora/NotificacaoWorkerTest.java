package br.com.example.meufuturoagora;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import java.time.OffsetDateTime;

import br.com.example.meufuturoagora.dados.Novidade;

// Texto e dados de cada tipo de novidade que o servidor devolve
public class NotificacaoWorkerTest {

    private static Novidade novidade(String tipo) {
        return new Novidade(tipo, "item-1", "Frações", "disc-1", "Matemática", "ativ-1", "Ana",
                OffsetDateTime.now());
    }

    @Test
    public void novaDisciplinaAvisaONomeEGuardaOId() {
        NotificacaoHistorico.Item item = NotificacaoWorker.montarItem(novidade(Novidade.DISCIPLINA));

        assertEquals("Nova disciplina", item.titulo);
        assertEquals("Foi adicionada a disciplina Frações", item.mensagem);
        assertEquals(NotificacaoHistorico.TIPO_DISCIPLINA, item.tipo);
        assertEquals("disc-1", item.extras.get("disciplinaId"));
    }

    @Test
    public void novaAtividadeNomeiaADisciplinaEGuardaOIdDaAtividade() {
        NotificacaoHistorico.Item item = NotificacaoWorker.montarItem(novidade(Novidade.ATIVIDADE));

        assertEquals("Nova atividade em Matemática", item.titulo);
        assertEquals("Frações foi adicionada pelo professor", item.mensagem);
        assertEquals(NotificacaoHistorico.TIPO_ATIVIDADE, item.tipo);
        assertEquals("ativ-1", item.extras.get("atividadeId"));
    }

    @Test
    public void avaliacaoAvisaQueOProfessorAvaliou() {
        NotificacaoHistorico.Item item = NotificacaoWorker.montarItem(novidade(Novidade.AVALIACAO));

        assertEquals("Atividade avaliada em Matemática", item.titulo);
        assertEquals("O professor avaliou Frações.", item.mensagem);
        assertEquals("ativ-1", item.extras.get("atividadeId"));
    }

    @Test
    public void novaEntregaNomeiaOAlunoEGuardaOIdDaEntrega() {
        NotificacaoHistorico.Item item = NotificacaoWorker.montarItem(novidade(Novidade.ENTREGA));

        assertEquals("Nova entrega em Matemática", item.titulo);
        assertEquals("Ana enviou a atividade Frações", item.mensagem);
        assertEquals(NotificacaoHistorico.TIPO_ENTREGA, item.tipo);
        assertEquals("item-1", item.extras.get("entregaId"));
        assertEquals("ativ-1", item.extras.get("atividadeId"));
    }

    @Test
    public void nomesAusentesViramTextoPadrao() {
        Novidade vazia = new Novidade(Novidade.ENTREGA, "i", null, null, null, "a", null, OffsetDateTime.now());

        NotificacaoHistorico.Item item = NotificacaoWorker.montarItem(vazia);

        assertEquals("Nova entrega em sua disciplina", item.titulo);
        assertEquals("Um aluno enviou a atividade ", item.mensagem);
    }

    @Test
    public void tipoDesconhecidoNaoGeraNotificacao() {
        assertNull(NotificacaoWorker.montarItem(novidade("outra-coisa")));
    }
}
