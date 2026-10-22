package dev.buildcli.ports;

import java.util.Map;

/** Where settings live: one file for every project on this machine, one for the current project. Never the project tree. */
public interface SettingsStore {
    enum Scope { GLOBAL, PROJECT }

    Map<String, String> load(Scope scope);

    void save(Scope scope, Map<String, String> values);
}
