package com.hermant.terminal;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class FontProvider {

    private final Font font;

    public FontProvider(String resource) {
        Font temp;
        try (InputStream stream = ClassLoader.getSystemClassLoader().getResourceAsStream(resource)) {
            if(stream == null) throw new IOException("Couldn't open stream");
            temp = Font.createFont(Font.TRUETYPE_FONT, stream);
        } catch (IOException | FontFormatException e) {
            e.printStackTrace();
            temp = new Font(Font.MONOSPACED, Font.PLAIN, 24);
        }
        font = temp;
    }

    public Font getFont(int size) {
        return font.deriveFont((float)size);
    }
}
