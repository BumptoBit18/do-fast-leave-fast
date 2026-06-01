package server.network;

import shared.json.JsonCodec;
import shared.socket.RealtimeEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class ClientSubscriptionRegistry {
    private static final List<ClientSubscription> SUBSCRIPTIONS = new ArrayList<>();

    private ClientSubscriptionRegistry() {
    }

    public static synchronized void register(String username, Socket socket, BufferedWriter writer) {
        SUBSCRIPTIONS.add(new ClientSubscription(username, socket, writer));
    }

    public static synchronized void unregister(Socket socket) {
        SUBSCRIPTIONS.removeIf(subscription -> subscription.socket() == socket);
    }

    public static synchronized void broadcast(RealtimeEvent event) {
        Iterator<ClientSubscription> iterator = SUBSCRIPTIONS.iterator();
        while (iterator.hasNext()) {
            ClientSubscription subscription = iterator.next();
            if (!matches(subscription.username(), event.username())) {
                continue;
            }
            try {
                Map<String, Object> payload = new LinkedHashMap<>();
                payload.put("type", event.type());
                payload.put("username", event.username());
                payload.put("auctionId", event.auctionId());
                subscription.writer().write(JsonCodec.toJson(payload));
                subscription.writer().newLine();
                subscription.writer().flush();
            } catch (IOException ex) {
                iterator.remove();
                closeQuietly(subscription.socket());
            }
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
