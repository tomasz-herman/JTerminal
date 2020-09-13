package com.hermant.terminal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class JTerminalTest {

    @ParameterizedTest
    @ValueSource(strings = {"12345", "a\nb\nc", "12", " ", "\n", "\t", "Lorem\nipsum\n"})
    void append(String s) {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append(s);
        assertEquals(s, controller.getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "a\nb\nc", "12", "", " ", "\n", "\t", "Lorem\nipsum\n"})
    void delete(String s) {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append(s);
        controller.delete(3);
        assertEquals(s.substring(0, Math.max(0, s.length() - 3)), controller.getText());
    }

    @Test
    void detach() {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append("Lorem\nipsum\n");
        controller.detach(1);
        assertEquals("ipsum\n", controller.getText());
    }

    @Test
    void moveCaretToLineStart() {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append("Lorem\nipsum");
        controller.moveCaretToLineStart();
        assertEquals(6, controller.getCaret());
    }

    @Test
    void moveCaretToLineEnd() {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append("Lorem\nipsum");
        controller.setCaret(8);
        controller.moveCaretToLineEnd();
        assertEquals(11, controller.getCaret());
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5})
    void moveCaret(int off) {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append("Lorem\nipsum");
        controller.moveCaretToLineStart();
        controller.moveCaret(off);
        assertEquals(6 + off, controller.getCaret());
    }
}