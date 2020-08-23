package com.hermant.terminal.io;

import com.hermant.terminal.JTerminal;
import sun.misc.Signal;

import java.awt.event.KeyEvent;

public class NonBufferedTerminalInputStream extends TerminalInputStream {

    public NonBufferedTerminalInputStream(JTerminal terminal, boolean echoToTos) {
        super(terminal, echoToTos);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER){
            buffer.add('\n');
            if(echoToTos) terminal.getTos().write('\n');
        } else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C){
            if(echoToTos) Signal.raise(new Signal("SIGINT"));
            else buffer.add('\3');
        } else if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE){
            buffer.add('\10');
        } else if(e.getKeyCode() == KeyEvent.VK_ESCAPE){
            buffer.add('\33');
        } else {
            char c = e.getKeyChar();
            if(c >= 32 && c < 127) {
                buffer.add(c);
                if(echoToTos) {
                    terminal.getTos().write(c);
                    terminal.getTos().flush();
                }
            }

        }
    }

}
