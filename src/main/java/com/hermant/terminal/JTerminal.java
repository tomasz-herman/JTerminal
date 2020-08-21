package com.hermant.terminal;

import com.hermant.terminal.io.LineBufferedTerminalInputStream;
import com.hermant.terminal.io.NonBufferedTerminalInputStream;
import com.hermant.terminal.io.TerminalInputStream;
import com.hermant.terminal.io.TerminalOutputStream;

import javax.swing.*;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.font.TextAttribute;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Map;

import static javax.swing.BorderFactory.createEmptyBorder;

public class JTerminal extends JScrollPane {

    private static final String DEFAULT_FONT_NAME = "FiraCode-Regular.ttf";
    private static final Font DEFAULT_FONT;

    static {
        Font temp;
        try (InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(DEFAULT_FONT_NAME)) {
            if(stream == null) throw new IOException("Couldn't open stream");
            temp = Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException e) {
            temp = new Font(Font.MONOSPACED, Font.PLAIN, 24);
        }
        DEFAULT_FONT = temp;
    }

    private final TerminalInputStream tis;
    private final TerminalOutputStream tos;
    private int verticalScrollBarMaximumValue;

    private final JTextArea terminal = new JTextArea(24, 80);

    public JTerminal(boolean bufferedInStream) {
        tis = bufferedInStream ? new LineBufferedTerminalInputStream(this) : new NonBufferedTerminalInputStream(this);
        tos = new TerminalOutputStream(terminal);

        setViewportView(terminal);
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
            e.getAdjustable().setValue(e.getAdjustable().getMaximum());
            verticalScrollBarMaximumValue = getVerticalScrollBar().getMaximum();
        });
        new SmartScroller(this);

        terminal.setLineWrap(true);
        terminal.setFont(getDefaultFont(24));
        terminal.addKeyListener(tis);

        setBorder(createEmptyBorder());

        disableArrowKeys();
        terminal.setEditable(false);
    }

    private void disableArrowKeys() {
        String[] keys = {"UP", "DOWN", "LEFT", "RIGHT", "HOME", "ENTER"};
        for (String key : keys) {
            terminal.getInputMap().put(KeyStroke.getKeyStroke(key), "none");
        }
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
//        System.setErr(printStream);
    }

    public static Font getDefaultFont(int size) {
        return DEFAULT_FONT.deriveFont((float)size);
    }

    /**
     * Might cause performance degradation.
     */
    public void enableFontLigatures() {
        @SuppressWarnings("unchecked") // It's always Map<TextAttribute, Object>
        Map<TextAttribute, Object> attributes = (Map<TextAttribute, Object>) terminal.getFont().getAttributes();
        attributes.put(TextAttribute.LIGATURES, TextAttribute.LIGATURES_ON);
        terminal.setFont(DEFAULT_FONT.deriveFont(attributes));
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
