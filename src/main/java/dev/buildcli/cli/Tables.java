package dev.buildcli.cli;

import java.io.PrintStream;
import java.util.List;

/** Minimal aligned plain-text tables for command output. */
final class Tables {
    private Tables() {}

    static void print(PrintStream out, List<String> headers, List<List<String>> rows) {
        int[] width = new int[headers.size()];
        for (int i = 0; i < width.length; i++) {
            width[i] = headers.get(i).length();
            for (List<String> r : rows) {
                width[i] = Math.max(width[i], r.get(i).length());
            }
        }
        line(out, headers, width);
        for (List<String> r : rows) {
            line(out, r, width);
        }
    }

    private static void line(PrintStream out, List<String> cells, int[] width) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < cells.size(); i++) {
            String c = cells.get(i);
            sb.append(i == cells.size() - 1 ? c : String.format("%-" + width[i] + "s  ", c));
        }
        out.println(sb.toString().stripTrailing());
    }

    static String cut(String s, int max) {
        String one = s.replace('\n', ' ');
        return one.length() <= max ? one : one.substring(0, max - 1) + "…";
    }
}
