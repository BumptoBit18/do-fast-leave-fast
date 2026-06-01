package network;

import shared.json.JsonCodec;
import shared.socket.SocketRequest;
import shared.socket.SocketResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;

final class ServerEventClient {
    private final String serverHost;
    private final int serverPort;
    private final MessageListener listener;

    private Socket socket;
    private Thread listenerThread;

    ServerEventClient(String serverHost, int serverPort, MessageListener listener) {
        this.serverHost = serverHost;
        this.serverPort = serverPort;
        this.listener = listener;
    }

    synchronized void connect(String username) {
        disconnect();

        listenerThread = new Thread(() -> listen(username), "auction-event-listener");
        listenerThread.setDaemon(true);
        listenerThread.start();
    }

    synchronized void disconnect() {
        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {
            }
            socket = null;
        }
        listenerThread = null;
    }

    private void listen(String username) {
        try (Socket connection = new Socket(serverHost, serverPort);
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(connection.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader input = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
            synchronized (this) {
                socket = connection;
            }

            SocketRequest request = new SocketRequest();
            request.setAction("SUBSCRIBE_EVENTS");
            request.setActorUsername(username);
            output.write(JsonCodec.toJson(request.toMap()));
            output.newLine();
            output.flush();

            String ack = input.readLine();
            if (ack == null) {
                return;
            }

            Object ackValue = JsonCodec.fromJson(ack);
            if (ackValue instanceof Map<?, ?> ackMap) {
                SocketResponse response = SocketResponse.fromMap(asStringObjectMap(ackMap));
                if (!response.isSuccess()) {
                    return;
                }
            }

            String raw;
            while ((raw = input.readLine()) != null) {
                Object decoded = JsonCodec.fromJson(raw);
                if (!(decoded instanceof Map<?, ?> values)) {
                    continue;
                }
                listener.onMessage(new shared.socket.RealtimeEvent(
                        stringValue(values.get("type")),
                        stringValue(values.get("username")),
                        stringValue(values.get("auctionId"))
                ));
            }
        } catch (Exception ignored) {
        } finally {
            synchronized (this) {
                socket = null;
            }
        }
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private Map<String, Object> asStringObjectMap(Map<?, ?> values) {
        Map<String, Object> typedValues = new java.util.LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() != null) {
                typedValues.put(String.valueOf(entry.getKey()), entry.getValue());
            }
        }
        return typedValues;
    }
}
