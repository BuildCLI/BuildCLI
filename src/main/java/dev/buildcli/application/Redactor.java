package dev.buildcli.application;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Best-effort removal of secrets from text that leaves the runtime: tool output sent to the model, and the event log.
 * It recognises common credential formats and {@code key = value} assignments with secret-like names. It cannot find
 * every secret (an arbitrary password in prose is invisible to it), so it is a safety net, not a guarantee.
 */
public final class Redactor {
    public static final String MASK = "[REDACTED]";

    private static final List<Pattern> PATTERNS = List.of(
            // PEM private keys (whole block)
            Pattern.compile("-----BEGIN [A-Z ]*PRIVATE KEY-----.*?-----END [A-Z ]*PRIVATE KEY-----", Pattern.DOTALL),
            // provider tokens
            Pattern.compile("\\bsk-[A-Za-z0-9_-]{16,}"),
            Pattern.compile("\\bgh[pousr]_[A-Za-z0-9]{30,}"),
            Pattern.compile("\\bgithub_pat_[A-Za-z0-9_]{30,}"),
            Pattern.compile("\\bxox[baprs]-[A-Za-z0-9-]{10,}"),
            Pattern.compile("\\bAKIA[0-9A-Z]{16}\\b"),
            Pattern.compile("\\bAIza[0-9A-Za-z_-]{30,}"),
            // JSON web tokens
            Pattern.compile("\\beyJ[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}\\.[A-Za-z0-9_-]{8,}"),
            // Authorization headers
            Pattern.compile("(?i)\\b(bearer|basic)\\s+[A-Za-z0-9._~+/=-]{12,}"),
            // credentials embedded in URLs
            Pattern.compile("(?<=://)[^/\\s:@]+:[^/\\s@]+(?=@)"));

    /** name = value / name: value where the name looks like a secret. Keeps the name, masks the value. */
    private static final Pattern ASSIGNMENT = Pattern.compile(
            "(?i)(\\b[\\w.-]*(?:api[_-]?key|secret|token|passw(?:or)?d|private[_-]?key|credential)[\\w.-]*\"?\\s*[:=]\\s*)"
                    + "(\"[^\"\\n]*\"|'[^'\\n]*'|[^\\s,;\"']+)");

    private static final int MIN_SECRET_LENGTH = 6;
    private static final java.util.Set<String> LITERALS = java.util.Set.of("true", "false", "null", "none", "undefined");

    private Redactor() {}

    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = text;
        for (Pattern p : PATTERNS) {
            out = p.matcher(out).replaceAll(MASK);
        }
        return ASSIGNMENT.matcher(out).replaceAll(m -> java.util.regex.Matcher.quoteReplacement(
                looksLikeASecret(m.group(2)) ? m.group(1) + MASK : m.group()));
    }

    /** Short values and literals (tokens = 3, password = null) are code, not credentials; masking them is only noise. */
    private static boolean looksLikeASecret(String value) {
        String v = value.replaceAll("(?:^[\"'])|(?:[\"']$)", "");
        return v.length() >= MIN_SECRET_LENGTH && !LITERALS.contains(v.toLowerCase());
    }
}
