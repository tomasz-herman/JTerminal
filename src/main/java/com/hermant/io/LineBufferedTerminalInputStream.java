package com.hermant.io;

import com.hermant.JTerminal;
import sun.misc.Signal;

import java.awt.event.KeyEvent;
import java.util.ArrayDeque;
import java.util.Deque;

public class LineBufferedTerminalInputStream extends TerminalInputStream {

    private final Deque<Character> line = new ArrayDeque<>(4096);

    public LineBufferedTerminalInputStream(JTerminal terminal) {
        super(terminal);
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
                //TODO: ThreadPool
                // avoid to write to ostream form AWT Thread as it might cause deadlock
                new Thread(() -> terminal.getTos().write(c)).start();
                buffer.add(c);
            }
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
