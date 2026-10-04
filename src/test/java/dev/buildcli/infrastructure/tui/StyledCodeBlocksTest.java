package dev.buildcli.infrastructure.tui;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.tamboui.style.Style;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Finding code blocks in a markdown answer, and the copy button on each. */
class StyledCodeBlocksTest {
    @Test
    void blocksAreReturnedInOrderWithTheirIndentation() {
        String text = "intro\n```java\nclass A {\n    int x;\n}\n```\nmid\n```\nplain\n```\n";
        assertEquals(List.of("class A {\n    int x;\n}", "plain"), Styled.codeBlocks(text));
    }

    @Test
    void anUnclosedBlockWhileStreamingStillCounts() {
        assertEquals(List.of("half a li"), Styled.codeBlocks("```sh\nhalf a li"));
    }

    @Test
    void textWithoutFencesHasNoBlocksAndABacktickInsideTextIsNotOne() {
        assertEquals(List.of(), Styled.codeBlocks("use `x` here"));
        assertEquals(List.of(), Styled.codeBlocks(""));
    }

    @Test
    void theHeaderNamesTheLanguageAndItsButtonCopiesThatBlock() {
        List<String> copied = new ArrayList<>();
        var lines = Styled.lines("```cobol\nMOVE A\n```", 40, Style.EMPTY, Style.EMPTY.bold(), Style.EMPTY, copied::add);
        assertEquals(2, lines.size(), "a header line and the code line");
        assertEquals("cobol  ", lines.get(0).get(0).text());
        var button = lines.get(0).get(1);
        assertTrue(button.text().contains("copy"));
        button.action().run();
        assertEquals(List.of("MOVE A"), copied);
        assertEquals("MOVE A", lines.get(1).get(0).text());
    }

    @Test
    void withoutACopyHandlerTheOutputIsAsBefore() {
        var lines = Styled.lines("```cobol\nMOVE A\n```", 40, Style.EMPTY, Style.EMPTY.bold(), Style.EMPTY);
        assertEquals("cobol", lines.get(0).get(0).text());
        assertTrue(lines.get(0).stream().noneMatch(sp -> sp.action() != null || sp.text().contains("copy")), "no button");
    }

    @Test
    void aBlockWithoutALanguageIsLabelledCode() {
        var lines = Styled.lines("```\nx\n```", 40, Style.EMPTY, Style.EMPTY.bold(), Style.EMPTY, t -> { });
        assertEquals("code  ", lines.get(0).get(0).text());
    }
}
