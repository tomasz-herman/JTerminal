package com.hermant.io;

import com.hermant.JTerminal;
import sun.misc.Signal;

import java.awt.event.KeyEvent;

public class NonBufferedTerminalInputStream extends TerminalInputStream {

    public NonBufferedTerminalInputStream(JTerminal terminal) {
        super(terminal);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER){
            buffer.add('\n');
            //TODO: ThreadPool
            // avoid to write to ostream form AWT Thread as it might cause deadlock
            new Thread(() -> terminal.getTos().write('\n')).start();
        } else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C){
            Signal.raise(new Signal("INT"));
        } else {
            char c = e.getKeyChar();
            if(c >= 32 && c < 127) {
                buffer.add(c);
                //TODO: ThreadPool
                // avoid to write to ostream form AWT Thread as it might cause deadlock
                new Thread(() -> {
                    terminal.getTos().write(c);
                    terminal.getTos().flush();
                }).start();
            }

        }
    }

}
