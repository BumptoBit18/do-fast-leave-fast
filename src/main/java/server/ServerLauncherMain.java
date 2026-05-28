package server;

import server.network.SocketAuctionServer;

public class ServerLauncherMain {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty(
                "auction.server.port",
                System.getenv().getOrDefault("AUCTION_SERVER_PORT", "5050")
        ));
        SocketAuctionServer.ensureStarted(port);
        System.out.println("Auction socket server is running on port " + port);
        Thread.currentThread().join();
    }
}
