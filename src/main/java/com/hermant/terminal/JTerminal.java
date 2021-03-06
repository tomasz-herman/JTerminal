package com.hermant.terminal;

import com.hermant.terminal.io.LineBufferedTerminalInputStream;
import com.hermant.terminal.io.NonBufferedTerminalInputStream;
import com.hermant.terminal.io.TerminalInputStream;
import com.hermant.terminal.io.TerminalOutputStream;

import javax.swing.*;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.*;
import java.awt.font.TextAttribute;
import java.io.*;
import java.util.Collections;
import java.util.Map;

import static javax.swing.BorderFactory.createEmptyBorder;

public class JTerminal extends JScrollPane {

    private static final FontProvider DEFAULT_FONT_PROVIDER = new FontProvider(FontProvider.INCONSOLATA);

    private TerminalInputStream tis;
    private TerminalOutputStream tos;
    private int verticalScrollBarMaximumValue;
    private final TerminalController controller;

    private final JTextArea terminal = new JTextArea(25, 80);

    public JTerminal() {
        this(false, false);
    }

    public JTerminal(boolean bufferedInStream, boolean echoToTos) {
        controller = new JTerminalController();
        createStreams(bufferedInStream, echoToTos);
        setupTerminal();
        enableAutoBorder();
        disableArrowKeys();
        enableSmartScroll();
    }

    private void setupTerminal() {
        setViewportView(terminal);
        terminal.setLineWrap(true);
        terminal.setFont(getDefaultFont(24));
        terminal.addKeyListener(tis);
        terminal.setEditable(false);
////        terminal.setCaret(new TerminalCaret());
////        terminal.getCaret().setVisible(true);
        terminal.setTabSize(4);
    }

    private void createStreams(boolean bufferedInStream, boolean echoToTos) {
        tis = bufferedInStream ?
                new LineBufferedTerminalInputStream(this, echoToTos):
                new NonBufferedTerminalInputStream(this, echoToTos);
        tos = new TerminalOutputStream(this);
    }

    /**
     * Smart scroll does two things.</br>
     * It disables scrolling to the appended text, when the scrollbar is not at the bottom.</br>
     * It aligns scrollbar with the lines of the terminal.
     */
    private void enableSmartScroll() {
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        verticalScrollBarMaximumValue = getVerticalScrollBar().getMaximum();
        getVerticalScrollBar().addAdjustmentListener(e -> {
            // Allow to scroll only by fixed length, which is font height:
            int val = e.getValue();
            if(terminal.getHeight() != 0)
                e.getAdjustable().setValue(val - val % (terminal.getFontMetrics(terminal.getFont()).getHeight()));
            //If on the bottom just make sure it scrolled to the end
            if ((verticalScrollBarMaximumValue - e.getAdjustable().getMaximum()) == 0) return;
            if (e.getValue() != e.getAdjustable().getMaximum() + e.getAdjustable().getVisibleAmount()) return;
            if (getHeight() > 0) e.getAdjustable().setValue(e.getAdjustable().getMaximum());
            verticalScrollBarMaximumValue = getVerticalScrollBar().getMaximum();
        });
        new SmartScroller(this);
    }

    /**
     * Auto border is applied to the terminal every time the component is resized.
     * It ensures that the bottom of the terminal is always aligned with the last line.
     */
    private void enableAutoBorder() {
        addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                int height = e.getComponent().getHeight()
                        - getBorder().getBorderInsets(JTerminal.this).bottom
                        - getBorder().getBorderInsets(JTerminal.this).top;
                int bottom = height % (terminal.getFontMetrics(terminal.getFont()).getHeight());
                terminal.setBorder(createEmptyBorder(0, 0, bottom, 0));
            }

            @Override
            public void componentMoved(ComponentEvent e) { }

            @Override
            public void componentShown(ComponentEvent e) { }

