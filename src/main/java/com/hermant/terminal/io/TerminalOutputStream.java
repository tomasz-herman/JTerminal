package com.hermant.terminal.io;

import com.hermant.terminal.JTerminal;
import com.hermant.terminal.TerminalController;

import javax.swing.*;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;

public class TerminalOutputStream extends OutputStream {

    private final OutputStreamBuffer buffer = new OutputStreamBuffer();

//    private char[] buff = new char[4096];

    private static final int MAX_LINES = 1000;
    private static final int BUFFER_AUTO_FLUSH_SIZE = 4096;

    private final int FRAMES_PER_SECOND = 30;
    private final long SKIP_TICKS = 1000000000 / FRAMES_PER_SECOND;

    public TerminalOutputStream(JTerminal terminal) {
        TerminalController controller = terminal.getTerminalController();
        Thread updater = getUpdaterThread(controller);
        updater.start();
    }

    private Thread getUpdaterThread(TerminalController controller){
        return new Thread(() -> {
            long lastUpdate = System.nanoTime();
            while(true){
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
        });
    }

    @Override
    public void write(int b) {
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
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                buffer.insert((char)b);
            }
        }
    }

    @Override
    public void write(byte[] b) throws IOException {
        write(b, 0, b.length);
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
//        if(buff.length < len) buff = new char[len];
//        int newLines = 0;
//        for (int i = 0; i < len; i++) {
//            buff[i] = (char)b[i + off];
//            if(buff[i] == '\n') newLines++;
//        }
//        line.append(buff, 0, len);
//        if(newLines > 0) {
//            synchronized (buffer){
//                buffer.append(line);
//                line.setLength(0);
//                buffer.notify();
//                while(buffer.length() > BUFFER_AUTO_FLUSH_SIZE) {
//                    try {
//                        buffer.wait();
//                    } catch (InterruptedException e) {
//                        e.printStackTrace();
//                    }
//                }
//            }
//        }
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
        private int tabLen = 4;
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
}

