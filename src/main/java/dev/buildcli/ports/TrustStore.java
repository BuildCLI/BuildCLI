package dev.buildcli.ports;

/**
 * Remembers which project definitions the user has approved. A project is trusted for one exact digest of its agent and
 * agent files: any change to them makes it untrusted again until the user re-approves.
 */
public interface TrustStore {
    boolean isTrusted(String projectKey, String digest);

    void trust(String projectKey, String digest);
}
