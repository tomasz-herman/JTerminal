package com.hermant;

import com.hermant.io.LineBufferedTerminalInputStream;
import com.hermant.io.NonBufferedTerminalInputStream;
import com.hermant.io.TerminalInputStream;
import com.hermant.io.TerminalOutputStream;

import javax.swing.*;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;
import java.awt.*;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;

import static javax.swing.BorderFactory.createEmptyBorder;

public class JTerminal extends JScrollPane {

    private static final String DEFAULT_FONT_NAME = "NotoMono-Regular.ttf";
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

    private Theme theme = new Theme(DEFAULT_DARK);

    private final JTextArea terminal = new JTextArea(24, 80);

    public JTerminal(boolean bufferedInStream) {
        tis = bufferedInStream ? new LineBufferedTerminalInputStream(this) : new NonBufferedTerminalInputStream(this);
        tos = new TerminalOutputStream(terminal);

        setViewportView(terminal);
        setVerticalScrollBarPolicy(VERTICAL_SCROLLBAR_ALWAYS);
        verticalScrollBarMaximumValue = getVerticalScrollBar().getMaximum();
        getVerticalScrollBar().addAdjustmentListener(e -> {
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
        theme.apply(this);

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

    private static final Theme DEFAULT_LIGHT = new Theme(
            getDefaultFont(24),
            new Color(51, 51, 51),
            new Color(255, 255, 255),
            new Color(184, 207, 229),
            new Color(51, 51, 51),
            new Color(238, 238, 238),
            new Color(188, 203, 218)
    );
    private static final Theme DEFAULT_DARK = new Theme(
            getDefaultFont(24),
            new Color(204, 204, 204),
            new Color(0, 0, 0),
            new Color(5, 37, 68),
            new Color(204, 204, 204),
            new Color(12, 12, 12),
            new Color(64, 112, 158)
    );
//    private static final Theme BREEZE_LIGHT = new Theme();
    private static final Theme BREEZE_DARK = new Theme(
            getDefaultFont(24),
            new Color(239, 240, 241),
            new Color(49, 54, 59),
            new Color(61, 174, 233),
            new Color(252, 252, 252),
            new Color(189, 195, 199),
            new Color(61, 174, 233)
);

    public static class Theme {

        private Font font;
        private Color scrollArea;
        private Color scrollBar;
        private Color foreground;
        private Color background;
        private Color highlight;
        private Color selected;

        private Theme(Font font, Color foreground, Color background, Color highlight, Color selected, Color scrollArea, Color scrollBar) {
            this.font = font;
            this.foreground = foreground;
            this.background = background;
            this.highlight = highlight;
            this.selected = selected;
            this.scrollArea = scrollArea;
            this.scrollBar = scrollBar;
        }

        private Theme(Theme other) {
            this.font = other.font;
            this.foreground = other.foreground;
            this.background = other.background;
            this.highlight = other.highlight;
            this.selected = other.selected;
            this.scrollArea = other.scrollArea;
            this.scrollBar = other.scrollBar;
        }

        public Theme(JTerminal jTerminal) {
            this.font = jTerminal.theme.font;
            this.foreground = jTerminal.theme.foreground;
            this.background = jTerminal.theme.background;
            this.highlight = jTerminal.theme.highlight;
            this.selected = jTerminal.theme.selected;
        }

        public void apply(JTerminal jTerminal){
            jTerminal.terminal.setFont(font);
            jTerminal.setBackground(background);
            jTerminal.setForeground(foreground);
            jTerminal.terminal.setForeground(foreground);
            jTerminal.terminal.setBackground(background);
            jTerminal.getVerticalScrollBar().setBackground(scrollArea);
            jTerminal.getVerticalScrollBar().setUI(new BasicScrollBarUI() {
                @Override
                protected void configureScrollBarColors() {
                    this.thumbColor = scrollBar;
                }
            });
            jTerminal.terminal.setSelectionColor(highlight);
            jTerminal.terminal.setSelectedTextColor(selected);
            jTerminal.theme = this;
        }

        public Font getFont() {
            return font;
        }

        public Theme setFont(Font font) {
            this.font = font;
            return this;
        }

        public Color getForeground() {
            return foreground;
        }

        public Theme setForeground(Color foreground) {
            this.foreground = foreground;
            return this;
        }

        public Color getBackground() {
            return background;
        }

        public Theme setBackground(Color background) {
            this.background = background;
            return this;
        }

        public Color getHighlight() {
            return highlight;
        }

        public Theme setHighlight(Color highlight) {
            this.highlight = highlight;
            return this;
        }

        public Color getSelected() {
            return selected;
        }

        public Theme setSelected(Color selected) {
            this.selected = selected;
            return this;
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
