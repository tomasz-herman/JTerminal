package com.hermant.terminal.io;

import com.hermant.terminal.JTerminal;
import com.hermant.terminal.TerminalController;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

public class TerminalOutputStream extends OutputStream {

    private final OutputStreamBuffer buffer = new OutputStreamBuffer();
    private static final int MAX_LINES = 1000;
    private static final int BUFFER_AUTO_FLUSH_SIZE = 4096;

    public TerminalOutputStream(JTerminal terminal) {
        TerminalController controller = terminal.getTerminalController();
        UpdateAreaTask updater = new UpdateAreaTask(controller);
        new Thread(updater).start();
    }

    @Override
    public synchronized void write(int b) {
        synchronized (buffer) {
            if(b < 32) {
                if(b == '\n'){
                    buffer.newline();
                } else if(b == '\r') {
                    buffer.ret();
                } else if(b == '\b') {
                    buffer.back();
                } else if(b == '\t') {
                    buffer.tab();
                }
                buffer.notify();
                while(buffer.shouldAutoFlush()) {
                    try {
                        buffer.wait();
                    } catch (InterruptedException ignored) { }
                }
            } else {
                buffer.insert((char)b);
            }
        }
    }

    @Override
    public synchronized void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public synchronized void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
    }

    @Override
    public void flush() {
        synchronized (buffer){
            buffer.flush();
            buffer.notify();
        }
    }

    private static class OutputStreamBuffer {
        private final StringBuilder buffer = new StringBuilder(65536);
        private int caretOffset = 0;
        private int newline = 0;
        private boolean flush = false;

        public Dump dump() {
            String result = buffer.toString();
            String line = buffer.substring(newline);
            buffer.setLength(0);
            buffer.append(line);
            newline = 0;
            flush = false;
            return new Dump(result, caretOffset);
        }

        public void insert(char c) {
            if(caretOffset == 0) {
                buffer.append(c);
            } else {
                buffer.setCharAt(buffer.length() + caretOffset++, c);
            }
        }

        public void newline() {
            caretOffset = 0;
            insert('\n');
            newline = buffer.length();
        }

        public void ret() {
            caretOffset = newline - buffer.length();
        }

        public void tab() {
            int lineLen = buffer.length() - newline + caretOffset;
            int tabLen = 4;
            int spaces = tabLen - lineLen % tabLen;
            for (int i = 0; i < spaces; i++) {
                insert(' ');
            }
        }

        public void back() {
            caretOffset--;
            caretOffset = Math.max(newline - buffer.length(), caretOffset);
        }

        public boolean shouldAutoFlush() {
            return buffer.length() > BUFFER_AUTO_FLUSH_SIZE;
        }

        public boolean shouldFlush() {
            return newline > 0 || flush || shouldAutoFlush();
        }

        public void flush() {
            if(buffer.length() == 0) return;
            flush = true;
        }

        private static final class Dump {
            public String string;
            public int caretOffset;

            public Dump(String string, int caretOffset) {
                this.string = string;
                this.caretOffset = caretOffset;
            }
        }

    }

    private class UpdateAreaTask implements Runnable {

        private final TerminalController controller;

        public UpdateAreaTask(TerminalController controller) {
            this.controller = controller;
        }

        @Override
        public void run() {
            long lastUpdate = System.nanoTime();
            int FRAMES_PER_SECOND = 30;
            long SKIP_TICKS = 1000000000 / FRAMES_PER_SECOND;
            while(!Thread.interrupted()){
                long now = System.nanoTime();
                long elapsed = now - lastUpdate;
                if(elapsed > SKIP_TICKS || buffer.shouldAutoFlush()){
                    //update text area
                    lastUpdate = now;
                    OutputStreamBuffer.Dump dump;
                    synchronized (buffer){
                        while(!buffer.shouldFlush()) {
                            try {
                                buffer.wait();
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }
                        dump = buffer.dump();
                        buffer.notify();
                    }
                    try {
                        SwingUtilities.invokeAndWait(() -> {
                            controller.moveCaretToLineStart();
                            controller.append(dump.string);
                            controller.moveCaret(dump.caretOffset);
                            int lines;
                            while((lines = controller.getLines()) > MAX_LINES) {
                                controller.detach(lines - MAX_LINES);
                            }
                        });
                    } catch (InterruptedException | InvocationTargetException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}

