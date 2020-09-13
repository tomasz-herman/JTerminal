package com.hermant.terminal;

public interface TerminalController {
    void detach(int lines);
    void append(String s);
    void delete(int chars);
    void moveCaret(int offset);
    void moveCaretToLineStart();
    void moveCaretToLineEnd();
}
