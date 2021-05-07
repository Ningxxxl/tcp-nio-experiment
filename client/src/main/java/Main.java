import java.io.IOException;
import java.util.Scanner;

/**
 * @author ningxy
 */
public class Main {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.err.println("Error Input.");
        }
        String hostname = args[0];
        int port = Integer.parseInt(args[1]);
        Client client1 = Client.start(hostname, port);

        Scanner s = new Scanner(System.in);
        while (true) {
            System.out.print(">>>");
            String line = s.nextLine();
            if ("exit".equalsIgnoreCase(line)) {
                Client.stop();
                break;
            }
            client1.sendMessage(line);
        }
    }
}
