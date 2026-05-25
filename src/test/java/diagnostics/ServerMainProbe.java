package diagnostics;

import server.ServerMain;

public final class ServerMainProbe {
    private ServerMainProbe() {
    }

    public static void main(String[] args) {
        try {
            ServerMain.getInstance();
            System.out.println("SERVER_MAIN_OK");
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            System.exit(1);
        }
    }
}
