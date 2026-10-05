package dev.buildcli.application;

import dev.buildcli.application.ChatSession.Pending;
import dev.buildcli.ports.ApprovalRequest;
import dev.buildcli.ports.EscalationChoice;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BiConsumer;

/**
 * The questions agents ask the user (approve this write, what to do with a task that keeps failing) and the "always allow
 * here" answers. An agent that asks waits on its own thread until the user answers; several can ask at once.
 */
final class Approvals {
    private final List<Pending> pending = new CopyOnWriteArrayList<>();
    /** "Always allow" answers: chat, agent and what, to what it means. In memory only: a grant never outlives the session. */
    private final Map<String, String> grants = new ConcurrentHashMap<>();
    private final Runnable changed;
    /** Sets what an agent is doing ("waiting for you"). */
    private final BiConsumer<String, String> setState;

    private final java.util.function.Supplier<ApprovalMode> mode;

    Approvals(Runnable changed, BiConsumer<String, String> setState, java.util.function.Supplier<ApprovalMode> mode) {
        this.changed = changed;
        this.setState = setState;
        this.mode = mode;
    }

    private static String grantId(String thread, ApprovalRequest r) {
        return thread + "\u0001" + r.agent() + "\u0001" + r.grantKey();
    }

    /** @param thread the chat the agent is working for; @param stopped whether the user already stopped this run */
    boolean approve(ApprovalRequest request, String thread, boolean stopped) {
        if (stopped) {
            return false;
        }
        if (request.grantKey() != null && grants.containsKey(grantId(thread, request))) {
            return true;
        }
        if (mode.get().approves(request.kind())) {
            return true;
        }
        var answer = new CompletableFuture<Boolean>();
        Pending p = new Pending.Approval(request, answer, thread);
        pending.add(p);
        changed.run();
        setState.accept(request.agent(), "waiting for you");
        try {
            return answer.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        } catch (ExecutionException | CancellationException e) {
            return false;
        } finally {
            pending.remove(p);
            changed.run();
            setState.accept(request.agent(), "working");
        }
    }

    EscalationChoice escalate(int taskId, String agent, String objective, String reason, String thread, boolean stopped) {
        if (stopped) {
            return EscalationChoice.ABORT;
        }
        var answer = new CompletableFuture<EscalationChoice>();
        Pending p = new Pending.Escalation(taskId, agent, objective, reason, answer, thread);
        pending.add(p);
        changed.run();
        try {
            return answer.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return EscalationChoice.ABORT;
        } catch (ExecutionException | CancellationException e) {
            return EscalationChoice.ABORT;
        } finally {
            pending.remove(p);
        }
    }

    /** Asks an open or multiple-choice question. @return what the user answered, or why there is no answer */
    String ask(String agent, String question, List<String> options, String thread, boolean stopped) {
        if (stopped) {
            return "The user stopped this work, so there is no answer. Stop and report.";
        }
        var answer = new CompletableFuture<String>();
        Pending p = new Pending.Question(agent, question, List.copyOf(options), answer, thread);
        pending.add(p);
        changed.run();
        setState.accept(agent, "waiting for you");
        try {
            String text = answer.get();
            return text == null || text.isBlank() ? "The user chose not to answer. Decide yourself, or report that you could not go on." : "The user answered: " + text;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "No answer (interrupted).";
        } catch (ExecutionException | CancellationException e) {
            return "No answer (" + e.getClass().getSimpleName() + ").";
        } finally {
            pending.remove(p);
            changed.run();
            setState.accept(agent, "working");
        }
    }

    /** Answers yes to this request and to the same kind of request from this agent in this chat from now on. */
    void approveAlways(Pending.Approval a) {
        ApprovalRequest r = a.request();
        if (r.grantKey() != null) {
            grants.put(grantId(a.thread(), r), r.grantLabel());
        }
        a.answer().complete(true);
    }

    /** What is being approved automatically in this chat. */
    List<String> grants(String thread) {
        List<String> out = new ArrayList<>();
        grants.forEach((id, label) -> {
            if (id.startsWith(thread + "\u0001")) {
                out.add(label);
            }
        });
        Collections.sort(out);
        return out;
    }

    /** Asks again from now on. @return how many permissions were taken back */
    int revokeGrants(String thread) {
        int before = grants.size();
        grants.keySet().removeIf(id -> id.startsWith(thread + "\u0001"));
        int n = before - grants.size();
        if (n > 0) {
            changed.run();
        }
        return n;
    }

    /** The oldest open question, or null. */
    Pending first() {
        // one read of the list: checking isEmpty() and then get(0) fails when an answer removes the request in between
        var oldest = pending.iterator();
        return oldest.hasNext() ? oldest.next() : null;
    }

    int count() {
        return pending.size();
    }

    /** Answers "no" (or "abort") to the questions of {@code thread}, or of every chat if null. */
    void declineAll(String thread) {
        for (Pending p : pending) {
            if (thread == null || p.thread().equals(thread)) {
                if (p instanceof Pending.Approval a) {
                    a.answer().complete(false);
                } else if (p instanceof Pending.Escalation e) {
                    e.answer().complete(EscalationChoice.ABORT);
                } else if (p instanceof Pending.Question q) {
                    q.answer().complete("");
                }
            }
        }
    }
}
