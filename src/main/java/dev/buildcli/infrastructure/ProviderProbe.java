package dev.buildcli.infrastructure;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.ModelRef;
import dev.buildcli.domain.Permissions;
import dev.buildcli.ports.LlmGateway;
import dev.buildcli.ports.LlmMessage;
import dev.buildcli.ports.LlmReply;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/** Sends one tiny request to check a key, a URL and a model name, for {@code provider test} and the settings screen. */
public final class ProviderProbe {
    private ProviderProbe() {}

    public record Outcome(boolean ok, long millis, String text) {}

    public static Outcome test(ModelRef ref, Function<ModelRef, LlmGateway> gateways) {
        long start = System.nanoTime();
        try {
            LlmReply reply = gateways.apply(ref).chat(new Agent("probe", "probe", "", Set.of(), Permissions.none()),
                    List.of(new LlmMessage.User("Reply with the single word: pong")), List.of());
            long ms = (System.nanoTime() - start) / 1_000_000;
            return new Outcome(true, ms, reply.text() == null ? "(empty reply)" : reply.text().strip());
        } catch (RuntimeException e) {
            return new Outcome(false, (System.nanoTime() - start) / 1_000_000, ProviderErrors.message(e));
        }
    }
}
