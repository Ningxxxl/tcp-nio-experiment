import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;
import java.util.Timer;
import java.util.concurrent.*;

/**
 * @author ningxy
 */
public class Server {

    private int port;
    private final ServerSocketChannel channel = ServerSocketChannel.open();
    private final ByteBuffer buffer = ByteBuffer.allocate(16);
    private ByteBuffer resultBuffer = ByteBuffer.allocate(0);
    private final Selector selector = Selector.open();

    private static final ThreadPoolExecutor THREAD_POOL_EXECUTOR = new ThreadPoolExecutor(
            2,
            2,
            1000,
            TimeUnit.MILLISECONDS,
            new LinkedBlockingQueue<>(),
            Executors.defaultThreadFactory(),
            new ThreadPoolExecutor.AbortPolicy()
    );

    public static void main(String[] args) throws IOException {
        int port = 6969;
        if (args.length > 0) {
            String p = args[0];
            try {
               port = Integer.parseInt(p);
            } catch (NumberFormatException e) {
                System.err.println(e.getMessage());
                System.exit(1);
            }
        }
        Server server = new Server(port);
        server.start();
    }

    private Server() throws IOException {
    }

    private Server(int port) throws IOException {
        this.port = port;
    }

    public void start() throws IOException {
        System.out.println("Server start...");

        channel.socket().bind(new InetSocketAddress(port));
        channel.configureBlocking(false);
        channel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            selector.select();
            Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
            while (iterator.hasNext()) {
                SelectionKey selectionKey = iterator.next();
                if (selectionKey.isAcceptable()) {
                    register();
                }

                if (selectionKey.isReadable()) {
                    SocketChannel client = (SocketChannel) selectionKey.channel();

                    int read = client.read(buffer);
                    if (read == -1) {
                        disconnect(client);
                        continue;
                    }

                    THREAD_POOL_EXECUTOR.execute(() -> {
                        try {
                            getData(client);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    });
                }
                iterator.remove();
            }
        }
    }

    private void register() throws IOException {
        SocketChannel clientChannel = channel.accept();
        System.out.printf("Client [%s] Connected. %n", clientChannel.getRemoteAddress());
        clientChannel.configureBlocking(false);
        clientChannel.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
    }

    private void getData(SocketChannel client) throws IOException {
        boolean isCompleted = buffer.limit() - buffer.position() != 0;

        resultBuffer = ByteBuffer.allocate(resultBuffer.position() + buffer.position())
                .put(resultBuffer.array(), 0, resultBuffer.position())
                .put(buffer.array(), 0, buffer.position());

        if (isCompleted) {
            String clientData = new String(resultBuffer.array());
            String returnData = String.format("[%s] Server received data: [%s]", LocalDateTime.now(), clientData);
            System.out.printf("[%s] Received data from [%s]: [%s] %n", Thread.currentThread().getName(), client.getRemoteAddress(), clientData);
            resultBuffer.flip();
            client.write(StandardCharsets.UTF_8.encode(returnData));
            resultBuffer.clear();
        }
        buffer.clear();
    }

    private static void disconnect(SocketChannel client) throws IOException {
        System.out.printf("Client [%s] Disconnect. %n", client.getRemoteAddress());
        client.close();
    }
}
