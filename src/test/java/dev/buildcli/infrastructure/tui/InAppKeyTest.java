package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.Settings;
import dev.buildcli.domain.Task;
import dev.buildcli.domain.TaskStatus;
import dev.buildcli.infrastructure.ModelCatalog;
import dev.tamboui.tui.event.KeyCode;
import dev.tamboui.tui.event.PasteEvent;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Typing an API key inside the app: checked before it is saved, masked, saved once, and followed by the model list. */
class InAppKeyTest {
    @TempDir Path dir;

    final Settings settings = Settings.defaults();
    final Map<String, String> saved = new HashMap<>();
    final List<String> checked = new ArrayList<>();

    SettingsServices services() {
        return new SettingsServices() {
            @Override
            public Settings settings() {
                return settings;
            }

            @Override
            public List<Provider> providers() {
                boolean has = saved.containsKey("deepseek");
                return List.of(new Provider("deepseek", "https://api.deepseek.com/v1", "DEEPSEEK_API_KEY", has, false, "DeepSeek", has ? "saved" : ""));
            }

            @Override
            public void addProvider(String name, String url, String keyEnv) { }

            @Override
            public void removeProvider(String name) { }

            @Override
            public CompletableFuture<String> test(String model) {
                return CompletableFuture.completedFuture("ok 10 ms: pong");
            }

            @Override
            public CompletableFuture<ModelCatalog.Result> models(String provider) {
                return CompletableFuture.completedFuture(saved.containsKey(provider)
                        ? new ModelCatalog.Result(List.of(new ModelCatalog.Model("deepseek:deepseek-chat", "", true, false)), null)
                        : new ModelCatalog.Result(List.of(), "DEEPSEEK_API_KEY is not set"));
            }

            @Override
            public List<AgentInfo> agents() {
                return List.of();
            }

            @Override
            public String createAgent(String name, String role, String instructions, List<String> capabilities, boolean global) {
                return "";
            }

            @Override
            public void deleteAgent(String name) { }


            @Override
            public CompletableFuture<ModelCatalog.Result> checkKey(String provider, String key) {
                checked.add(key);
                return CompletableFuture.completedFuture(key.startsWith("good") ? new ModelCatalog.Result(List.of(), null)
                        : key.startsWith("bad") ? new ModelCatalog.Result(List.of(), "HTTP 401: invalid api key")
                        : new ModelCatalog.Result(List.of(), "connection refused"));
            }

            @Override
            public void saveKey(String provider, String key) {
                saved.put(provider, key);
            }

            @Override
            public boolean forgetKey(String provider) {
                return saved.remove(provider) != null;
            }

            @Override
            public String keyFile() {
                return "/home/me/.buildcli/credentials.json";
            }
        };
    }

    ChatScreen screen() {
        var session = new ChatSession(ChatScreenTest.ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            Task t = new Task(1, null, "user", "ana", request.text(), "");
            t.status = TaskStatus.DONE;
            t.result = "ok";
            return t;
        });
        return new ChatScreen(session, Map.of(), dir, () -> { }, services());
    }

    static String render(ChatScreen s) {
        return ChatScreenTest.render(s, 130, 40);
    }

    static void until(ChatScreen screen, String text) throws Exception {
        long end = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!render(screen).contains(text) && System.nanoTime() < end) {
            Thread.sleep(20);
        }
    }

    @Test
    void aCorrectKeyIsCheckedSavedMaskedAndLeadsStraightToTheModels() throws Exception {
        var screen = screen();
        render(screen);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.type(screen, "good-key-1234567890abcd");
        String typed = render(screen);
        assertFalse(typed.contains("good-key"), "the key is never shown:\n" + typed);
        assertTrue(typed.contains("•••••••••••••••••••abcd"), "only the last four characters are visible:\n" + typed);
        assertTrue(typed.contains("account only") && typed.contains("/home/me/.buildcli/credentials.json"), typed);
        assertTrue(saved.isEmpty(), "nothing is saved before it was checked");

        ChatScreenTest.key(screen, KeyCode.ENTER);
        until(screen, "Which deepseek model?");
        assertEquals(List.of("good-key-1234567890abcd"), checked);
        assertEquals("good-key-1234567890abcd", saved.get("deepseek"));
        assertTrue(render(screen).contains("deepseek-chat"), "it went on to the model list by itself");
    }

    @Test
    void aKeyThatTheProviderRejectsIsNotSavedAndTheFieldIsCleared() throws Exception {
        var screen = screen();
        render(screen);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.type(screen, "bad-key-1234567890");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        until(screen, "did not accept that key");
        String out = render(screen);
        assertTrue(out.contains("deepseek did not accept that key (HTTP 401: invalid api key)"), out);
        assertTrue(saved.isEmpty(), "a rejected key is never kept");
        assertTrue(out.contains("Paste the key here"), "the field is empty again:\n" + out);
    }

    @Test
    void whenTheProviderCannotBeReachedASecondEnterSavesItAnyway() throws Exception {
        var screen = screen();
        render(screen);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.type(screen, "offline-key-1234567890");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        until(screen, "Press Enter again to save it anyway");
        assertTrue(saved.isEmpty());
        ChatScreenTest.key(screen, KeyCode.ENTER);
        assertEquals("offline-key-1234567890", saved.get("deepseek"));
        assertEquals(1, checked.size(), "the second Enter did not check again");
    }

    @Test
    void aPastedKeyLosesItsLineBreakAndTypingAfterAFailureStartsOver() throws Exception {
        var screen = screen();
        render(screen);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        screen.handlePasteEvent(new PasteEvent("good-pasted-key-0987654321\n"));
        ChatScreenTest.key(screen, KeyCode.ENTER);
        until(screen, "Which deepseek model?");
        assertEquals("good-pasted-key-0987654321", saved.get("deepseek"), "no newline in the saved key");
    }

    @Test
    void emptyOrSpacedInputIsRefusedWithoutAskingTheProvider() throws Exception {
        var screen = screen();
        render(screen);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        assertTrue(render(screen).contains("Paste the key first."));
        assertTrue(checked.isEmpty());
    }

    @Test
    void escGoesBackToTheListWithoutSavingAndDForgetsASavedKeyOnlyOnTheSecondPress() throws Exception {
        var screen = screen();
        render(screen);
        ChatScreenTest.key(screen, KeyCode.ENTER);
        ChatScreenTest.type(screen, "good-key-1234567890");
        ChatScreenTest.key(screen, KeyCode.ESCAPE);
        String out = render(screen);
        assertTrue(out.contains("Where should your agents' model come from?"), out);
        assertTrue(saved.isEmpty());

        saved.put("deepseek", "good-key-1234567890");
        ChatScreenTest.type(screen, "r"); // look again: the key is there now
        until(screen, "Key: saved by you");
        ChatScreenTest.type(screen, "d");
        assertTrue(render(screen).contains("Press D again to forget the saved key for deepseek."));
        assertTrue(saved.containsKey("deepseek"), "one press only asks");
        ChatScreenTest.type(screen, "d");
        assertTrue(saved.isEmpty(), "the second press forgets it");
        assertTrue(render(screen).contains("Forgot the saved key for deepseek."));
    }
}
