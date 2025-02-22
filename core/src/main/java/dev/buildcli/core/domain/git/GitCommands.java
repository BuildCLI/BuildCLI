package dev.buildcli.core.domain.git;

public final class GitCommands {
    public static final String GIT = "git";
    public static final String INIT = "init";
    public static final String PULL = "pull";
    public static final String ORIGIN = "origin";
    public static final String MAIN = "main";
    public static final String FETCH = "fetch";
    public static final String STATUS = "status";
    public static final String DIFF = "diff";
    public static final String LOG = "log";
    public static final String COMMIT = "commit";
    public static final String CHECKOUT = "checkout";
    public static final String MERGE = "merge";
    public static final String REBASE = "rebase";
    public static final String CLONE = "clone";
    public static final String BRANCH = "branch";
    public static final String ADD = "add";
    public static final String RM = "rm";
    public static final String RESET = "reset";
    public static final String STASH = "stash";
    public static final String TAG = "tag";
    public static final String PUSH = "push";

    public static final String PUSH_U = "push -u";
    public static final String CHECKOUT_B = "checkout -b";
    public static final String ORIGIN_MAIN = "origin/main";

    public static final String FORMAT_CONTRIBUTORS = "--format='- %aN <%aE>'";//
    public static final String SORT_UNIQUE= "| sort | uniq";
    public static final String EXIT_CODE = "--exit-code";
    public static final String REV_PARSE = "rev-parse";
    public static final String ONELINE = "--oneline";

    public static final String REV_LIST = "rev-list";
    public static final String COUNT = "--count";
    public static final String RANGE = "..";
    public static final String HEAD = "HEAD";


    private GitCommands() {
    }
}
