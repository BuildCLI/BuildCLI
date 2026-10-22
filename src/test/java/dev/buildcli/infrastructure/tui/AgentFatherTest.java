package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.application.ChatSession;
import dev.buildcli.application.Settings;
import dev.buildcli.domain.Limits;
import dev.buildcli.infrastructure.ModelCatalog;
import dev.tamboui.tui.event.KeyCode;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

/** AgentFather: the built-in contact that creates and manages agents by conversation, with no model. */
class AgentFatherTest {
    final ChatScreenTest base = new ChatScreenTest();
    final Settings settings = Settings.defaults();
    final List<String> created = new ArrayList<>();
    final List<String> deleted = new ArrayList<>();
    final List<String> edited = new ArrayList<>();
    final List<String> sampled = new ArrayList<>();

    SettingsServices services(ChatSession session) {
        return new SettingsServices() {
            @Override
            public Settings settings() {
                return settings;
            }

            @Override
            public List<Provider> providers() {
                return List.of();
            }

            @Override
            public void addProvider(String name, String url, String keyEnv) { }

            @Override
            public void removeProvider(String name) { }

            @Override
            public CompletableFuture<String> test(String model) {
                return CompletableFuture.completedFuture("ok");
            }

            @Override
            public CompletableFuture<ModelCatalog.Result> models(String provider) {
                return CompletableFuture.completedFuture(new ModelCatalog.Result(List.of(), null));
            }

            @Override
            public List<AgentInfo> agents() {
                return List.of();
            }

            @Override
            public String createAgent(String name, String role, String instructions, List<String> capabilities, boolean global) {
                created.add(name + "|" + role + "|" + instructions + "|" + String.join(",", capabilities) + "|" + global);
                return "/project/.buildcli/agents/" + name + ".md";
            }

            @Override
            public void deleteAgent(String name) {
                deleted.add(name);
            }

            @Override
            public List<String> sampleTeams() {
                return List.of("dev: Software team (...)", "writing: Writing desk (...)");
            }

            @Override
            public List<String> createSampleAgents(String team) {
                sampled.add(team);
                return List.of("writer", "editor");
            }

            @Override
            public void updateAgent(String name, dev.buildcli.infrastructure.AgentFileEditor.Edit edit) {
                edited.add(name + "|" + edit);
            }
        };
    }

    ChatSession session() {
        return new ChatSession(ChatScreenTest.ROSTER, (roster, request, ui, cancelled, dispatcher) -> {
            throw new AssertionError("AgentFather uses no model, and no agent should run here");
        });
    }

    ChatScreen screen(ChatSession session) {
        base.dir = Path.of(System.getProperty("java.io.tmpdir"));
        return new ChatScreen(session, Map.of(), base.dir, () -> { }, services(session));
    }

    static void say(ChatScreen screen, String text) {
        ChatScreenTest.type(screen, text);
        ChatScreenTest.key(screen, KeyCode.ENTER);
    }

    static void openFather(ChatScreen screen) {
        ChatScreenTest.render(screen, 130, 40);
        ChatListSearchTest.ctrlK(screen);
        ChatScreenTest.type(screen, "father");
        ChatScreenTest.key(screen, KeyCode.ENTER);
    }

    @Test
    void greetsOnceAndCreatesAnAgentStepByStepOnlyAfterYes() {
        ChatSession session = session();
        ChatScreen screen = screen(session);
        openFather(screen);
        assertEquals(ChatSession.FATHER, screen.selectedForTest());
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("Hi, I'm AgentFather"), out);
        assertTrue(out.contains("Ask AgentFather: /newagent"), "its chat has its own hint:\n" + out);

