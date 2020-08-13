package com.hermant.io;

import sun.misc.Signal;

import java.awt.event.KeyEvent;

public class NonBufferedTerminalInputStream extends TerminalInputStream {

    @Override
    public void keyPressed(KeyEvent e) {
        if(e.getKeyCode() == KeyEvent.VK_ENTER){
            buffer.add('\n');
        } else if(e.isControlDown() && e.getKeyCode() == KeyEvent.VK_C){
            Signal.raise(new Signal("INT"));
        } else {
            char c = e.getKeyChar();
            if(c >= 32 && c < 127) buffer.add(c);
        }
    }

}
