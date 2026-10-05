package dev.buildcli.infrastructure;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.buildcli.domain.Agent;
import dev.buildcli.ports.LlmGateway;
import dev.buildcli.ports.LlmMessage;
import dev.buildcli.ports.LlmReply;
import dev.buildcli.ports.ToolCall;
import dev.buildcli.ports.ToolSpec;
import dev.langchain4j.agent.tool.ToolExecutionRequest;
import dev.langchain4j.agent.tool.ToolSpecification;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.ToolExecutionResultMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.StreamingChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.chat.response.StreamingChatResponseHandler;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaChatRequestParameters;
import dev.langchain4j.model.ollama.OllamaStreamingChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * The only class that knows LangChain4j's chat API. Everything above it uses the neutral port types.
 * Any LangChain4j {@link ChatModel} can sit behind it; see the factories for Ollama and OpenAI-compatible.
 */
public final class LangChain4jGateway implements LlmGateway {
    private static final ObjectMapper JSON = new ObjectMapper();

    private static final Duration TIMEOUT = Duration.ofMinutes(2);

    private final ChatModel model;
    private final StreamingChatModel streamingModel;

    public LangChain4jGateway(ChatModel model) {
        this(model, null);
    }

    /** @param streamingModel may be null: then {@link #chatStreaming} reports the text only when it is complete */
    public LangChain4jGateway(ChatModel model, StreamingChatModel streamingModel) {
        this.model = model;
        this.streamingModel = streamingModel;
    }

    /** Local Ollama. {@code threads} is Ollama's num_thread (its 16-thread default was ~50x slower on the spike box). */
    public static LangChain4jGateway ollama(String baseUrl, String modelName, int threads, double temperature, boolean streaming) {
        var params = OllamaChatRequestParameters.builder()
                .modelName(modelName)
                .temperature(temperature)
                .maxOutputTokens(512)
                .numThread(threads)
                .build();
        ChatModel chat = OllamaChatModel.builder().baseUrl(baseUrl).defaultRequestParameters(params).timeout(TIMEOUT).build();
        StreamingChatModel stream = streaming
                ? OllamaStreamingChatModel.builder().baseUrl(baseUrl).defaultRequestParameters(params).timeout(TIMEOUT).build()
                : null;
        return new LangChain4jGateway(chat, stream);
    }

    /** Any OpenAI-compatible endpoint (OpenAI, vLLM, LM Studio, Ollama's /v1, ...). The key comes from the caller. */
    public static LangChain4jGateway openAiCompatible(String baseUrl, String apiKey, String modelName, double temperature,
            boolean streaming, int maxOutputTokens) {
        ChatModel chat = OpenAiChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(modelName)
                .temperature(temperature).maxCompletionTokens(maxOutputTokens).timeout(TIMEOUT).build();
        StreamingChatModel stream = streaming
                ? OpenAiStreamingChatModel.builder().baseUrl(baseUrl).apiKey(apiKey).modelName(modelName)
                        .temperature(temperature).maxCompletionTokens(maxOutputTokens).timeout(TIMEOUT).build()
                : null;
        return new LangChain4jGateway(chat, stream);
    }

    /** Waits before each retry of a rate-limited or unavailable provider: 3 tries in total, about 10 s at most. */
    static final long[] BACKOFF_MS = {2_000, 6_000};

    @Override
    public LlmReply chat(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools) {
        for (int attempt = 0; ; attempt++) {
            try {
                return toReply(model.chat(request(messages, tools)));
            } catch (RuntimeException e) {
                if (attempt >= BACKOFF_MS.length || !ProviderErrors.retryable(e)) {
                    throw new IllegalStateException(ProviderErrors.message(e), e);
                }
                pause(BACKOFF_MS[attempt]);
            }
        }
    }

