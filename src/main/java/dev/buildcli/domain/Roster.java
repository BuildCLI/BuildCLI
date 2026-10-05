package dev.buildcli.domain;

import java.util.List;
import java.util.Optional;

/**
 * Who takes part in one run: the agent that answers ({@code lead}), the others it can reach, the limits and which model serves
 * whom. It is made for a conversation (a group, or a direct chat) and thrown away after it; it is not something users define.
 */
public record Roster(String name, String lead, List<Agent> agents, Limits limits, ModelRouting routing) {

    public Roster(String name, String lead, List<Agent> agents, Limits limits) {
        this(name, lead, agents, limits, ModelRouting.unspecified());
    }

    public Optional<Agent> agent(String name) {
        return agents.stream().filter(a -> a.name().equalsIgnoreCase(name)).findFirst();
    }
}
