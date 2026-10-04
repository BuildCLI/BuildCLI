package dev.buildcli.application;

import dev.buildcli.domain.Agent;
import dev.buildcli.domain.Origin;
import dev.buildcli.domain.Roster;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.ConfigRepository;
import dev.buildcli.ports.TrustStore;
import dev.buildcli.ports.UserInterface;
import java.util.List;

/**
 * Agents defined by a project are untrusted until the user approves them: cloning a repository must never be enough
 * to hand a model write access or a command allow list. The user approves an exact digest of the project's definition
 * files; changing any of them asks again. Definitions from the user's own global directory need no approval.
 */
public final class TrustGate {
    private TrustGate() {}

    /** @return true if the agents may run; false if the user declined to trust the project's definitions */
    public static boolean ensureTrusted(Roster roster, ConfigRepository config, TrustStore store, String projectKey, UserInterface ui) {
        List<Agent> fromProject = roster.agents().stream().filter(a -> a.origin() == Origin.PROJECT).toList();
        String digest = config.projectDigest();
        if (fromProject.isEmpty() || store.isTrusted(projectKey, digest)) {
            return true;
        }
        StringBuilder detail = new StringBuilder("These definitions come from the project, not from you. Review what they may do:\n");
        for (Agent a : fromProject) {
            detail.append("\n").append(a.name()).append(" (").append(a.role()).append(")  ").append(a.source())
                    .append("\n  capabilities: ").append(a.capabilities())
                    .append("\n  may read:     ").append(a.permissions().readGlobs())
                    .append("\n  may write:    ").append(a.permissions().writeGlobs())
                    .append("\n  may run:      ").append(a.permissions().commandAllow()).append("\n");
        }
        boolean granted = ui.approve(new ApprovalRequest("system", "trust",
                "Trust " + (roster.agents().size() == 1 ? "this agent" : "these agents") + " from the project?", detail.toString()));
        if (granted) {
            store.trust(projectKey, digest);
        }
        return granted;
    }
}