        say(screen, "/newagent");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("What should the new agent be called?"));
        say(screen, "Rita!");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("lowercase letters, digits"), "a bad name is explained and asked again");
        say(screen, "rita");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("What is rita's role?"));
        say(screen, "reviewer");
        out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("What may rita do?") && out.contains("4. filesystem.read") && out.contains("create and change files"), out);
        say(screen, "4,8");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("how should rita work?"));
        say(screen, "reviews pull requests");
        out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("I will create rita (reviewer)") && out.contains("filesystem.read, search"), out);
        assertTrue(created.isEmpty(), "nothing is created before yes");

        say(screen, "yes");
        assertEquals(List.of("rita|reviewer|reviews pull requests|filesystem.read,search|false"), created, "created in this project, not globally");
        out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("Created rita"), out);

        // opening the chat again does not greet again
        ChatListSearchTest.ctrlK(screen);
        ChatScreenTest.type(screen, "father");
        ChatScreenTest.key(screen, KeyCode.ENTER);
        assertEquals(1, session.messages().stream().filter(m -> m.text().startsWith("Hi, I'm AgentFather")).count());
    }

    @Test
    void backGoesOneQuestionBackAndCancelChangesNothing() {
        ChatSession session = session();
        ChatScreen screen = screen(session);
        openFather(screen);
        say(screen, "/newagent bob");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("What is bob's role?"), "a name given with the command skips the first question");
        say(screen, "back");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("What should the new agent be called?"));
        say(screen, "cancel");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("nothing was changed"));
        say(screen, "bob");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("I only follow commands"), "free text with no question open is not an answer");
        assertTrue(created.isEmpty());
    }

    @Test
    void anExistingNameIsRefusedAndAgentsAreListedAndDeletedOnlyAfterYes() {
        ChatSession session = session();
        ChatScreen screen = screen(session);
        openFather(screen);
        say(screen, "/newagent ana");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("There is already an agent called ana"));
        say(screen, "/cancel");

        say(screen, "/agents");
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("2 agents") && out.contains("ana · architect") && out.contains("bruno · developer"), out);
        assertTrue(out.contains("no model yet"), out);

        say(screen, "/deleteagent bruno");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("Delete bruno?"));
        say(screen, "no");
        assertTrue(deleted.isEmpty());
        say(screen, "/deleteagent bruno");
        say(screen, "yes");
        assertEquals(List.of("bruno"), deleted);
        assertFalse(ChatScreenTest.render(screen, 130, 40).contains("could not"));
    }

    @Test
    void anEmptyChatOffersToTalkToAgentFather() {
        var session = new ChatSession(List.of(), List.of(), Limits.defaults(), (roster, request, ui, cancelled, d) -> null,
                dev.buildcli.ports.ChatStore.NONE, () -> 6, dev.buildcli.ports.ChatLog.NONE);
        ChatScreen screen = screen(session);
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("No agents yet") && out.contains("Talk to AgentFather"), out);
        assertTrue(out.contains("AgentFather"), "it is in the chat list from the start");
    }

    @Test
    void editAgentChangesOneSettingAtATimeAndOnlyAfterYes() {
        ChatSession session = session();
        session.addContact(new dev.buildcli.domain.Agent("rita", "reviewer", "", java.util.Set.of("filesystem.read"),
                dev.buildcli.domain.Permissions.none(), dev.buildcli.domain.Origin.PROJECT, "/p/.buildcli/agents/rita.md"));
        ChatScreen screen = screen(session);
        openFather(screen);

        say(screen, "/editagent rita");
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("role: reviewer") && out.contains("writes in: nowhere") && out.contains("What do you want to change?"), out);

        say(screen, "4");
        say(screen, "../secrets/**");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("is not a folder pattern inside the project"), "a bad folder is refused with a reason");
        say(screen, "src/**, docs/**");
        out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("it may write in src/**, docs/**"), out);
        assertTrue(edited.isEmpty(), "nothing changes before yes");
        say(screen, "yes");
        assertEquals(1, edited.size());
        assertTrue(edited.get(0).startsWith("rita|") && edited.get(0).contains("writeGlobs=[src/**, docs/**]"), edited.get(0));
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("What do you want to change?"), "back at the menu for the next change");

        say(screen, "5");
        say(screen, "mvn -q test; mvn -q verify");
        out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("WITHOUT asking you: mvn -q test;"), out);
        say(screen, "no");
        assertEquals(1, edited.size(), "no changes nothing");
        say(screen, "done");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("as you left it"));
    }

    @Test
    void editAgentRefusesAnAgentWithoutAMarkdownFile() {
        ChatSession session = session();
        ChatScreen screen = screen(session);
        openFather(screen);
        say(screen, "/editagent ana");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("cannot edit it"));
        say(screen, "/editagent nobody");
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("There is no agent called nobody"));
    }

    @Test
    void samplesOffersTheTeamsAndAddsTheOneYouName() {
        ChatScreen screen = screen(session());
        openFather(screen);
        say(screen, "/samples");
        String out = ChatScreenTest.render(screen, 130, 40);
        assertTrue(out.contains("Which team do you want?") && out.contains("writing: Writing desk"), out);
        say(screen, "writing");
        assertEquals(List.of("writing"), sampled);
        assertTrue(ChatScreenTest.render(screen, 130, 40).contains("Added writer, editor"));
        say(screen, "/samples dev");
        assertEquals(List.of("writing", "dev"), sampled, "a team named with the command is added at once");
    }

    @Test
    void agentFatherSpeaksPortugueseAndUnderstandsSimNaoEnglishAside() {
        settings.set(dev.buildcli.ports.SettingsStore.Scope.GLOBAL, Settings.LANGUAGE, "pt");
        try {
            ChatSession session = session();
            session.addContact(new dev.buildcli.domain.Agent("rita", "revisora", "", java.util.Set.of("filesystem.read"),
                    dev.buildcli.domain.Permissions.none(), dev.buildcli.domain.Origin.PROJECT, "/p/.buildcli/agents/rita.md"));
            ChatScreen screen = screen(session);
            openFather(screen);
            String out = ChatScreenTest.render(screen, 130, 40);
            assertTrue(out.contains("Olá, sou o AgentFather"), out);
            say(screen, "/editagent rita");
            out = ChatScreenTest.render(screen, 130, 40);
            assertTrue(out.contains("agora:") && out.contains("O que queres mudar?"), out);
            say(screen, "4");
            assertTrue(ChatScreenTest.render(screen, 130, 40).contains("Em que pastas pode rita escrever?"));
            say(screen, "src/**");
            say(screen, "sim");
            assertEquals(1, edited.size());
            say(screen, "feito");
            assertTrue(ChatScreenTest.render(screen, 130, 40).contains("como o deixaste"));
        } finally {
            dev.buildcli.application.I18n.use("en");
        }
    }
}
