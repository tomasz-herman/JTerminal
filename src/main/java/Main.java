import com.hermant.JTerminal;
import com.hermant.swing.WindowBuilder;

public class Main {
    public static void main(String[] args) {
        JTerminal terminal = new JTerminal();
        new WindowBuilder()
            .setContentPane(terminal)
            .setSize(1280, 720)
            .buildFrame();
        terminal.bindToSystemStreams();
        for (int j = 0; j < 1; j++) {
            new Thread(() -> {
                int k = 0;
                while(true) System.out.println(k++);
            }).start();
        }
    }
}
