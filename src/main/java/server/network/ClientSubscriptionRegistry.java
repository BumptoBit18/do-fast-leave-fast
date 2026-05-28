package server.network;

import shared.json.JsonCodec;
import shared.socket.RealtimeEvent;

import java.io.BufferedWriter;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class ClientSubscriptionRegistry {

    private ClientSubscriptionRegistry() {
    }

        SUBSCRIPTIONS.add(new ClientSubscription(username, socket, writer));
    }

        SUBSCRIPTIONS.removeIf(subscription -> subscription.socket() == socket);
    }

            if (!matches(subscription.username(), event.username())){
                continue;
            }
            try {
                subscription.writer().newLine();
                subscription.writer().flush();
            } catch (IOException ex) {
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
