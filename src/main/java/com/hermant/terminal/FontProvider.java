package com.hermant.terminal;

import java.awt.*;
import java.io.IOException;
import java.io.InputStream;

public class FontProvider {

    public static final String ANONYMOUS_PRO = "Anonymous_Pro.ttf";
    public static final String DEJAVU_SANS_MONO = "DejaVuSansMono.ttf";
    public static final String DROID_SANS_MONO = "DroidSansMono.ttf";
    public static final String FIRA_CODE = "FiraCode-Regular.ttf";
    public static final String INCONSOLATA = "Inconsolata.otf";
    public static final String LIBERATION_MONO = "LiberationMono-Regular.ttf";
    public static final String NOTO_MONO = "NotoMono-Regular.ttf";
    public static final String UBUNTU_MONO = "UbuntuMono-R.ttf";
    public static final String VERA_MONO = "VeraMono.ttf";

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
