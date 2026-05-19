package server;

import server.network.SocketAuctionServer;

public class ServerLauncherMain {
    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getProperty(
                "auction.server.port",
                System.getenv().getOrDefault("AUCTION_SERVER_PORT", "5050")
        ));

        // Khoi tao DB va load data truoc khi mo cong - neu loi thi bao ro va thoat
        System.out.println("[Server] Dang ket noi database va khoi tao du lieu tại port " + port);
        try {
            ServerMain.getInstance(); // kích hoạt singleton ngay, bắt lỗi DB sớm
            System.out.println("[Server] Database OK.");
        } catch (Exception ex) {
            System.err.println("[Server] KHONG THE KHOI DONG: " + ex.getMessage());
            if (ex.getCause() != null) {
                System.err.println("  Nguyen nhan: " + ex.getCause().getMessage());
            }
            System.err.println("[Server] Kiem tra lai config/database.properties.");
            System.exit(1);
        }

        SocketAuctionServer.ensureStarted(port);
        System.out.println("[Server] Dang lang nghe tren cong " + port + ". San sang nhan ket noi.");
        Thread.currentThread().join();
    }
}
