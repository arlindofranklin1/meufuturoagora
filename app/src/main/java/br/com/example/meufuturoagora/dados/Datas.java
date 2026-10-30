package br.com.example.meufuturoagora.dados;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

// Datas como a tela mostra e o usuário digita (dd/MM/aaaa). O servidor sempre
// recebe e devolve aaaa-MM-dd (tipo date).
public final class Datas {

    // STRICT: "31/02/2026" é recusada (o SimpleDateFormat antigo aceitava e virava março)
    private static final DateTimeFormatter FORMATO =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Datas() {
    }

    public static String formatar(LocalDate data) {
        return data != null ? data.format(FORMATO) : "";
    }

    // Texto vazio = sem data (null); texto inválido lança IllegalArgumentException
    public static LocalDate ler(String texto) {

        if (texto == null || texto.trim().isEmpty()) {
            return null;
        }

        try {
            return LocalDate.parse(texto.trim(), FORMATO);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("Data inválida: " + texto.trim(), e);
        }
    }
}
