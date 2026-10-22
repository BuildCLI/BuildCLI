package dev.buildcli.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Turns provider failures into one readable line and says which are worth retrying. Providers answer errors with JSON
 * bodies (OpenAI, OpenRouter) that also carry account ids; only the human message and the status survive.
 */
final class ProviderErrors {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final Pattern STATUS = Pattern.compile("\"code\"\\s*:\\s*(\\d{3})|status code:? ?(\\d{3})|HTTP (\\d{3})");

    private ProviderErrors() {}

    static String message(Throwable t) {
        String raw = firstMessage(t);
        if (raw == null) {
            return t.getClass().getSimpleName();
        }
        int brace = raw.indexOf('{');
        if (brace >= 0) {
            try {
                JsonNode root = JSON.readTree(raw.substring(brace));
                JsonNode err = root.has("error") ? root.get("error") : root;
                String msg = err.path("message").asText("");
                String upstream = err.path("metadata").path("raw").asText("");
                String code = err.path("code").asText("");
                StringBuilder sb = new StringBuilder();
                if (!upstream.isBlank() && !upstream.startsWith("{")) {
                    sb.append(upstream);
                } else if (!msg.isBlank()) {
                    sb.append(msg);
                }
                if (!code.isBlank() && sb.indexOf(code) < 0) {
                    sb.append(" (").append(code).append(')');
                }
                if (!sb.isEmpty()) {
                    return sb.toString();
                }
            } catch (Exception ignored) {
                // not JSON after all: fall through to the raw text
            }
        }
        return raw.length() > 300 ? raw.substring(0, 300) + "…" : raw;
    }

    /** Rate limits, overloaded or unavailable servers and dropped connections pass if you wait a moment. */
    static boolean retryable(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof java.net.ConnectException) {
                return false; // nothing listening: waiting will not help (Ollama not started, wrong port)
            }
            if (c instanceof java.net.SocketTimeoutException || c instanceof java.net.http.HttpTimeoutException) {
                return false; // the call already waited for minutes
            }
            String m = c.getMessage();
            if (m == null) {
                continue;
            }
            Matcher s = STATUS.matcher(m);
            while (s.find()) {
                String code = s.group(1) != null ? s.group(1) : s.group(2) != null ? s.group(2) : s.group(3);
                int status = Integer.parseInt(code);
                if (status == 429 || status == 500 || status == 502 || status == 503 || status == 504 || status == 529) {
                    return true;
                }
            }
            if (m.contains("rate-limited") || m.contains("Rate limit") || m.contains("overloaded") || m.contains("Connection reset")) {
                return true;
            }
        }
        return false;
    }

    private static String firstMessage(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            boolean wrapperOnly = c.getCause() != null && c.getCause().toString().equals(c.getMessage());
            if (c.getMessage() != null && !c.getMessage().isBlank() && !wrapperOnly) {
                return c.getMessage();
            }
        }
        return null;
    }
}
