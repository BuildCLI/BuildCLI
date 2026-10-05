package dev.buildcli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.ModelRef;
import dev.buildcli.domain.ModelRouting;
import dev.buildcli.domain.Permissions;
import dev.buildcli.infrastructure.LangChain4jGateway;
import dev.buildcli.infrastructure.ProviderRegistry;
import dev.buildcli.infrastructure.ProviderSettings;
import dev.buildcli.infrastructure.ProviderSpec;
import dev.buildcli.infrastructure.RoutingGateway;
import dev.buildcli.ports.LlmGateway;
import dev.buildcli.ports.LlmMessage;
import dev.buildcli.ports.LlmReply;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.output.TokenUsage;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class GatewayTest {
    static Agent agent(String name) {
        return new Agent(name, "r", "", Set.of(), Permissions.none());
    }

    static final List<LlmMessage> HELLO = List.of(new LlmMessage.User("hi"));

    static ChatResponse response(AiMessage ai) {
        return ChatResponse.builder().aiMessage(ai).tokenUsage(new TokenUsage(7, 3)).build();
    }

    // ---- streaming through the LangChain4j adapter ----

    @Test
    void streamingReportsTextAsItArrivesAndReturnsTheCompleteReply() {
        StreamingChatModel stream = new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onPartialResponse("Hel");
                handler.onPartialResponse("lo");
                handler.onCompleteResponse(response(AiMessage.from("Hello")));
            }
        };
        var gateway = new LangChain4jGateway(failingChat(), stream);
        List<String> deltas = new ArrayList<>();
        LlmReply reply = gateway.chatStreaming(agent("ana"), HELLO, List.of(), deltas::add);
        assertEquals(List.of("Hel", "lo"), deltas);
        assertEquals("Hello", reply.text());
        assertEquals(7, reply.inputTokens());
        assertEquals(3, reply.outputTokens());
    }

    @Test
    void streamingDeliversToolCallsWithParsedArguments() {
        var request = ToolExecutionRequest.builder().id("c1").name("write_file")
                .arguments("{\"path\":\"out/a.txt\",\"content\":\"hi\"}").build();
        StreamingChatModel stream = new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest r, StreamingChatResponseHandler handler) {
                handler.onCompleteResponse(response(AiMessage.from("", List.of(request))));
            }
        };
        LlmReply reply = new LangChain4jGateway(failingChat(), stream).chatStreaming(agent("bruno"), HELLO, List.of(), d -> { });
        assertEquals(1, reply.toolCalls().size());
        assertEquals("write_file", reply.toolCalls().get(0).name());
        assertEquals(Map.of("path", "out/a.txt", "content", "hi"), reply.toolCalls().get(0).args());
    }

    @Test
    void aStreamingErrorSurfacesAsAnException() {
        StreamingChatModel stream = new StreamingChatModel() {
            @Override
            public void doChat(ChatRequest request, StreamingChatResponseHandler handler) {
                handler.onError(new RuntimeException("connection refused"));
            }
        };
        var gateway = new LangChain4jGateway(failingChat(), stream);
        var ex = assertThrows(IllegalStateException.class,
                () -> gateway.chatStreaming(agent("ana"), HELLO, List.of(), d -> { }));
        assertEquals("connection refused", ex.getMessage());
    }

    @Test
    void withoutAStreamingModelTheWholeTextIsReportedAtTheEnd() {
        ChatModel chat = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                return response(AiMessage.from("whole answer"));
            }
        };
        List<String> deltas = new ArrayList<>();
        LlmReply reply = new LangChain4jGateway(chat).chatStreaming(agent("ana"), HELLO, List.of(), deltas::add);
        assertEquals(List.of("whole answer"), deltas);
        assertEquals("whole answer", reply.text());
    }

    @Test
    void malformedToolArgumentsFromTheModelAreAnErrorNotACrashOfTheRuntime() {
        var bad = ToolExecutionRequest.builder().id("c").name("write_file").arguments("{not json").build();
        ChatModel chat = new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                return response(AiMessage.from("", List.of(bad)));
            }
        };
        var ex = assertThrows(IllegalStateException.class, () -> new LangChain4jGateway(chat).chat(agent("a"), HELLO, List.of()));
        assertTrue(ex.getMessage().contains("malformed tool arguments"), ex.getMessage());
    }

    private static ChatModel failingChat() {
        return new ChatModel() {
            @Override
            public ChatResponse doChat(ChatRequest request) {
                throw new AssertionError("the non-streaming path must not be used");
            }
        };
    }

    // ---- routing ----

    @Test
    void eachAgentGoesToItsConfiguredModelWithTheTeamDefaultAsFallback() {
        Map<ModelRef, LlmGateway> built = new HashMap<>();
        List<ModelRef> factoryCalls = new ArrayList<>();
        var routing = new ModelRouting(new ModelRef("ollama", "small"), Map.of("ana", new ModelRef("openai", "big")));
        var gateway = new RoutingGateway(routing, null, ref -> {
            factoryCalls.add(ref);
            LlmGateway g = (a, m, t) -> new LlmReply(ref.provider() + "/" + ref.model(), List.of(), 0, 0);
            built.put(ref, g);
            return g;
        });
        assertEquals("openai/big", gateway.chat(agent("ana"), HELLO, List.of()).text());
        assertEquals("ollama/small", gateway.chat(agent("bruno"), HELLO, List.of()).text());
        assertEquals("ollama/small", gateway.chat(agent("carla"), HELLO, List.of()).text());
        assertEquals(2, factoryCalls.size(), "one gateway per provider/model pair, reused across agents and calls");
        assertSame(built.get(new ModelRef("ollama", "small")), built.get(new ModelRef("ollama", "small")));
    }

    @Test
    void theFallbackServesAgentsTheTeamDidNotConfigure() {
        var gateway = new RoutingGateway(ModelRouting.unspecified(), new ModelRef("ollama", "cli-choice"),
                ref -> (a, m, t) -> new LlmReply(ref.model(), List.of(), 0, 0));
        assertEquals("cli-choice", gateway.chat(agent("ana"), HELLO, List.of()).text());
    }

    @Test
    void aMissingModelIsAClearErrorNamingTheAgent() {
        var gateway = new RoutingGateway(ModelRouting.unspecified(), null, ref -> null);
        var ex = assertThrows(IllegalStateException.class, () -> gateway.chat(agent("ana"), HELLO, List.of()));
        assertTrue(ex.getMessage().contains("no model configured for agent 'ana'"), ex.getMessage());
        assertTrue(ex.getMessage().contains("/connect"), ex.getMessage());
    }

    @Test
    void routingKeepsStreamingWorking() {
        var gateway = new RoutingGateway(ModelRouting.unspecified(), new ModelRef("ollama", "m"),
                ref -> (a, m, t) -> new LlmReply("text", List.of(), 0, 0));
        List<String> deltas = new ArrayList<>();
        gateway.chatStreaming(agent("ana"), HELLO, List.of(), deltas::add);
        assertEquals(List.of("text"), deltas);
    }

    // ---- provider settings ----

    @Test
    void providerSettingsComeFromTheEnvironmentWithSafeDefaults() {
        var d = ProviderSettings.fromEnvironment(Map.of());
        assertEquals("http://localhost:11434", d.ollamaUrl());
        assertEquals("https://api.openai.com/v1", d.registry().find("openai").orElseThrow().baseUrl());
        var custom = ProviderSettings.fromEnvironment(Map.of("OLLAMA_HOST", "gpu-box:11434", "OPENAI_BASE_URL", "http://vllm:8000/v1"));
        assertEquals("http://gpu-box:11434", custom.ollamaUrl());
        assertEquals("http://vllm:8000/v1", custom.registry().find("openai").orElseThrow().baseUrl());
    }

    @Test
    void cloudProvidersNeedTheirKeyFromTheEnvironmentAndSayWhichOne() {
        for (String p : List.of("openrouter", "deepseek", "kimi", "moonshot", "openai", "groq")) {
            assertTrue(ProviderSettings.fromEnvironment(Map.of()).registry().find(p).isPresent(), p);
        }
        var ex = assertThrows(IllegalStateException.class,
                () -> ProviderSettings.fromEnvironment(Map.of()).gatewayFor(new ModelRef("openrouter", "openrouter/free")));
        assertTrue(ex.getMessage().contains("OPENROUTER_API_KEY"), ex.getMessage());
        assertNotNull(ProviderSettings.fromEnvironment(Map.of("OPENROUTER_API_KEY", "k")).gatewayFor(new ModelRef("openrouter", "openrouter/free")));
        assertNotNull(ProviderSettings.fromEnvironment(Map.of()).gatewayFor(new ModelRef("lmstudio", "x")), "local servers need no key");
    }

    @Test
    void userProvidersAreLoadedFromTheGlobalDirectoryAndValidated(@org.junit.jupiter.api.io.TempDir java.nio.file.Path dir) throws Exception {
        ProviderRegistry.save(dir, new ProviderSpec("my-vllm", ProviderSpec.Kind.OPENAI_COMPATIBLE, "http://gpu:8000/v1", "VLLM_KEY", ""));
        var settings = ProviderSettings.fromEnvironment(Map.of("VLLM_KEY", "k"), dir);
        assertEquals("http://gpu:8000/v1", settings.registry().find("my-vllm").orElseThrow().baseUrl());
        assertTrue(settings.registry().find("openrouter").isPresent(), "built-ins stay");
        assertNotNull(settings.gatewayFor(new ModelRef("my-vllm", "m")));
        assertThrows(IllegalArgumentException.class, () -> ProviderRegistry.save(dir,
                new ProviderSpec("bad", ProviderSpec.Kind.OPENAI_COMPATIBLE, "ftp://x", null, "")));
        assertThrows(IllegalArgumentException.class, () -> ProviderRegistry.save(dir,
                new ProviderSpec("bad", ProviderSpec.Kind.OPENAI_COMPATIBLE, "http://x", "sk-secret-value", "")),
                "a key pasted where the variable name goes must be refused, not stored");
    }

    @Test
    void anUnknownProviderIsRejected() {
        var ex = assertThrows(IllegalArgumentException.class,
                () -> ProviderSettings.fromEnvironment(Map.of()).gatewayFor(new ModelRef("mystery", "x")));
        assertTrue(ex.getMessage().contains("unknown provider 'mystery'"));
    }
}
