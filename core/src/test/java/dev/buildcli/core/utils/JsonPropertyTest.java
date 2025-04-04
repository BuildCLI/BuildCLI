package dev.buildcli.core.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
public class JsonPropertyTest {


    @Test
    void testVal_withModel() {
        assertEquals("model",JsonProperty.MODEL.val());
    }

    @Test
    void testVal_withRole() {
        assertEquals("role",JsonProperty.ROLE.val());
    }

    @Test
    void testVal_withContent() {
        assertEquals("content",JsonProperty.CONTENT.val());
    }

    @Test
    void testVal_withMessages() {
        assertEquals("messages",JsonProperty.MESSAGES.val());
    }

    @Test
    void testVal_withMessage() {
        assertEquals("message",JsonProperty.MESSAGE.val());
    }

    @Test
    void testVal_withChoices() {
        assertEquals("choices",JsonProperty.CHOICES.val());
    }
}
