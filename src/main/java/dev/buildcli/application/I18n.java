package dev.buildcli.application;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * The language of what BuildCLI shows: English, or Portuguese. The English text is the key: {@code t("Type a message")} gives
 * the Portuguese sentence when that language is on and the entry exists, and the English one otherwise, so a sentence nobody
 * translated yet still shows. Translations live in {@code i18n/pt.tsv} (English, a tab, Portuguese; {@code \n} is a line break).
 * {@code {0}}, {@code {1}}... in a text are replaced by the arguments.
 */
public final class I18n {
    public static final String AUTO = "auto";
    public static final String EN = "en";
    public static final String PT = "pt";

    private static final Map<String, String> PT_TEXTS = load("/i18n/pt.tsv");
    private static volatile String language = EN;

    private I18n() { }

    /** Chooses the language: {@code en}, {@code pt}, or {@code auto} (the computer's own, English if it is not one we have). */
    public static void use(String choice) {
        String c = choice == null ? AUTO : choice.strip().toLowerCase(Locale.ROOT);
        if (c.equals(AUTO) || c.isEmpty()) {
            c = Locale.getDefault().getLanguage().equalsIgnoreCase(PT) ? PT : EN;
        }
        language = c.equals(PT) ? PT : EN;
    }

    public static String language() {
        return language;
    }

    /** The text in the language in use. */
    public static String t(String english, Object... args) {
        String text = english;
        if (language.equals(PT)) {
            String pt = PT_TEXTS.get(english);
            if (pt != null) {
                text = pt;
            }
        }
        for (int i = 0; i < args.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(args[i]));
        }
        return text;
    }

    /** What an agent is doing ("thinking", "waiting for bruno"): the fixed ones and the one that names a teammate. */
    public static String state(String state) {
        if (state.startsWith("waiting for ") && !PT_TEXTS.containsKey(state)) {
            return t("waiting for {0}", state.substring("waiting for ".length()));
        }
        return t(state);
    }

    /** How many texts the Portuguese file has, for tests. */
    static int portugueseTexts() {
        return PT_TEXTS.size();
    }

    static Map<String, String> portuguese() {
        return PT_TEXTS;
    }

    private static Map<String, String> load(String resource) {
        Map<String, String> out = new HashMap<>();
        try (InputStream in = I18n.class.getResourceAsStream(resource)) {
            if (in == null) {
                return out;
            }
            BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                int tab = line.indexOf('\t');
                if (line.isBlank() || line.startsWith("#") || tab <= 0) {
                    continue;
                }
                out.put(line.substring(0, tab).replace("\\n", "\n"), line.substring(tab + 1).replace("\\n", "\n"));
            }
        } catch (IOException e) {
            return out;
        }
        return out;
    }
}
