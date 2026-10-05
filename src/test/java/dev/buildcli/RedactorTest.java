package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.Redactor;
import org.junit.jupiter.api.Test;

class RedactorTest {

    @Test
    void providerTokensAreMasked() {
        String text = "openai sk-abcdEFGH1234567890abcd, github ghp_abcdefghijklmnopqrstuvwxyz0123456789, aws AKIAIOSFODNN7EXAMPLE";
        String out = Redactor.redact(text);
        assertFalse(out.contains("sk-abcd"), out);
        assertFalse(out.contains("ghp_"), out);
        assertFalse(out.contains("AKIAIOSFODNN7EXAMPLE"), out);
        assertTrue(out.contains(Redactor.MASK));
    }

    @Test
    void secretLikeAssignmentsKeepTheNameAndMaskTheValue() {
        assertEquals("db.password=[REDACTED]", Redactor.redact("db.password=hunter2"));
        assertEquals("API_KEY: [REDACTED]", Redactor.redact("API_KEY: abc123"));
        assertEquals("\"client_secret\": [REDACTED]", Redactor.redact("\"client_secret\": \"s3cr3t\""));
        assertEquals("token = [REDACTED]", Redactor.redact("token = 'abcdef12'"));
    }

    @Test
    void privateKeyBlocksAreMaskedWhole() {
        String pem = "before\n-----BEGIN RSA PRIVATE KEY-----\nMIIabc\ndef\n-----END RSA PRIVATE KEY-----\nafter";
        assertEquals("before\n" + Redactor.MASK + "\nafter", Redactor.redact(pem));
    }

    @Test
    void jwtsBearerHeadersAndUrlCredentialsAreMasked() {
        String jwt = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        assertFalse(Redactor.redact("t " + jwt).contains("eyJhbGci"));
        assertEquals("Authorization: " + Redactor.MASK, Redactor.redact("Authorization: Bearer abcdefghijklmnop123456"));
        assertEquals("https://" + Redactor.MASK + "@host/x", Redactor.redact("https://user:pass@host/x"));
    }

    @Test
    void ordinaryTextIsLeftAlone() {
        String text = "mvn -q verify\npublic class Token { private final int tokens = 3; }\nThe password policy is documented.";
        assertEquals(text, Redactor.redact(text));
    }

    @Test
    void shortValuesAndLiteralsAreCodeNotCredentials() {
        String text = "int tokens = 3;\npassword = null\nsecret: true";
        assertEquals(text, Redactor.redact(text));
    }

    @Test
    void nullAndEmptyPassThrough() {
        assertEquals(null, Redactor.redact(null));
        assertEquals("", Redactor.redact(""));
    }
}
