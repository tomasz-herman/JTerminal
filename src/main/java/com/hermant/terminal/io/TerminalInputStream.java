package com.hermant.terminal.io;

import com.hermant.terminal.JTerminal;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public abstract class TerminalInputStream extends InputStream implements KeyListener {

    protected final static int EOF = -1;
    private final static int BUFFER_SIZE = 65536;

    protected final BlockingQueue<Character> buffer = new ArrayBlockingQueue<>(BUFFER_SIZE);
    protected final JTerminal terminal;
    protected boolean echoToTos;

    protected TerminalInputStream(JTerminal terminal, boolean echoToTos) {
        this.terminal = terminal;
        this.echoToTos = echoToTos;
    }

    @Override
    public void reset(){
        buffer.clear();
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
    public void keyReleased(KeyEvent e) { }

    public boolean isEchoToTos() {
        return echoToTos;
    }

    public void setEchoToTos(boolean echoToTos) {
        this.echoToTos = echoToTos;
    }
}
