package dev.buildcli.infrastructure;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * The providers BuildCLI can talk to: a built-in list plus the user's own {@code providers.yaml} in the global
 * directory. Project files can NOT add providers: a provider is a URL that receives an API key, so it must be something
 * the user chose, not something a cloned repository dropped in.
 */
public final class ProviderRegistry {
    public static final String FILE_NAME = "providers.yaml";
    private static final long MAX_BYTES = 256L * 1024;
    private static final Pattern NAME = Pattern.compile("[a-z][a-z0-9-]{0,39}");
    private static final Pattern ENV = Pattern.compile("[A-Z_][A-Z0-9_]{0,63}");

    private final Map<String, ProviderSpec> specs;

    private ProviderRegistry(Map<String, ProviderSpec> specs) {
        this.specs = specs;
    }

    public static ProviderRegistry builtIn() {
        return new ProviderRegistry(new LinkedHashMap<>(builtIns()));
    }

    /** Built-ins, overridden and extended by {@code <globalDir>/providers.yaml} when it exists. */
    public static ProviderRegistry load(Path globalDir) {
        Map<String, ProviderSpec> all = new LinkedHashMap<>(builtIns());
        Path file = globalDir.resolve(FILE_NAME);
        if (Files.isRegularFile(file)) {
            for (ProviderSpec s : parse(file)) {
                all.put(s.name(), s);
            }
        }
        return new ProviderRegistry(all);
    }

