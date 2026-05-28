package server;

import server.network.SocketAuctionServer;

public class ServerLauncherMain {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty(
                "auction.server.port",
                System.getenv().getOrDefault("AUCTION_SERVER_PORT", "5050")
        ));

        // Khoi tao DB va load du lieu TRUOC khi mo port
        // Neu DB loi thi bao ro rang va thoat, khong de crash am tham khi co request
        System.out.println("[Server] Dang ket noi database va khoi tao du lieu...");
        try {
            ServerMain.getInstance();
            System.out.println("[Server] Database OK. Du lieu da tai xong.");
        } catch (Exception ex) {
            System.err.println("[Server] KHOI DONG THAT BAI: " + ex.getMessage());
            Throwable cause = ex.getCause();
            while (cause != null) {
                System.err.println("  Nguyen nhan: " + cause.getMessage());
                cause = cause.getCause();
            }
            System.err.println("[Server] Kiem tra lai config/database.properties.");
            System.exit(1);
        }

        SocketAuctionServer.ensureStarted(port);
        System.out.println("[Server] San sang, dang lang nghe tren cong " + port + ".");
        Thread.currentThread().join();
    }
}
