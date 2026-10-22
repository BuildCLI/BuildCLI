package dev.buildcli.domain;

/** Where a definition came from. Definitions from a cloned project are untrusted until the user approves them. */
public enum Origin { BUILTIN, GLOBAL, PROJECT }
