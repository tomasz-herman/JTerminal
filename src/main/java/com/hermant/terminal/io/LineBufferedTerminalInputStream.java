package com.hermant.terminal.io;

import com.hermant.terminal.JTerminal;
import sun.misc.Signal;

import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

public class LineBufferedTerminalInputStream extends TerminalInputStream {

    private final Deque<Character> line = new ArrayDeque<>(4096);

    public LineBufferedTerminalInputStream(JTerminal terminal, boolean echoToTos) {
        super(terminal, echoToTos);
    }

    @Override
    public void reset(){
        super.reset();
        line.clear();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER){
            line.add('\n');
            while(!line.isEmpty()) {
                char c = line.remove();
                if(echoToTos) terminal.getTos().write(c);
                buffer.add(c);
            }
        } else if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if(!line.isEmpty()) line.removeLast();
            else buffer.add('\10');
        } else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C){
            if(echoToTos) Signal.raise(new Signal("SIGINT"));
            else buffer.add('\3');
        } else {
            char c = e.getKeyChar();
            if(c >= 32 && c < 127) line.add(c);
        }
    }

}
