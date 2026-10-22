package dev.buildcli.domain;

import java.time.Instant;

/** One execution of a request, by an agent or through a group. {@code finishedAt} and {@code summary} are null while it runs. */
public record RunInfo(String id, String group, String request, Instant startedAt, Instant finishedAt, String status,
                      String summary) {}
