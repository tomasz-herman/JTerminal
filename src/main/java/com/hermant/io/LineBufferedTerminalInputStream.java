package com.hermant.io;

import sun.misc.Signal;

import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

public class LineBufferedTerminalInputStream extends TerminalInputStream {

    private final Deque<Character> line = new ArrayDeque<>(4096);

    @Override
    public void reset(){
        super.reset();
        line.clear();
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER){
            line.add('\n');
            while(!line.isEmpty()) buffer.add(line.remove());
        } else if(e.getKeyCode() == KeyEvent.VK_BACK_SPACE) {
            if(!line.isEmpty()) line.removeLast();
        } else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C){
            Signal.raise(new Signal("INT"));
        } else {
            char c = e.getKeyChar();
            if(c >= 32 && c < 127) line.add(c);
        }
    }

}
