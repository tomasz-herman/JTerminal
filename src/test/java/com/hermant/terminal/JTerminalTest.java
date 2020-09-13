package com.hermant.terminal;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import javax.swing.*;

import static org.junit.jupiter.api.Assertions.*;

class JTerminalTest {

    @ParameterizedTest
    @ValueSource(strings = {"12345", "a\nb\nc", "12", " ", "\n", "\t", "Lorem\nipsum\n"})
    void append(String s) throws IllegalAccessException {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append(s);
        JTextArea text = (JTextArea)FieldUtils.readField(terminal, "terminal", true);
        assertEquals(s, text.getText());
    }

    @ParameterizedTest
    @ValueSource(strings = {"12345", "a\nb\nc", "12", "", " ", "\n", "\t", "Lorem\nipsum\n"})
    void delete(String s) throws IllegalAccessException {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append(s);
        controller.delete(3);
        JTextArea text = (JTextArea)FieldUtils.readField(terminal, "terminal", true);
        assertEquals(s.substring(0, Math.max(0, s.length() - 3)), text.getText());
    }

    @Test
    void detach() throws IllegalAccessException {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append("Lorem\nipsum\n");
        controller.detach(1);
        JTextArea text = (JTextArea)FieldUtils.readField(terminal, "terminal", true);
        assertEquals("ipsum\n", text.getText());
    }

    @Test
    void moveCaretToLineStart() throws IllegalAccessException {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append("Lorem\nipsum");
        controller.moveCaretToLineStart();
        JTextArea text = (JTextArea)FieldUtils.readField(terminal, "terminal", true);
        assertEquals(6, text.getCaretPosition());
    }

    @Test
    void moveCaretToLineEnd() throws IllegalAccessException {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append("Lorem\nipsum");
        JTextArea text = (JTextArea)FieldUtils.readField(terminal, "terminal", true);
        text.setCaretPosition(8);
        controller.moveCaretToLineEnd();
        assertEquals(11, text.getCaretPosition());
    }

    @ParameterizedTest
    @ValueSource(ints = {-5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5})
    void moveCaret(int off) throws IllegalAccessException {
        JTerminal terminal = new JTerminal();
        TerminalController controller = terminal.getTerminalController();
        controller.append("Lorem\nipsum");
        JTextArea text = (JTextArea)FieldUtils.readField(terminal, "terminal", true);
        controller.moveCaretToLineStart();
        controller.moveCaret(off);
        assertEquals(6 + off, text.getCaretPosition());
    }
}