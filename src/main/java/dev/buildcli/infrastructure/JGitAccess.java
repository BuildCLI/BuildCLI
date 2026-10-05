package dev.buildcli.infrastructure;

import dev.buildcli.ports.GitAccess;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.StatusCommand;
import org.eclipse.jgit.api.errors.AbortedByHookException;
import org.eclipse.jgit.api.errors.EmptyCommitException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.JGitInternalException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

/**
 * Git through JGit, in the process: no {@code git} program on the PATH, no repository-defined program run for a diff
 * (there is no external diff or textconv to switch off), and no options for a caller to smuggle in. The only programs
 * that can run are the repository's own commit hooks, as for the user.
 */
public final class JGitAccess implements GitAccess {
    private static final int ABBREVIATION = 7;

    @Override
    public String status(Path workspace, boolean branch, List<String> paths) throws IOException {
        try (Repository repo = open(workspace); Git git = new Git(repo)) {
            StatusCommand command = git.status();
            for (String p : repoPaths(repo, workspace, paths)) {
                command.addPath(p);
            }
            Status status = command.call();
            Map<String, char[]> rows = new TreeMap<>();
            mark(rows, status.getAdded(), 'A', 0);
            mark(rows, status.getChanged(), 'M', 0);
            mark(rows, status.getRemoved(), 'D', 0);
            mark(rows, status.getModified(), 'M', 1);
            mark(rows, status.getMissing(), 'D', 1);
            mark(rows, status.getConflicting(), 'U', 0);
            mark(rows, status.getConflicting(), 'U', 1);
            StringBuilder out = new StringBuilder();
            if (branch) {
                out.append(branchLine(repo)).append('\n');
            }
            rows.forEach((path, xy) -> out.append(xy[0]).append(xy[1]).append(' ').append(path).append('\n'));
            for (String path : new java.util.TreeSet<>(status.getUntracked())) {
                out.append("?? ").append(path).append('\n');
            }
            return out.toString();
        } catch (GitAPIException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public String diff(Path workspace, Scope scope, List<String> paths) throws IOException {
        try (Repository repo = open(workspace); ByteArrayOutputStream out = new ByteArrayOutputStream();
                DiffFormatter formatter = new DiffFormatter(out); ObjectReader reader = repo.newObjectReader()) {
            formatter.setRepository(repo);
            List<String> filter = repoPaths(repo, workspace, paths);
            if (!filter.isEmpty()) {
                formatter.setPathFilter(PathFilterGroup.createFromStrings(filter));
            }
            DirCache index = repo.readDirCache();
            ObjectId head = repo.resolve(Constants.HEAD + "^{tree}");
            AbstractTreeIterator committed = head == null ? new EmptyTreeIterator() : new CanonicalTreeParser(null, reader, head);
            List<DiffEntry> entries = switch (scope) {
                case WORKTREE -> formatter.scan(new DirCacheIterator(index), new FileTreeIterator(repo));
                case STAGED -> formatter.scan(committed, new DirCacheIterator(index));
                case HEAD -> formatter.scan(committed, new FileTreeIterator(repo));
            };
            // like git, never show a file git does not know: a new file in the working tree that was not added is not a change
            List<DiffEntry> known = scope == Scope.STAGED ? entries
                    : entries.stream().filter(e -> e.getChangeType() != DiffEntry.ChangeType.ADD || index.findEntry(e.getNewPath()) >= 0).toList();
            formatter.format(known);
            formatter.flush();
            return out.toString(StandardCharsets.UTF_8);
        }
    }

    @Override
    public String log(Path workspace, int max, boolean decorate) throws IOException {
        try (Repository repo = open(workspace); Git git = new Git(repo)) {
            Map<ObjectId, List<String>> names = decorate ? decorations(repo) : Map.of();
            StringBuilder out = new StringBuilder();
            for (RevCommit c : git.log().setMaxCount(max).call()) {
                out.append(c.abbreviate(ABBREVIATION).name());
                List<String> on = names.get(c.getId());
                if (on != null) {
                    out.append(" (").append(String.join(", ", on)).append(')');
                }
                out.append(' ').append(c.getShortMessage()).append('\n');
            }
            return out.toString();
        } catch (NoHeadException e) {
            throw new IOException("your current branch does not have any commits yet", e);
        } catch (GitAPIException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    @Override
    public String commit(Path workspace, String message, List<String> paths) throws IOException {
        try (Repository repo = open(workspace); Git git = new Git(repo)) {
            List<String> named = repoPaths(repo, workspace, paths);
            for (String p : named) {
                stage(repo, git, p);
            }
            ByteArrayOutputStream hooks = new ByteArrayOutputStream();
            var commit = git.commit().setMessage(message).setAllowEmpty(false).setHookOutputStream(new java.io.PrintStream(hooks, true, StandardCharsets.UTF_8))
                    .setHookErrorStream(new java.io.PrintStream(hooks, true, StandardCharsets.UTF_8));
            named.forEach(commit::setOnly);
            RevCommit made = commit.call();
            return "[" + shortBranch(repo) + " " + made.abbreviate(ABBREVIATION).name() + "] " + made.getShortMessage()
                    + (hooks.size() == 0 ? "" : "\n" + hooks.toString(StandardCharsets.UTF_8).strip());
        } catch (EmptyCommitException e) {
            throw new IOException("nothing to commit for the given paths", e);
        } catch (JGitInternalException e) {
            // "No changes": the paths were named but are the same as the last commit
            throw new IOException("No changes".equals(e.getMessage()) ? "nothing to commit for the given paths" : e.getMessage(), e);
        } catch (AbortedByHookException e) {
            throw new IOException("a commit hook refused the commit: " + e.getMessage(), e);
        } catch (GitAPIException e) {
            throw new IOException(e.getMessage(), e);
        }
    }

    /** {@code git add -- path}: a changed or new file is added, a file that is gone has its removal staged. */
    private static void stage(Repository repo, Git git, String path) throws IOException, GitAPIException {
        if (Files.exists(repo.getWorkTree().toPath().resolve(path))) {
            git.add().addFilepattern(path).call();
        } else if (repo.readDirCache().findEntry(path) >= 0) {
            git.rm().setCached(true).addFilepattern(path).call();
        } else {
            throw new IOException("'" + path + "' did not match any file known to git");
        }
    }

    /** The repository that holds {@code dir}, found the way git finds it (the folder or one above it); the environment is not consulted. */
    private static Repository open(Path dir) throws IOException {
        FileRepositoryBuilder builder = new FileRepositoryBuilder().findGitDir(dir.toFile());
        if (builder.getGitDir() == null) {
            throw new IOException("not a git repository (or any of the parent directories): " + dir);
        }
        return builder.build();
    }

    /** Workspace-relative paths as JGit wants them: relative to the work tree, with forward slashes. */
    private static List<String> repoPaths(Repository repo, Path workspace, List<String> paths) {
        Path root = repo.getWorkTree().toPath().toAbsolutePath().normalize();
        List<String> out = new ArrayList<>();
        for (String p : paths) {
            out.add(root.relativize(workspace.toAbsolutePath().normalize().resolve(p).normalize()).toString().replace(File.separatorChar, '/'));
        }
        return out;
    }

    private static void mark(Map<String, char[]> rows, java.util.Set<String> paths, char code, int column) {
        for (String p : paths) {
            rows.computeIfAbsent(p, k -> new char[] {' ', ' '})[column] = code;
        }
    }

    private static String branchLine(Repository repo) throws IOException {
        String branch = repo.getBranch();
        if (repo.resolve(Constants.HEAD) == null) {
            return "## No commits yet on " + branch;
        }
        if (!repo.getFullBranch().startsWith(Constants.R_HEADS)) {
            return "## HEAD (no branch)";
        }
        BranchTrackingStatus tracking = BranchTrackingStatus.of(repo, branch);
        if (tracking == null) {
            return "## " + branch;
        }
        List<String> counts = new ArrayList<>();
        if (tracking.getAheadCount() > 0) {
            counts.add("ahead " + tracking.getAheadCount());
        }
        if (tracking.getBehindCount() > 0) {
            counts.add("behind " + tracking.getBehindCount());
        }
        return "## " + branch + "..." + Repository.shortenRefName(tracking.getRemoteTrackingBranch())
                + (counts.isEmpty() ? "" : " [" + String.join(", ", counts) + "]");
    }

    private static String shortBranch(Repository repo) throws IOException {
        String branch = repo.getBranch();
        return branch == null ? "HEAD" : branch;
    }

    /** Branch and tag names by commit, in the order git prints them: HEAD first, then tags, then branches. */
    private static Map<ObjectId, List<String>> decorations(Repository repo) throws IOException {
        Map<ObjectId, List<String>> names = new LinkedHashMap<>();
        Ref head = repo.exactRef(Constants.HEAD);
        String attached = head != null && head.isSymbolic() ? Repository.shortenRefName(head.getTarget().getName()) : null;
        if (head != null && head.getObjectId() != null) {
            names.computeIfAbsent(head.getObjectId(), k -> new ArrayList<>()).add(attached == null ? "HEAD" : "HEAD -> " + attached);
        }
        for (Ref ref : repo.getRefDatabase().getRefs()) {
            ObjectId target = peeledTarget(repo, ref);
            String shown = shownName(ref.getName(), attached);
            if (target != null && shown != null) {
                names.computeIfAbsent(target, k -> new ArrayList<>()).add(shown);
            }
        }
        return names;
    }

    /** The commit a ref finally points at (a tag may point at a tag object first). */
    private static ObjectId peeledTarget(Repository repo, Ref ref) throws IOException {
        Ref peeled = repo.getRefDatabase().peel(ref);
        return peeled.getPeeledObjectId() != null ? peeled.getPeeledObjectId() : ref.getObjectId();
    }

    /** How git prints a ref in a log decoration; null for the refs it leaves out (HEAD and the checked-out branch, printed first). */
    private static String shownName(String name, String attached) {
        if (name.equals(Constants.HEAD) || name.equals(Constants.R_HEADS + attached)) {
            return null;
        }
        String shortName = Repository.shortenRefName(name);
        return name.startsWith(Constants.R_TAGS) ? "tag: " + shortName : shortName;
    }
}
