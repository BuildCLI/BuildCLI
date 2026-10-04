package dev.buildcli.infrastructure.tui;

import static dev.buildcli.application.I18n.t;
import dev.buildcli.infrastructure.ModelCatalog;
import dev.buildcli.infrastructure.ProviderRegistry;
import dev.tamboui.style.Color;
import java.util.ArrayList;
import java.util.List;

/** What a provider still needs before it can be used, in one short line and a few lines of help. */
record ProviderStatus(String text, Color color, boolean ready, List<String> help) {
    /** @param r what asking the provider for its models returned, or null while that is still going on */
    static ProviderStatus of(SettingsServices.Provider p, ModelCatalog.Result r) {
        if (p.keyEnv() != null && !p.keySet()) {
            return new ProviderStatus(t("needs a key"), Theme.AMBER, false, keyHelp(p));
        }
        if (r == null) {
            return new ProviderStatus(t("checking…"), Theme.DIM, false, List.of(t("Checking {0} …", p.url())));
        }
        if (r.problem() != null) {
            if (p.local()) {
                return new ProviderStatus(t("not running"), Theme.DIM, false, localHelp(p));
            }
            boolean rejected = r.problem().startsWith("HTTP 401") || r.problem().startsWith("HTTP 403");
            if (rejected && p.keyEnv() != null) {
                return new ProviderStatus(t("key rejected"), Theme.RED, false, List.of(r.problem(), "",
                        p.keyFrom().equals("environment") ? t("The key comes from the environment variable {0}. Fix it there, or press K to save a different key here (a variable that is set wins, so unset it first).", p.keyEnv())
                                : t("Press K to enter a new key.")));
            }
            return new ProviderStatus(p.keyEnv() == null ? t("not reachable") : t("key or endpoint failed"), Theme.RED, false,
                    List.of(r.problem(), "", p.keyEnv() == null ? t("Check the URL {0}, then press R to check again.", p.url())
                            : t("Check the URL {0} and the key (press K to enter a new one), then press R to check again.", p.url())));
        }
        if (r.models().isEmpty()) {
            return new ProviderStatus(t("no models yet"), Theme.AMBER, false, p.name().equals("ollama")
                    ? List.of(t("Ollama is running but has no models. Download one in a terminal:"), "",
                            "    ollama pull qwen2.5-coder:7b", "", t("then press R. Models of 7B or more work much better with tools."))
                    : List.of(t("{0} answered but lists no models. Press Enter to type a model name yourself.", p.name())));
        }
        long free = r.models().stream().filter(ModelCatalog.Model::free).count();
        String count = (r.models().size() == 1 ? t("1 model") : t("{0} models", r.models().size())) + (p.local() || free == 0 ? "" : ", " + t("{0} free", free));
        return new ProviderStatus(t("ready") + " · " + count, Theme.GREEN, true, List.of((p.description().isBlank() ? p.name() : p.description())
                + ". " + t("Press Enter to choose a model.")));
    }

    private static List<String> keyHelp(SettingsServices.Provider p) {
        List<String> help = new ArrayList<>();
        String page = ProviderRegistry.keyPage(p.name());
        if (!p.description().isBlank()) {
            help.add(p.description() + ".");
            help.add("");
        }
        help.add("1. " + (page != null ? t("Create a key at {0}", page) : t("Get a key from the provider")));
        help.add("2. " + t("Press Enter here and paste it. It is saved only on this computer, readable by your account only."));
        help.add("");
        help.add(t("Prefer an environment variable? Set {0} before starting BuildCLI; it always wins over a saved key.", p.keyEnv()));
        return help;
    }

    private static List<String> localHelp(SettingsServices.Provider p) {
        return switch (p.name()) {
            case "ollama" -> List.of(t("Ollama runs models on this machine, free and private."), "",
                    "1. " + t("Install it from {0}", "https://ollama.com"), "2. " + t("Start it:") + "  ollama serve", "3. " + t("Download a model:") + "  ollama pull qwen2.5-coder:7b",
                    "4. " + t("Press R to check again."));
            case "lmstudio" -> List.of(t("LM Studio runs models on this machine, free and private."), "",
                    "1. " + t("Install it from {0} and download a model", "https://lmstudio.ai"), "2. " + t("Start its server: Developer tab › Start server"),
                    "3. " + t("Press R to check again."));
            default -> List.of(t("Nothing answers at {0}. Start the server, then press R to check again.", p.url()));
        };
    }
}