            @Override
            public void componentHidden(ComponentEvent e) { }
        });
    }

    private void disableArrowKeys() {
        String[] keys = {"UP", "DOWN", "LEFT", "RIGHT", "HOME", "ENTER", "TAB", "shift TAB"};
        for (String key : keys) {
            terminal.getInputMap().put(KeyStroke.getKeyStroke(key), "none");
            getInputMap().put(KeyStroke.getKeyStroke(key), "none");
            getVerticalScrollBar().getInputMap().put(KeyStroke.getKeyStroke(key), "none");
        }
        terminal.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, Collections.emptySet());
        terminal.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, Collections.emptySet());
        ActionMap am = getActionMap();
        am.put("scrollDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { }
        });
        am.put("scrollUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { }
        });
        am.put("unitScrollDown", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { }
        });
        am.put("unitScrollUp", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) { }
        });
    }

    public TerminalInputStream getTis() {
        return tis;
    }

    public TerminalOutputStream getTos() {
        return tos;
    }

    public void bindToSystemStreams() {
        System.setIn(tis);
        PrintStream printStream = new PrintStream(tos);
        System.setOut(printStream);
        System.setErr(printStream);
    }

    public static Font getDefaultFont(int size) {
        return DEFAULT_FONT_PROVIDER.getFont(size);
    }

    public void removeBorder() {
        setBorder(createEmptyBorder());
    }

    /**
     * Might cause performance degradation.
     */
    public void enableFontLigatures() {
        @SuppressWarnings("unchecked") // It's always Map<TextAttribute, Object>
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) terminal.getFont().getAttributes();
        attributes.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        terminal.setFont(terminal.getFont().deriveFont(attributes));
    }

    public void setTerminalFont(Font font) {
        terminal.setFont(font);
    }

    public void setTextColor(Color color) {
        terminal.setForeground(color);
        terminal.setCaretColor(color);
    }

    public void setBackgroundColor(Color color) {
        terminal.setBackground(color);
    }

    public void setSelectedTextColor(Color color) {
        terminal.setSelectedTextColor(color);
    }

    public void setSelectionColor(Color color) {
        terminal.setSelectionColor(color);
    }

    public TerminalController getTerminalController() {
        return controller;
    }

    private class JTerminalController implements TerminalController {
        private int caret = terminal.getCaretPosition();

        @Override
        public synchronized void detach(int lines){
            try {
                int max = terminal.getLineCount();
                if(lines > max) lines = max;
                int offset = terminal.getLineEndOffset(lines - 1);
                terminal.getDocument().remove(0, offset);
                caret -= offset;
                if(caret < 0) caret = 0;
                updateCaret();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        @Override
        public synchronized void append(String s){
            try {
                if(caret == terminal.getDocument().getLength())
                    terminal.append(s);
                else {
                    int end = Math.min(caret + s.length(), terminal.getDocument().getLength());
                    terminal.replaceRange(s, caret, end);
                }
                moveCaret(s.length());
            } catch (Error ignored) { }
        }

        @Override
        public synchronized void delete(int chars) {
            int start = caret - chars;
            if (chars > 0) {
                if(start < 0) start = 0;
                terminal.replaceRange(null, start, caret);
                moveCaret(-chars);
            } else if(chars < 0) {
                if(start > terminal.getDocument().getLength()) start = terminal.getDocument().getLength();
                terminal.replaceRange(null, caret, start);
            }
        }

        @Override
        public synchronized void clear() {
            terminal.setText("");
            caret = 0;
        }

        @Override
        public synchronized void setCaret(int pos) {
            caret = pos;
            if(caret < 0) caret = 0;
            if(caret > terminal.getDocument().getLength()) caret = terminal.getDocument().getLength();
            updateCaret();
        }

        @Override
        public synchronized void moveCaret(int offset){
            caret += offset;
            if(caret < 0) caret = 0;
            if(caret > terminal.getDocument().getLength()) caret = terminal.getDocument().getLength();
            updateCaret();
        }

        @Override
        public synchronized void moveCaretToLineStart(){
            try {
                int line = terminal.getLineOfOffset(caret);
                caret = terminal.getLineStartOffset(line);
                updateCaret();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        @Override
        public synchronized void moveCaretToLineEnd(){
            try {
                int line = terminal.getLineOfOffset(caret);
                caret = terminal.getLineEndOffset(line);
                updateCaret();
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }

        @Override
        public synchronized int getLines() {
            return terminal.getLineCount();
        }

        @Override
        public synchronized int getCaret() {
            return terminal.getCaretPosition();
        }

        @Override
        public synchronized String getText() {
            return terminal.getText();
        }

        private void updateCaret() {
            //terminal.setCaretPosition(caret);
        }
    }

    /**
     *  The SmartScroller will attempt to keep the viewport positioned based on
     *  the users interaction with the scrollbar. The normal behaviour is to keep
     *  the viewport positioned to see new data as it is dynamically added.
     *
     *  Assuming vertical scrolling and data is added to the bottom:
     *
     *  - when the viewport is at the bottom and new data is added,
     *    then automatically scroll the viewport to the bottom
     *  - when the viewport is not at the bottom and new data is added,
     *    then do nothing with the viewport
     *
     *  Assuming vertical scrolling and data is added to the top:
     *
     *  - when the viewport is at the top and new data is added,
     *    then do nothing with the viewport
     *  - when the viewport is not at the top and new data is added, then adjust
     *    the viewport to the relative position it was at before the data was added
     *
     *  Similiar logic would apply for horizontal scrolling.
     */
    private static class SmartScroller implements AdjustmentListener
    {
        public final static int HORIZONTAL = 0;
        public final static int VERTICAL = 1;

        public final static int START = 0;
        public final static int END = 1;

        private final int viewportPosition;

        private boolean adjustScrollBar = true;

        private int previousValue = -1;
        private int previousMaximum = -1;

        /**
         *  Convenience constructor.
         *  Scroll direction is VERTICAL and viewport position is at the END.
         *
         *  @param scrollPane the scroll pane to monitor
         */
        public SmartScroller(JScrollPane scrollPane)
        {
            this(scrollPane, VERTICAL, END);
        }

        /**
         *  Convenience constructor.
         *  Scroll direction is VERTICAL.
         *
         *  @param scrollPane the scroll pane to monitor
         *  @param viewportPosition valid values are START and END
         */
        public SmartScroller(JScrollPane scrollPane, int viewportPosition)
        {
            this(scrollPane, VERTICAL, viewportPosition);
        }

        /**
         *  Specify how the SmartScroller will function.
         *
         *  @param scrollPane the scroll pane to monitor
         *  @param scrollDirection indicates which JScrollBar to monitor.
         *                         Valid values are HORIZONTAL and VERTICAL.
         *  @param viewportPosition indicates where the viewport will normally be
         *                          positioned as data is added.
         *                          Valid values are START and END
         */
        public SmartScroller(JScrollPane scrollPane, int scrollDirection, int viewportPosition)
        {
            if (scrollDirection != HORIZONTAL
                    &&  scrollDirection != VERTICAL)
                throw new IllegalArgumentException("invalid scroll direction specified");

            if (viewportPosition != START
                    &&  viewportPosition != END)
                throw new IllegalArgumentException("invalid viewport position specified");

            this.viewportPosition = viewportPosition;

            JScrollBar scrollBar;
            if (scrollDirection == HORIZONTAL)
                scrollBar = scrollPane.getHorizontalScrollBar();
            else
                scrollBar = scrollPane.getVerticalScrollBar();

            scrollBar.addAdjustmentListener( this );

            //  Turn off automatic scrolling for text components

            Component view = scrollPane.getViewport().getView();

            if (view instanceof JTextComponent)
            {
                JTextComponent textComponent = (JTextComponent)view;
                DefaultCaret caret = (DefaultCaret)textComponent.getCaret();
                caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);
            }
        }

        @Override
        public void adjustmentValueChanged(final AdjustmentEvent e)
        {
            SwingUtilities.invokeLater(() -> checkScrollBar(e));
        }

        /*
         *  Analyze every adjustment event to determine when the viewport
         *  needs to be repositioned.
         */
        private void checkScrollBar(AdjustmentEvent e)
        {
            //  The scroll bar listModel contains information needed to determine
            //  whether the viewport should be repositioned or not.

            JScrollBar scrollBar = (JScrollBar)e.getSource();
            BoundedRangeModel listModel = scrollBar.getModel();
            int value = listModel.getValue();
            int extent = listModel.getExtent();
            int maximum = listModel.getMaximum();

            boolean valueChanged = previousValue != value;
            boolean maximumChanged = previousMaximum != maximum;

            //  Check if the user has manually repositioned the scrollbar

            if (valueChanged && !maximumChanged)
            {
                if (viewportPosition == START)
                    adjustScrollBar = value != 0;
                else
                    adjustScrollBar = value + extent >= maximum;
            }

            //  Reset the "value" so we can reposition the viewport and
            //  distinguish between a user scroll and a program scroll.
            //  (ie. valueChanged will be false on a program scroll)

            if (adjustScrollBar && viewportPosition == END)
            {
                //  Scroll the viewport to the end.
                scrollBar.removeAdjustmentListener( this );
                value = maximum - extent;
                scrollBar.setValue( value );
                scrollBar.addAdjustmentListener( this );
            }

            if (adjustScrollBar && viewportPosition == START)
            {
                //  Keep the viewport at the same relative viewportPosition
                scrollBar.removeAdjustmentListener( this );
                value = value + maximum - previousMaximum;
                scrollBar.setValue( value );
                scrollBar.addAdjustmentListener( this );
            }

            previousValue = value;
            previousMaximum = maximum;
        }
    }


}
