package com.hermant.io;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

public class TerminalOutputStream extends OutputStream {

    private final StringBuilder line = new StringBuilder(1024);
    private final StringBuilder buffer = new StringBuilder(8192);

    private char[] buff = new char[4096];

    private int lines = 0;
    private int bufferedLines = 0;

    private static final int MAX_LINES = 16384;
    private static final int TRUNK = 4096;
    private static final int BUFFER_AUTO_FLUSH_SIZE = 4096;

    private final int FRAMES_PER_SECOND = 30;
    private final long SKIP_TICKS = 1000000000 / FRAMES_PER_SECOND;

    public TerminalOutputStream(JTextArea area) {
        Thread updater = getUpdaterThread(area);
        updater.start();
    }

    private Thread getUpdaterThread(JTextArea area){
        return new Thread(() -> {
            long lastUpdate = System.nanoTime();
            while(true){
                long now = System.nanoTime();
                long elapsed = now - lastUpdate;
                if(elapsed > SKIP_TICKS || buffer.length() > BUFFER_AUTO_FLUSH_SIZE){
                    //update text area
                    lastUpdate = now;
                    String s;
                    synchronized (buffer){
                        while(buffer.length() == 0) {
                            try {
                                buffer.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        s = buffer.toString();
                        buffer.setLength(0);
                        buffer.notify();
                    }
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            appendLinesToTerminal(area, s);
                            while(lines > MAX_LINES) {
                                detachLinesFromTerminal(area);
                            }
                        });
                    } catch (InterruptedException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    private void detachLinesFromTerminal(JTextArea area){
        try {
            area.getDocument().remove(0, area.getLineEndOffset(TRUNK - 1));
            lines -= TRUNK;
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendLinesToTerminal(JTextArea area, String s){
        try {
            area.append(s);
            lines += bufferedLines;
            bufferedLines = 0;
        } catch (Error ignored) { }
    }

    @Override
    public void write(int b) {
        line.append((char)b);
        if(b=='\n'){
            synchronized (buffer){
                bufferedLines++;
                buffer.append(line);
                line.setLength(0);
                buffer.notify();
                while(buffer.length() > BUFFER_AUTO_FLUSH_SIZE) {
                    try {
                        buffer.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void write(byte[] b) {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) {
        if(buff.length < len) buff = new char[len];
        int newLines = 0;
        for (int i = 0; i < len; i++) {
            buff[i] = (char)b[i + off];
            if(buff[i] == '\n') newLines++;
        }
        line.append(buff, 0, len);
        if(newLines > 0) {
            synchronized (buffer){
                bufferedLines+= newLines;
                buffer.append(line);
                line.setLength(0);
                buffer.notify();
                while(buffer.length() > BUFFER_AUTO_FLUSH_SIZE) {
                    try {
                        buffer.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    @Override
    public void flush() {
        if(line.length() == 0) return;
        synchronized (buffer){
            buffer.append(line);
            bufferedLines +=
                    line.toString().codePoints().filter(c -> c == '\n').count();
            line.setLength(0);
            buffer.notify();
        }
    }
}

