package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.I18n;
import dev.buildcli.application.Settings;
import dev.buildcli.infrastructure.tui.ChatCommandsAccess;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** English and Portuguese: the texts exist, say the same things, and a missing one falls back to English. */
class I18nTest {
    @AfterEach
    void backToEnglish() {
        I18n.use("en");
    }

    private static List<String[]> entries() throws Exception {
        List<String[]> out = new ArrayList<>();
        try (InputStream in = I18nTest.class.getResourceAsStream("/i18n/pt.tsv")) {
            for (String line : new String(in.readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
                int tab = line.indexOf('\t');
                if (!line.isBlank() && !line.startsWith("#") && tab > 0) {
                    out.add(new String[] {line.substring(0, tab).replace("\\n", "\n"), line.substring(tab + 1).replace("\\n", "\n")});
                }
            }
        }
        return out;
    }

    private static TreeSet<String> placeholders(String text) {
        TreeSet<String> out = new TreeSet<>();
        Matcher m = Pattern.compile("\\{\\d+}").matcher(text);
        while (m.find()) {
            out.add(m.group());
        }
        return out;
    }

    @Test
    void portugueseGivesPortugueseAndEnglishStaysEnglishAndAMissingTextFallsBack() {
        I18n.use("pt");
        assertEquals("Escreve uma mensagem", I18n.t("Type a message"));
        assertEquals("Reencaminhado de Ana:", I18n.t("Forwarded from {0}:", "Ana"));
        assertEquals("a sentence nobody translated", I18n.t("a sentence nobody translated"), "English shows when there is no translation");
        assertEquals("à espera de bruno", I18n.state("waiting for bruno"));
        I18n.use("en");
        assertEquals("Type a message", I18n.t("Type a message"));
        assertEquals("Forwarded from Ana:", I18n.t("Forwarded from {0}:", "Ana"));
        assertEquals("waiting for bruno", I18n.state("waiting for bruno"));
    }

    @Test
    void autoFollowsTheComputersLanguageAndAnythingElseIsEnglish() {
        java.util.Locale was = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.forLanguageTag("pt-PT"));
            I18n.use("auto");
            assertEquals("pt", I18n.language());
            java.util.Locale.setDefault(java.util.Locale.FRENCH);
            I18n.use("auto");
            assertEquals("en", I18n.language());
            I18n.use("klingon");
            assertEquals("en", I18n.language());
        } finally {
            java.util.Locale.setDefault(was);
        }
    }

    @Test
    void everyTranslationKeepsThePlaceholdersOfItsEnglishText() throws Exception {
        for (String[] e : entries()) {
            assertEquals(placeholders(e[0]), placeholders(e[1]), "placeholders differ in: " + e[0]);
        }
    }

    @Test
    void theSettingsAndTheCommandsAreAllTranslated() throws Exception {
        TreeSet<String> have = new TreeSet<>();
        entries().forEach(e -> have.add(e[0]));
        List<String> missing = new ArrayList<>();
        for (Settings.Definition d : Settings.DEFINITIONS) {
            for (String text : List.of(d.label(), d.help())) {
                if (!have.contains(text)) {
                    missing.add(text);
                }
            }
        }
        for (String text : ChatCommandsAccess.commandDescriptions()) {
            if (!have.contains(text)) {
                missing.add(text);
            }
        }
        for (dev.buildcli.application.ApprovalMode m : dev.buildcli.application.ApprovalMode.values()) {
            if (!have.contains(m.description())) {
                missing.add(m.description());
            }
        }
        assertTrue(missing.isEmpty(), "no Portuguese for: " + missing);
    }
}
