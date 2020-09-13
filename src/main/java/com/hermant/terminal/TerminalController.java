package com.hermant.terminal;

public interface TerminalController {
    void detach(int lines);
    void append(String s);
    void delete(int chars);
    void setCaret(int pos);
    void moveCaret(int offset);
    void moveCaretToLineStart();
    void moveCaretToLineEnd();
    int getLines();
    int getCaret();
    String getText();
}
