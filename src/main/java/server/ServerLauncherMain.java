package server;

import server.network.SocketAuctionServer;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ServerLauncherMain {
    private static final ScheduledExecutorService BACKGROUND_WORKER = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "auction-background-worker");
        thread.setDaemon(true);
        return thread;
    });

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty(
                "auction.server.port",
                System.getenv().getOrDefault("AUCTION_SERVER_PORT", "5050")
        ));
        // Force backend initialization before opening the socket port so startup failures
        // are visible immediately instead of surfacing as invalid socket responses later.
        ServerMain server = ServerMain.getInstance();
        SocketAuctionServer.ensureStarted(port);
        BACKGROUND_WORKER.scheduleAtFixedRate(() -> {
            try {
                server.getAuctionController().closeExpiredAuctions();
                server.processApprovedTopUpCredits();
            } catch (Exception ex) {
                System.err.println("Background worker failed: " + ex.getMessage());
                ex.printStackTrace(System.err);
            }
        }, 1, 1, TimeUnit.SECONDS);
        System.out.println("Auction socket server is running on port " + port);
        Thread.currentThread().join();
    }
}
