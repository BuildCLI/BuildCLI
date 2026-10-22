package dev.buildcli.cli;

import dev.buildcli.infrastructure.FileCredentialStore;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * The process environment plus the API keys saved from inside BuildCLI, as one read-only map, so everything that looks a
 * key up by its variable name (gateways, the model list, doctor) finds a saved key without knowing where it came from.
 * A variable that is set and not blank always wins over a saved key.
 */
final class KeyedEnvironment extends AbstractMap<String, String> {
    private final Map<String, String> process;
    private final FileCredentialStore saved;

    KeyedEnvironment(Map<String, String> process, FileCredentialStore saved) {
        this.process = process;
        this.saved = saved;
    }

    @Override
    public String get(Object name) {
        if (!(name instanceof String variable)) {
            return null;
        }
        String set = process.get(variable);
        if (set != null && !set.isBlank()) {
            return set;
        }
        return saved.get(variable).orElse(set);
    }

    @Override
    public boolean containsKey(Object name) {
        return get(name) != null;
    }

    @Override
    public Set<Entry<String, String>> entrySet() {
        Map<String, String> all = new TreeMap<>(process);
        for (String variable : saved.variables()) {
            if (get(variable) != null) {
                all.put(variable, get(variable));
            }
        }
        return java.util.Collections.unmodifiableMap(all).entrySet();
    }
}
