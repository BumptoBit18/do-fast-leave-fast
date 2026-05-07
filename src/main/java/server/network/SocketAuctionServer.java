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

    private SocketAuctionServer() {
    }

    public static void ensureStarted(int port) {
        if (STARTED.compareAndSet(false, true)) {
            executor = Executors.newCachedThreadPool();
            Thread serverThread = new Thread(() -> runServer(port), "auction-socket-server");
            serverThread.setDaemon(true);
            serverThread.start();
        }
    }

    private static void runServer(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                executor.submit(new ServerHandler(socket));
            }
        } catch (IOException ex) {
            STARTED.set(false);
            throw new IllegalStateException("Khong the khoi dong socket server o cong " + port, ex);
        }
    }
}