    private static void pause(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting to retry", e);
        }
    }

    @Override
    public LlmReply chatStreaming(Agent agent, List<LlmMessage> messages, List<ToolSpec> tools, Consumer<String> onText) {
        if (streamingModel == null) {
            return LlmGateway.super.chatStreaming(agent, messages, tools, onText);
        }
        for (int attempt = 0; ; attempt++) {
            boolean[] spoke = {false};
            try {
                return streamOnce(messages, tools, delta -> {
                    spoke[0] = true;
                    onText.accept(delta);
                });
            } catch (RuntimeException e) {
                // once text has been shown, a retry would repeat it: only retry failures before the first word
                if (spoke[0] || attempt >= BACKOFF_MS.length || !ProviderErrors.retryable(e)) {
                    throw e instanceof IllegalStateException && e.getCause() == null ? e : new IllegalStateException(ProviderErrors.message(e), e);
                }
                pause(BACKOFF_MS[attempt]);
            }
        }
    }

    private LlmReply streamOnce(List<LlmMessage> messages, List<ToolSpec> tools, Consumer<String> onText) {
        CompletableFuture<ChatResponse> done = new CompletableFuture<>();
        streamingModel.chat(request(messages, tools), new StreamingChatResponseHandler() {
            @Override
            public void onPartialResponse(String partial) {
                onText.accept(partial);
            }

            @Override
            public void onCompleteResponse(ChatResponse response) {
                done.complete(response);
            }

            @Override
            public void onError(Throwable error) {
                done.completeExceptionally(error);
            }
        });
        try {
            return toReply(done.get(TIMEOUT.toMillis(), TimeUnit.MILLISECONDS));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting for the model", e);
        } catch (ExecutionException e) {
            Throwable cause = e.getCause() == null ? e : e.getCause();
            throw new IllegalStateException(ProviderErrors.message(cause), cause);
        } catch (TimeoutException e) {
            throw new IllegalStateException("request timed out", e);
        }
    }

    private static ChatRequest request(List<LlmMessage> messages, List<ToolSpec> tools) {
        ChatRequest.Builder request = ChatRequest.builder().messages(messages.stream().map(LangChain4jGateway::toLc).toList());
        if (!tools.isEmpty()) {
            request.toolSpecifications(tools.stream().map(LangChain4jGateway::toLc).toList());
        }
        return request.build();
    }

    private static LlmReply toReply(ChatResponse response) {
        AiMessage ai = response.aiMessage();
        List<ToolCall> calls = new ArrayList<>();
        if (ai.hasToolExecutionRequests()) {
            for (ToolExecutionRequest r : ai.toolExecutionRequests()) {
                calls.add(new ToolCall(r.id() == null ? UUID.randomUUID().toString() : r.id(), r.name(), parseArgs(r.arguments())));
            }
        }
        var usage = response.tokenUsage();
        return new LlmReply(ai.text(), calls,
                usage == null || usage.inputTokenCount() == null ? 0 : usage.inputTokenCount(),
                usage == null || usage.outputTokenCount() == null ? 0 : usage.outputTokenCount());
    }

    private static Map<String, Object> parseArgs(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return JSON.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalStateException("model returned malformed tool arguments: " + json);
        }
    }

    private static ChatMessage toLc(LlmMessage m) {
        return switch (m) {
            case LlmMessage.System s -> SystemMessage.from(s.text());
            case LlmMessage.User u -> u.attachments().isEmpty() ? UserMessage.from(u.text()) : UserMessage.from(withAttachments(u));
            case LlmMessage.Assistant a -> a.toolCalls().isEmpty()
                    ? AiMessage.from(a.text() == null ? "" : a.text())
                    : AiMessage.from(a.text() == null ? "" : a.text(), a.toolCalls().stream()
                            .map(c -> ToolExecutionRequest.builder().id(c.id()).name(c.name()).arguments(toJson(c.args())).build())
                            .toList());
            case LlmMessage.ToolResult r -> ToolExecutionResultMessage.from(r.callId(), r.toolName(), r.text());
        };
    }

    private static List<dev.langchain4j.data.message.Content> withAttachments(LlmMessage.User u) {
        List<dev.langchain4j.data.message.Content> parts = new ArrayList<>();
        parts.add(dev.langchain4j.data.message.TextContent.from(u.text()));
        for (var a : u.attachments()) {
            String base64;
            try {
                base64 = java.util.Base64.getEncoder().encodeToString(java.nio.file.Files.readAllBytes(a.path()));
            } catch (java.io.IOException e) {
                throw new IllegalStateException("cannot read attachment " + a.name() + ": " + e.getMessage(), e);
            }
            parts.add(a.kind() == dev.buildcli.domain.Attachment.Kind.IMAGE
                    ? dev.langchain4j.data.message.ImageContent.from(base64, a.mime())
                    : dev.langchain4j.data.message.AudioContent.from(base64, a.mime()));
        }
        return parts;
    }

    private static ToolSpecification toLc(ToolSpec spec) {
        JsonObjectSchema.Builder schema = JsonObjectSchema.builder();
        for (ToolSpec.Param p : spec.params()) {
            if (p.array()) {
                schema.addProperty(p.name(), JsonArraySchema.builder()
                        .items(JsonStringSchema.builder().build()).description(p.description()).build());
            } else {
                schema.addStringProperty(p.name(), p.description());
            }
        }
        schema.required(spec.params().stream().filter(ToolSpec.Param::required).map(ToolSpec.Param::name).toList());
        return ToolSpecification.builder().name(spec.name()).description(spec.description()).parameters(schema.build()).build();
    }

    private static String toJson(Map<String, Object> args) {
        try {
            return JSON.writeValueAsString(args);
        } catch (Exception e) {
            return "{}";
        }
    }
}
