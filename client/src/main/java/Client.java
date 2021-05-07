import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * @author ningxy
 */
public class Client {
    private static SocketChannel server;
    private static ByteBuffer buffer;
    private static Client instance;
    private static StringBuilder sb;

    public static Client start(String hostname, int port) {
        if (instance == null) {
            instance = new Client(hostname, port);
        }

        return instance;
    }

    public static void stop() throws IOException {
        System.out.printf("Disconnect to server [%s] %n", server.getRemoteAddress());
        server.close();
        buffer = null;
    }

    private Client() {
    }

    private Client(String hostname, int port) {
        try {
            server = SocketChannel.open(new InetSocketAddress(hostname, port));
            server.configureBlocking(false);
            buffer = ByteBuffer.allocate(256);
            sb = new StringBuilder();
            System.out.printf("Connect to server [%s] %n", server.getRemoteAddress());
        } catch (IOException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
    }

    public String sendMessage(String msg) throws IOException {
        buffer = ByteBuffer.wrap(msg.getBytes());
        String response = null;
        server.write(buffer);
        buffer.clear();
        int read = 0;
        boolean readFlag = false;
        while (!readFlag) {
            while ((read = server.read(buffer)) > 0) {
                readFlag = true;
                buffer.flip();
                sb.append(StandardCharsets.UTF_8.decode(buffer));
                buffer.clear();
            }

            if (read == -1) {
                System.out.println("Server offline.");
            }
        }

        response = sb.toString();
        sb.setLength(0);
        System.out.printf("Response from server[%s]: [%s] %n", server.getRemoteAddress(), response);
        buffer.clear();

        return response;
    }
}