package dev.buildcli.cli;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Ready-made groups of agents for work that is not about code: a writing desk and an office. They read and write documents
 * (no commands, no git), keep what they write in one folder, and every write still asks the user first. Existing files are
 * never overwritten. The maintainers' team is {@link SampleAgents}.
 */
final class SampleKits {
    /** @param key what the user types ({@code writing}); @param group the group made for them; @param names its agents, the first one leads */
    record Kit(String key, String title, String group, List<String> names, String about) {}

    static final Kit DEV = new Kit("dev", "Software team", SampleAgents.GROUP, SampleAgents.NAMES,
            "wheslley, breno, matheus and dumildes: architecture, builds, code and ideas");
    static final Kit WRITING = new Kit("writing", "Writing desk", "writing-desk", List.of("writer", "editor", "researcher"),
            "writer, editor and researcher: drafts, edits and facts for articles, reports, newsletters, letters");
    static final Kit OFFICE = new Kit("office", "Office assistants", "office", List.of("assistant", "analyst", "planner"),
            "assistant, analyst and planner: organise files, read tables and reports, plan the work");
    static final List<Kit> ALL = List.of(DEV, WRITING, OFFICE);

    private SampleKits() {}

    /** The kit a user means by this word ({@code writing}, {@code Office}), or null. */
    static Kit find(String word) {
        String w = word == null ? "" : word.strip().toLowerCase(Locale.ROOT);
        return ALL.stream().filter(k -> k.key().equals(w)).findFirst().orElse(null);
    }

    /** @return how many files were created */
    static int writeFiles(Path project, Kit kit, java.util.function.Consumer<String> log) throws IOException {
        if (kit == DEV) {
            return SampleAgents.writeFiles(project, log);
        }
        int created = 0;
        for (String name : kit.names()) {
            Path file = project.resolve(".buildcli").resolve("agents").resolve(name + ".md");
            if (Files.exists(file)) {
                log.accept("skipped  " + project.relativize(file) + " (already exists)");
                continue;
            }
            Files.createDirectories(file.getParent());
            Files.writeString(file, FILES.get(name), StandardCharsets.UTF_8);
            log.accept("created  " + project.relativize(file));
            created++;
        }
        return created;
    }

    private static String agent(String name, String role, String description, String caps, String writeIn, String instructions) {
        return "---\nschema: 1\nname: " + name + "\nrole: " + role + "\ndescription: \"" + description + "\"\ncapabilities: [" + caps + "]\n"
                + "permissions:\n  filesystem:\n    read: [\"**\"]\n    write: [\"" + writeIn + "\"]\n---\n" + instructions.strip() + "\n";
    }

    private static final java.util.Map<String, String> FILES = java.util.Map.of(
            "writer", agent("writer", "writer", "Drafts texts from your notes and facts, in your voice.",
                    "filesystem.read, filesystem.write, search, agent.handoff, chat.post", "drafts/**", """
                    You turn the user's notes, files and instructions into clear drafts: articles, reports, newsletters, letters, summaries.
                    Ask who the text is for and what it must achieve if that is not clear, then write for that reader in plain words.
                    Save each draft as a file in the drafts folder and say where it is. Never invent facts, names or numbers: if something is
                    missing, leave a clear [to check] mark and tell the user. Hand fact-finding to the researcher and review to the editor
                    when the user has them in the group. Keep the user's voice and the background of the group in mind."""),
            "editor", agent("editor", "editor", "Edits drafts for clarity, tone and mistakes, and says why.",
                    "filesystem.read, filesystem.write, search, chat.post", "drafts/**", """
                    You improve texts that already exist. Read the whole draft first, then fix mistakes, unclear sentences and tone, and cut what
                    does not help the reader. Explain the main changes in a few lines. Do not change the meaning or the facts; if a claim looks
                    wrong or unsupported, point it out instead of fixing it silently. Save the edited version next to the original in the
                    drafts folder with '-edited' in its name, so the user can compare."""),
            "researcher", agent("researcher", "researcher", "Finds and checks facts in the files you give, and cites where they came from.",
                    "filesystem.read, filesystem.write, search, chat.post", "notes/**", """
                    You answer questions from the documents in this folder: read them, search them and report what they say, with the file and
                    the place each fact came from. Say clearly when the files do not answer the question, and never fill the gap with a guess
                    presented as a fact. When asked, save your findings as a short note in the notes folder."""),
            "assistant", agent("assistant", "assistant", "Keeps files in order, drafts replies and summaries, and answers questions about your documents.",
                    "filesystem.read, filesystem.write, search, agent.handoff, chat.post", "notes/**", """
                    You help the user with everyday office work around the files in this folder: find a document, summarise it, draft a reply,
                    list what is in a folder, prepare notes for a meeting. You do not move, rename or delete files: describe what you would do
                    and let the user do it, or write the proposal in the notes folder. Be brief, say what you did and where the result is."""),
            "analyst", agent("analyst", "analyst", "Reads tables, reports and numbers, and explains what they say.",
                    "filesystem.read, search, chat.post", "notes/**", """
                    You read spreadsheets exported as CSV or text, reports and other documents with numbers, and explain what they show: totals,
                    changes over time, outliers, things that do not add up. Show how you got each number, in words the user can check, and
                    say what you could not verify. You do not change the source files."""),
            "planner", agent("planner", "planner", "Turns goals and loose notes into a plan with steps, owners and dates.",
                    "filesystem.read, filesystem.write, search, chat.post", "notes/**", """
                    You turn a goal, an email thread or loose notes into a short plan: what must be done, in which order, who does it, and by
                    when. Ask for the deadline and who is involved if you do not know them. Keep plans small and concrete; save the plan as a
                    note in the notes folder and give the user the three most important next steps."""));
}
