package com.hermant;

import sun.misc.Signal;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class TerminalInputStream extends InputStream implements KeyListener {

    private final BlockingQueue<Character> buffer = new ArrayBlockingQueue<>(65536);
    private final Deque<Character> line = new ArrayDeque<>(4096);
    private final static int EOF = -1;

    @Override
    public void reset(){
        buffer.clear();
        line.clear();
    }

    @Override
    public int available() {
        return buffer.size();
    }

    @Override
    public int read() {
        try {
            return buffer.take();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return EOF;
    }


    @Override
    public int read(byte[] b) {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] b, int off, int len) {
        try {
            for (int i = off; i < len; i++) {
                b[i] = (byte)(char) buffer.take();
                if(b[i] == '\n') return i - off + 1;
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return EOF;
    }

    @Override
    public long skip(long n) throws IOException {
        return super.skip(n);
    }

    @Override
    public void keyTyped(KeyEvent e) { }

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

    @Override
    public void keyReleased(KeyEvent e) { }
}
