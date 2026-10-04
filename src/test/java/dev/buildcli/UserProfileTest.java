package dev.buildcli;

import static dev.buildcli.PeopleRuntimeTest.ROSTER;
import static dev.buildcli.PeopleRuntimeTest.awaitIdle;
import static dev.buildcli.PeopleRuntimeTest.done;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.Settings;
import dev.buildcli.application.UserProfile;
import dev.buildcli.ports.SettingsStore.Scope;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

/** What the agents are told about the person they work for, so they talk to someone and not to "the user". */
@Timeout(20)
class UserProfileTest {
    @Test
    void withNoProfileTheAgentsAreStillToldNotToSayTheUser() {
        String text = UserProfile.describe(Settings.defaults());
        assertTrue(text.contains("Say 'you'") && text.contains("Never call them 'the user'"), text);
        assertFalse(text.contains("What they told you"), "nothing invented");
    }

    @Test
    void theNameTheAboutAndTheStyleReachEveryAgentOfEveryChat() throws Exception {
        Settings settings = Settings.defaults();
        UserProfile.set(settings, Settings.PROFILE_NAME, "Dumi");
        UserProfile.set(settings, Settings.PROFILE_ABOUT, "I run a small bakery and write the newsletter myself.");
        UserProfile.set(settings, Settings.PROFILE_STYLE, "Short answers, informal, in Portuguese.");
        assertEquals("Dumi", settings.stored(Scope.GLOBAL, Settings.PROFILE_NAME), "it is yours, for all projects");

        AtomicReference<String> seen = new AtomicReference<>();
        var session = new ChatSession(ROSTER, (team, request, ui, cancelled, dispatcher) -> {
            seen.set(request.chat());
            return done("ana", "ok");
        });
        session.profile(() -> UserProfile.describe(settings));
        session.submit("hello", List.of(), "ana");
        awaitIdle(session);
        String chat = seen.get();
        assertTrue(chat.contains("is Dumi. Call them Dumi"), chat);
        assertTrue(chat.contains("small bakery") && chat.contains("Short answers, informal, in Portuguese."), chat);
        assertTrue(chat.indexOf("Dumi") < chat.indexOf("direct chat"), "who they are comes before where the chat is");

        seen.set(null);
        session.submit("hi again", List.of(), ChatSession.MAIN);
        awaitIdle(session);
        assertTrue(seen.get().contains("Dumi"), "in a group too");
    }

    @Test
    void aVeryLongProfileIsCutSoItCannotCrowdOutTheWork() {
        Settings settings = Settings.defaults();
        UserProfile.set(settings, Settings.PROFILE_ABOUT, "x".repeat(10_000));
        assertTrue(UserProfile.describe(settings).length() < 2000);
    }
}
