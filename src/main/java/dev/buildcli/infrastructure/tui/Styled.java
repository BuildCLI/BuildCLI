package dev.buildcli.infrastructure.tui;

import dev.tamboui.style.Style;
import java.util.ArrayList;
import java.util.List;

/** Text with light markdown (bold, inline code, fenced code, headings, bullets), wrapped into styled lines. */
final class Styled {
    private Styled() {}

    /** A run of text in one style; {@code action} is what a click on it does, or null. */
    record Span(String text, Style style, Runnable action) {
        Span(String text, Style style) {
            this(text, style, null);
        }

        int width() {
            return Wrap.width(text);
        }
    }

    static int width(List<Span> line) {
        int w = 0;
        for (Span s : line) {
            w += s.width();
        }
        return w;
    }

    /** The text of each fenced code block, without its fences, in order. An unclosed block (still streaming) counts. */
    static List<String> codeBlocks(String text) {
        List<String> blocks = new ArrayList<>();
        StringBuilder current = null;
        for (String raw : text.replace("\t", "    ").split("\n", -1)) {
            if (raw.strip().startsWith("```")) {
                if (current == null) {
                    current = new StringBuilder();
                } else {
                    blocks.add(current.toString());
                    current = null;
                }
            } else if (current != null) {
                current.append(current.isEmpty() ? "" : "\n").append(raw);
            }
        }
        if (current != null) {
            blocks.add(current.toString());
        }
        return blocks;
    }

    static List<List<Span>> lines(String text, int width, Style base, Style bold, Style code) {
        return lines(text, width, base, bold, code, null);
    }

    /**
     * Wraps {@code text} to {@code width} columns; each returned line is a list of spans.
     *
     * @param onCopy when not null, every code block gets a header line with its language and a "copy" button that passes
     *               the block's text to it
     */
    static List<List<Span>> lines(String text, int width, Style base, Style bold, Style code, java.util.function.Consumer<String> onCopy) {
        List<List<Span>> out = new ArrayList<>();
        boolean fenced = false;
        List<String> blocks = codeBlocks(text);
        int block = 0;
        List<Style[]> coloured = null;
        int codeLine = 0;
        int blockStart = -1;
        for (String raw : text.replace("\t", "    ").split("\n", -1)) {
            String trimmed = raw.strip();
            if (trimmed.startsWith("```")) {
                fenced = !fenced;
                if (!fenced) {
                    padBlock(out, blockStart, code);
                    blockStart = -1;
                } else {
                    blockStart = out.size();
                }
                if (fenced) {
                    String body = block < blocks.size() ? blocks.get(block) : "";
                    block++;
                    coloured = CodeHighlight.styles(body, trimmed.substring(3), code);
                    codeLine = 0;
                } else {
                    coloured = null;
                }
                if (fenced && onCopy != null) {
                    String label = trimmed.length() > 3 ? trimmed.substring(3).strip() : "code";
                    String body = blocks.get(Math.min(block, blocks.size()) - 1);
                    out.add(List.of(new Span(label + "  ", code), new Span(" ⧉ copy ", code.bold(), () -> onCopy.accept(body))));
                } else if (fenced && trimmed.length() > 3) {
                    out.add(List.of(new Span(trimmed.substring(3).strip(), code)));
                }
                continue;
            }
            String plain;
            Style[] styles;
            if (fenced) {
                plain = raw;
                styles = coloured != null && codeLine < coloured.size() && coloured.get(codeLine).length == raw.length()
                        ? coloured.get(codeLine) : new Style[raw.length()];
                codeLine++;
                for (int i = 0; i < styles.length; i++) {
                    if (styles[i] == null) {
                        styles[i] = code;
                    }
                }
            } else {
                String line = raw;
                Style lineBase = base;
                if (trimmed.startsWith("#")) {
                    line = trimmed.replaceFirst("^#+\\s*", "");
                    lineBase = bold;
                } else if (trimmed.startsWith(">")) {
                    line = "▎ " + trimmed.substring(1).strip();
                    lineBase = base.italic().dim();
                } else if (trimmed.startsWith("- ") || trimmed.startsWith("* ")) {
                    line = raw.substring(0, raw.indexOf(trimmed.charAt(0))) + "• " + trimmed.substring(2);
                }
                StringBuilder sb = new StringBuilder();
                List<Style> st = new ArrayList<>();
                inline(line, lineBase, bold, code, sb, st);
                plain = sb.toString();
                styles = st.toArray(new Style[0]);
            }
            for (Wrap.Segment seg : Wrap.layout(plain, width)) {
                out.add(slice(plain, styles, seg.start(), stripEnd(plain, seg.start(), seg.end())));
            }
        }
        if (blockStart >= 0) {
            padBlock(out, blockStart, code); // a block still streaming
        }
        if (out.isEmpty()) {
            out.add(List.of());
        }
        return out;
    }

    /** Lines of a code block get the same width, so its background is a rectangle and not a ragged edge. */
    private static void padBlock(List<List<Span>> out, int from, Style code) {
        int widest = 0;
        for (int i = from; i < out.size(); i++) {
            widest = Math.max(widest, width(out.get(i)));
        }
        for (int i = from; i < out.size(); i++) {
            int missing = widest - width(out.get(i));
            if (missing > 0) {
                List<Span> padded = new ArrayList<>(out.get(i));
                padded.add(new Span(" ".repeat(missing), code));
                out.set(i, padded);
            }
        }
    }

    private static int stripEnd(String s, int start, int end) {
        int e = end;
        while (e > start && s.charAt(e - 1) == ' ') {
            e--;
        }
        return e;
    }

    private static void inline(String line, Style base, Style bold, Style code, StringBuilder text, List<Style> styles) {
        boolean isBold = false;
        boolean isCode = false;
        boolean isItalic = false;
        Style italic = base.italic();
        for (int i = 0; i < line.length(); i++) {
            if (!isCode && line.startsWith("**", i) && (isBold || line.indexOf("**", i + 2) > 0)) {
                isBold = !isBold;
                i++;
                continue;
            }
            if (line.charAt(i) == '`' && (isCode || line.indexOf('`', i + 1) > 0)) {
                isCode = !isCode;
                continue;
            }
            char c = line.charAt(i);
            boolean edge = i == 0 || !Character.isLetterOrDigit(line.charAt(i - 1));
            if (!isCode && (c == '*' || c == '_') && !line.startsWith("**", i)
                    && (isItalic ? i + 1 >= line.length() || !Character.isLetterOrDigit(line.charAt(i + 1))
                        : edge && i + 1 < line.length() && !Character.isWhitespace(line.charAt(i + 1)) && line.indexOf(c, i + 2) > 0)) {
                isItalic = !isItalic;
                continue;
            }
            text.append(c);
            styles.add(isCode ? code : isBold ? bold : isItalic ? italic : base);
        }
    }

    /** One line of plain text without markdown markers, for previews. */
    static String plain(String text) {
        String first = text.strip().lines().findFirst().orElse("");
        StringBuilder sb = new StringBuilder();
        inline(first.replaceFirst("^#+\\s*", "").replace("```", ""), Style.EMPTY, Style.EMPTY.bold(), Style.EMPTY.dim(), sb, new ArrayList<>());
        return sb.toString();
    }

    private static List<Span> slice(String plain, Style[] styles, int start, int end) {
        List<Span> spans = new ArrayList<>();
        int from = start;
        for (int i = start + 1; i <= end; i++) {
            if (i == end || styles[i] != styles[from]) {
                spans.add(new Span(plain.substring(from, i), styles[from]));
                from = i;
            }
        }
        return spans;
    }
}