    private static Map<String, ProviderSpec> builtIns() {
        Map<String, ProviderSpec> m = new LinkedHashMap<>();
        add(m, "ollama", ProviderSpec.Kind.OLLAMA, "http://localhost:11434", null, "local models through Ollama");
        add(m, "lmstudio", ProviderSpec.Kind.OPENAI_COMPATIBLE, "http://localhost:1234/v1", null, "local models through LM Studio");
        add(m, "openrouter", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://openrouter.ai/api/v1", "OPENROUTER_API_KEY",
                "hundreds of models, including free ones (try openrouter:openrouter/free)");
        add(m, "deepseek", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://api.deepseek.com/v1", "DEEPSEEK_API_KEY", "DeepSeek");
        add(m, "kimi", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://api.moonshot.ai/v1", "MOONSHOT_API_KEY", "Moonshot Kimi");
        add(m, "moonshot", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://api.moonshot.ai/v1", "MOONSHOT_API_KEY", "same as kimi");
        add(m, "openai", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://api.openai.com/v1", "OPENAI_API_KEY", "OpenAI");
        add(m, "groq", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://api.groq.com/openai/v1", "GROQ_API_KEY", "Groq");
        add(m, "mistral", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://api.mistral.ai/v1", "MISTRAL_API_KEY", "Mistral");
        add(m, "gemini", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://generativelanguage.googleapis.com/v1beta/openai",
                "GEMINI_API_KEY", "Google Gemini (OpenAI-compatible endpoint)");
        add(m, "together", ProviderSpec.Kind.OPENAI_COMPATIBLE, "https://api.together.xyz/v1", "TOGETHER_API_KEY", "Together AI");
        return m;
    }

    /** Where to create a key for a built-in provider, or null. */
    public static String keyPage(String provider) {
        return switch (provider) {
            case "openrouter" -> "https://openrouter.ai/keys";
            case "deepseek" -> "https://platform.deepseek.com/api_keys";
            case "kimi", "moonshot" -> "https://platform.moonshot.ai/console/api-keys";
            case "openai" -> "https://platform.openai.com/api-keys";
            case "groq" -> "https://console.groq.com/keys";
            case "mistral" -> "https://console.mistral.ai/api-keys";
            case "gemini" -> "https://aistudio.google.com/apikey";
            case "together" -> "https://api.together.ai/settings/api-keys";
            default -> null;
        };
    }

    private static void add(Map<String, ProviderSpec> m, String name, ProviderSpec.Kind kind, String url, String env, String description) {
        m.put(name, new ProviderSpec(name, kind, url, env, description));
    }

    public Optional<ProviderSpec> find(String name) {
        return Optional.ofNullable(specs.get(name));
    }

    public List<ProviderSpec> all() {
        return new ArrayList<>(specs.values());
    }

    /** Same registry with one provider's URL replaced (OLLAMA_HOST, --url, ...). */
    public ProviderRegistry withBaseUrl(String name, String baseUrl) {
        ProviderSpec s = specs.get(name);
        if (s == null || baseUrl == null) {
            return this;
        }
        Map<String, ProviderSpec> copy = new LinkedHashMap<>(specs);
        copy.put(name, new ProviderSpec(s.name(), s.kind(), baseUrl, s.apiKeyEnv(), s.description()));
        return new ProviderRegistry(copy);
    }

    /** Adds or replaces a provider in {@code <globalDir>/providers.yaml}, keeping the other entries. */
    public static void save(Path globalDir, ProviderSpec spec) throws IOException {
        validate(spec);
        Map<String, ProviderSpec> mine = new LinkedHashMap<>();
        Path file = globalDir.resolve(FILE_NAME);
        if (Files.isRegularFile(file)) {
            parse(file).forEach(s -> mine.put(s.name(), s));
        }
        mine.put(spec.name(), spec);
        ObjectMapper yaml = new ObjectMapper(new YAMLFactory());
        Map<String, Object> providers = new LinkedHashMap<>();
        for (ProviderSpec s : mine.values()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("kind", s.kind() == ProviderSpec.Kind.OLLAMA ? "ollama" : "openai-compatible");
            entry.put("baseUrl", s.baseUrl());
            if (s.apiKeyEnv() != null) {
                entry.put("apiKeyEnv", s.apiKeyEnv());
            }
            if (s.description() != null && !s.description().isBlank()) {
                entry.put("description", s.description());
            }
            providers.put(s.name(), entry);
        }
        Files.createDirectories(globalDir);
        yaml.writeValue(file.toFile(), Map.of("schema", 1, "providers", providers));
    }

    /** Removes a provider from {@code <globalDir>/providers.yaml}. Built-ins cannot be removed. @return false if it was not there */
    public static boolean remove(Path globalDir, String name) throws IOException {
        Path file = globalDir.resolve(FILE_NAME);
        if (!Files.isRegularFile(file)) {
            return false;
        }
        List<ProviderSpec> mine = new ArrayList<>(parse(file));
        if (!mine.removeIf(s -> s.name().equals(name))) {
            return false;
        }
        Files.delete(file);
        for (ProviderSpec s : mine) {
            save(globalDir, s);
        }
        return true;
    }

    /** True for providers that come with BuildCLI (a user file may still override their URL). */
    public static boolean isBuiltIn(String name) {
        return builtIns().containsKey(name);
    }

    /** The providers in the user's own file. */
    public static List<String> userDefined(Path globalDir) {
        Path file = globalDir.resolve(FILE_NAME);
        return Files.isRegularFile(file) ? parse(file).stream().map(ProviderSpec::name).toList() : List.of();
    }

    private static List<ProviderSpec> parse(Path file) {
        try {
            if (Files.size(file) > MAX_BYTES) {
                throw new IllegalArgumentException(file + " is larger than " + MAX_BYTES / 1024 + " KB");
            }
            JsonNode root = new ObjectMapper(new YAMLFactory()).readTree(Files.readString(file));
            List<ProviderSpec> out = new ArrayList<>();
            if (root == null || !root.path("providers").isObject()) {
                return out;
            }
            root.path("providers").fields().forEachRemaining(e -> {
                JsonNode n = e.getValue();
                String kind = n.path("kind").asText("openai-compatible");
                ProviderSpec.Kind k = switch (kind) {
                    case "ollama" -> ProviderSpec.Kind.OLLAMA;
                    case "openai-compatible", "openai" -> ProviderSpec.Kind.OPENAI_COMPATIBLE;
                    default -> throw new IllegalArgumentException(file + ": provider '" + e.getKey() + "' has unknown kind '" + kind
                            + "' (expected ollama or openai-compatible)");
                };
                ProviderSpec s = new ProviderSpec(e.getKey(), k, n.path("baseUrl").asText(""),
                        n.hasNonNull("apiKeyEnv") ? n.get("apiKeyEnv").asText() : null, n.path("description").asText(""));
                try {
                    validate(s);
                } catch (IllegalArgumentException ex) {
                    throw new IllegalArgumentException(file + ": " + ex.getMessage());
                }
                out.add(s);
            });
            return out;
        } catch (IOException e) {
            throw new IllegalArgumentException("cannot read " + file + ": " + e.getMessage());
        }
    }

    static void validate(ProviderSpec s) {
        if (!NAME.matcher(s.name()).matches()) {
            throw new IllegalArgumentException("provider name '" + s.name() + "' must be lowercase letters, digits and dashes");
        }
        if (!s.baseUrl().startsWith("http://") && !s.baseUrl().startsWith("https://")) {
            throw new IllegalArgumentException("provider '" + s.name() + "': baseUrl must start with http:// or https://");
        }
        if (s.apiKeyEnv() != null && !ENV.matcher(s.apiKeyEnv()).matches()) {
            throw new IllegalArgumentException("provider '" + s.name() + "': apiKeyEnv must be an environment variable name like MY_KEY; "
                    + "keys are read from the environment, never stored");
        }
    }
}
