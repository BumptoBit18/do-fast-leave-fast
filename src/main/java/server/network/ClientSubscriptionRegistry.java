package server.network;

import shared.json.JsonCodec;
import shared.socket.RealtimeEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ClientSubscriptionRegistry {
    private static final List<ClientSubscription> SUBSCRIPTIONS = new CopyOnWriteArrayList<>();

    private ClientSubscriptionRegistry() {
    }

    public static void register(String username, Socket socket, BufferedWriter writer) {
        SUBSCRIPTIONS.add(new ClientSubscription(username, socket, writer));
        System.out.printf("[Registry] Client %s đã kết nối. Tổng: %d\n",username, SUBSCRIPTIONS.size());
    }

    public static void unregister(Socket socket) {
        SUBSCRIPTIONS.removeIf(subscription -> subscription.socket() == socket);
    }

    public static void broadcast(RealtimeEvent event) {
        List<ClientSubscription> failedSubscriptions = new ArrayList<>();

        for (ClientSubscription subscription: SUBSCRIPTIONS){
            if (!matches(subscription.username(), event.username())){
                continue;
            }

            try {
                subscription.writer().write(JsonCodec.toJson(Map.of(
                        "type", event.type(),
                        "username", event.username() == null ? "" : event.username(),
                        "auctionId", event.auctionId() == null ? "" : event.auctionId()
                )));

                subscription.writer().newLine();
                subscription.writer().flush();
            } catch (IOException ex) {
                // Client bị ngắt kết nối — gom lại xóa sau
                failedSubscriptions.add(subscription);
                closeQuietly(subscription.socket());
            }
        }

        // Xóa các client đã mất kết nối sau khi duyệt xong
        if (!failedSubscriptions.isEmpty()) {
            SUBSCRIPTIONS.removeAll(failedSubscriptions);
        }
    }

    private static boolean matches(String subscribedUser, String eventUser) {
        return eventUser == null
                || eventUser.isBlank()
                || "ALL".equalsIgnoreCase(eventUser)
                || subscribedUser == null
                || subscribedUser.equalsIgnoreCase(eventUser);
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private record ClientSubscription(String username, Socket socket, BufferedWriter writer) {
    }
}
