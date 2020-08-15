import com.hermant.JTerminal;
import com.hermant.swing.WindowBuilder;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Main {
    public static void main(String[] args) {
        JTerminal terminal = new JTerminal(false);
        new WindowBuilder()
            .setContentPane(terminal)
            .setSize(1280, 720)
            .buildFrame();
        terminal.bindToSystemStreams();
        Executors.newScheduledThreadPool(1).scheduleAtFixedRate(
                () -> System.out.print(UUID.randomUUID().toString() + (new Random().nextDouble() > 0.1 ? "" : '\n')),
                0, 500, TimeUnit.MILLISECONDS);
    }
}
