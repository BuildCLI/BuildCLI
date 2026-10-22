package dev.buildcli.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ProviderErrorsTest {
    static final String OPENROUTER_429 = "{\"error\":{\"message\":\"Provider returned error\",\"code\":429,\"metadata\":{\"raw\":"
            + "\"google/gemma:free is temporarily rate-limited upstream.\",\"provider_name\":\"Google\"}},\"user_id\":\"user_123secret\"}";

    @Test
    void jsonErrorsBecomeOneReadableLineWithoutAccountIds() {
        String m = ProviderErrors.message(new RuntimeException(OPENROUTER_429));
        assertEquals("google/gemma:free is temporarily rate-limited upstream. (429)", m);
        assertFalse(m.contains("user_"));
        assertEquals("Incorrect API key provided (invalid_api_key)", ProviderErrors.message(new RuntimeException(
                "{\"error\":{\"message\":\"Incorrect API key provided\",\"code\":\"invalid_api_key\"}}")));
        assertEquals("plain text", ProviderErrors.message(new RuntimeException(new RuntimeException("plain text"))));
    }

    @Test
    void rateLimitsAndServerErrorsAreRetriedButNotBadRequestsOrNothingListening() {
        assertTrue(ProviderErrors.retryable(new RuntimeException(OPENROUTER_429)));
        assertTrue(ProviderErrors.retryable(new RuntimeException("status code: 503")));
        assertFalse(ProviderErrors.retryable(new RuntimeException("{\"error\":{\"code\":400,\"message\":\"bad\"}}")));
        assertFalse(ProviderErrors.retryable(new RuntimeException("x", new java.net.ConnectException("Connection refused"))));
    }
}
