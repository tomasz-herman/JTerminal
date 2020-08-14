package com.hermant;

import com.hermant.io.LineBufferedTerminalInputStream;
import com.hermant.io.TerminalInputStream;
import com.hermant.io.TerminalOutputStream;

import javax.swing.*;
import java.awt.*;
import java.io.PrintStream;

public class JTerminal extends JScrollPane {
    private LineBufferedTerminalInputStream tis;
    private TerminalOutputStream tos;
    private int verticalScrollBarMaximumValue;

    private final JTextArea terminal = new JTextArea();

    public JTerminal() {
        tis = new LineBufferedTerminalInputStream();
        tos = new TerminalOutputStream(terminal);

        setViewportView(terminal);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        verticalScrollBarMaximumValue = getVerticalScrollBar().getMaximum();
        getVerticalScrollBar().addAdjustmentListener(e -> {
            if ((verticalScrollBarMaximumValue - e.getAdjustable().getMaximum()) == 0) return;
            e.getAdjustable().setValue(e.getAdjustable().getMaximum());
            verticalScrollBarMaximumValue = getVerticalScrollBar().getMaximum();
        });

        terminal.setLineWrap(true);
        terminal.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 24));
        terminal.addKeyListener(tis);

        disableArrowKeys();
        terminal.setEditable(false);
    }

    private void disableArrowKeys() {
        String[] keys = {"UP", "DOWN", "LEFT", "RIGHT", "HOME", "ENTER"};
        for (String key : keys) {
            terminal.getInputMap().put(KeyStroke.getKeyStroke(key), "none");
        }
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
//        System.setErr(printStream);
    }

}
