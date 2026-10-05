package dev.buildcli.infrastructure.tui;

import dev.buildcli.application.Settings;
import dev.buildcli.infrastructure.ModelCatalog;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** What the settings screen needs from the rest of the app: storage, providers, model lists and agent files. */
public interface SettingsServices {
    Settings settings();

    /** @param keyFrom where its key comes from: "environment", "saved" (typed into BuildCLI) or "" */
    record Provider(String name, String url, String keyEnv, boolean keySet, boolean removable, String description, String keyFrom) {
        public Provider(String name, String url, String keyEnv, boolean keySet, boolean removable, String description) {
            this(name, url, keyEnv, keySet, removable, description, keySet ? "environment" : "");
        }

        public Provider(String name, String url, String keyEnv, boolean keySet, boolean removable) {
            this(name, url, keyEnv, keySet, removable, "");
        }

        /** Runs on this machine, so it needs no key but must be started. */
        public boolean local() {
            return keyEnv == null && (url.contains("://localhost") || url.contains("://127.0.0.1"));
        }
    }

    List<Provider> providers();

    void addProvider(String name, String url, String keyEnv) throws Exception;

    void removeProvider(String name) throws Exception;

    /** One line: "ok 1200 ms: pong" or the reason it failed. */
    CompletableFuture<String> test(String model);

    CompletableFuture<ModelCatalog.Result> models(String provider);

    /** Saves the API key of a provider in the user's own BuildCLI folder, so no variable has to be set before starting. */
    default void saveKey(String provider, String key) throws Exception {
        throw new IllegalStateException("keys cannot be saved here");
    }

    /**
     * Asks the provider for its models with this key, without saving it, so a wrong key is caught before it is kept. Providers
     * that list their models without a key (OpenRouter) accept any key here; the test message later is the real check.
     */
    default CompletableFuture<ModelCatalog.Result> checkKey(String provider, String key) {
        return CompletableFuture.completedFuture(new ModelCatalog.Result(List.of(), null));
    }

    /** Deletes a key saved with {@link #saveKey}. A variable set in the environment is not touched. @return false if none was saved */
    default boolean forgetKey(String provider) throws Exception {
        return false;
    }

    /** Where saved keys are kept, to tell the user. */
    default String keyFile() {
        return "";
    }

    /** Forgets the model lists, so the next {@link #models} asks the providers again (after starting Ollama, say). */
    default void refreshModels() { }

    /** False where nothing can be connected (tests, the demo): the chat then never opens the connect screen by itself. */
    default boolean canConnect() {
        return !providers().isEmpty();
    }

    record AgentInfo(String name, String role, String origin, String file, List<String> capabilities) {}

    List<AgentInfo> agents();

    /** @return where the agent file was written */
    String createAgent(String name, String role, String instructions, List<String> capabilities, boolean global) throws Exception;

    void deleteAgent(String name) throws Exception;

    /** Changes an existing agent's file (and the agent in the open chat). Only what {@code edit} sets is changed. */
    default void updateAgent(String name, dev.buildcli.infrastructure.AgentFileEditor.Edit edit) throws Exception {
        throw new IllegalStateException("not available here");
    }

    /**
     * Writes the four sample agents (the maintainers) and AGENTS.md into the project, puts the agents in the open
     * chat and makes a group of them. Files that exist are kept. @return the names of the agents that were added
     */
    default List<String> createSampleAgents() throws Exception {
        throw new IllegalStateException("not available here");
    }

    /** The same for another sample team: {@code dev}, {@code writing} or {@code office}. */
    default List<String> createSampleAgents(String team) throws Exception {
        return createSampleAgents();
    }

    /** The sample teams on offer, as "key: what is in it" lines. */
    default List<String> sampleTeams() {
        return List.of();
    }
}
