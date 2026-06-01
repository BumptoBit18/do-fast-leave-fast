package server.network;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public final class SocketAuctionServer {
    private static final AtomicBoolean STARTED = new AtomicBoolean(false);
    private static ExecutorService executor;

    private static final int MAX_CONCURRENT_CLIENTS = 50;

    private SocketAuctionServer() {
    }

    public static void ensureStarted(int port) {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }

        try {
            ServerSocket serverSocket = new ServerSocket(port);
            executor = Executors.newFixedThreadPool(MAX_CONCURRENT_CLIENTS);
            Thread serverThread = new Thread(() -> runServer(serverSocket), "auction-socket-server");
            serverThread.setDaemon(true);
            serverThread.start();
            System.out.println("[Server] Dang lang nghe tai cong " + port + ", toi da " + MAX_CONCURRENT_CLIENTS + " client cung luc.");
        } catch (IOException ex) {
            STARTED.set(false);
            throw new IllegalStateException("Khong the khoi dong socket server o cong " + port + ". Cong nay co the dang bi chiem.", ex);
        }
    }

    private static void runServer(ServerSocket serverSocket) {
        try (serverSocket) {
            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(new ServerHandler(socket));
            }
        } catch (IOException ex) {
            STARTED.set(false);
            throw new IllegalStateException("Socket server da dung bat ngo.", ex);
        }
    }
}
