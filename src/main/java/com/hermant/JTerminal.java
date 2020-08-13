package com.hermant;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;

public class JTerminal extends JScrollPane {
    private TerminalInputStream tis;
    private TerminalOutputStream tos;
    private int maxLines = 1000;

    private final JTextArea terminal = new JTextArea();

    public JTerminal() {
        tis = new TerminalInputStream();
        tos = new TerminalOutputStream();

        setViewportView(terminal);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);

        terminal.setLineWrap(true);
        terminal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 24));
        terminal.addKeyListener(tis);
        System.setIn(tis);
    }

    public TerminalInputStream getTis() {
        return tis;
    }

    public TerminalOutputStream getTos() {
        return tos;
    }

    public void bindToSystemStreams() {
        System.setIn(tis);
        PrintStream printStream = new PrintStream(tos);
        System.setOut(printStream);
        System.setErr(printStream);
    }

    public int getMaxLines() {
        return maxLines;
    }

    public void setMaxLines(int maxLines) {
        this.maxLines = maxLines;
    }

}
