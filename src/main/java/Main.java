import com.hermant.JTerminal;
import com.hermant.swing.WindowBuilder;

public class Main {
    public static void main(String[] args) {
        JTerminal terminal = new JTerminal();
        WindowBuilder builder = new WindowBuilder();
        builder.setContentPane(terminal);
        builder.setSize(1280, 720);
        builder.buildFrame();
    }
}
