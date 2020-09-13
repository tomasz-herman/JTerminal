import com.hermant.terminal.JTerminal;
import com.hermant.swing.WindowBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Main {
    public static void main(String[] args) {
        JTerminal terminal = new JTerminal();
        new WindowBuilder()
            .setContentPane(terminal)
            .buildFrame();
        try {
            Process p = new ProcessBuilder()
                    .command(new String[] {"/usr/bin/script", "-qfc", "/usr/bin/bash", "/dev/null"})
                    .start();
            new Thread(() -> {
                InputStream in = p.getInputStream();
                int read;
                try {
                    while((read = in.read()) != -1) {
//                        System.out.println(read);
                        terminal.getTos().write(read);
                        terminal.getTos().flush();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            new Thread(() -> {
                OutputStream out = p.getOutputStream();
                int read;
                while((read = terminal.getTis().read()) != -1) {
                    try {
//                        System.out.println(read);
                        out.write(read);
                        out.flush();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            p.waitFor();
            Runtime.getRuntime().addShutdownHook(new Thread(p::destroy));
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}
