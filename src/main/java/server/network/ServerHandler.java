package server.network;

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

public class ServerHandler implements Runnable {
    private final Socket socket;

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (Socket client = socket;
             BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
            String raw = input.readLine();
            if (raw == null || raw.isBlank()) {
                output.write(JsonCodec.toJson(SocketResponse.error("Yeu cau khong hop le.").toMap()));
                output.newLine();
                output.flush();
                return;
            }

            Object decoded = JsonCodec.fromJson(raw);
            if (!(decoded instanceof Map<?, ?> values)) {
                output.write(JsonCodec.toJson(SocketResponse.error("Yeu cau khong hop le.").toMap()));
                output.newLine();
                output.flush();
                return;
            }

            SocketRequest request = SocketRequest.fromMap((Map<String, Object>) values);
            if ("SUBSCRIBE_EVENTS".equalsIgnoreCase(request.getAction())) {
                output.write(JsonCodec.toJson(SocketResponse.ok("Subscribed", null).toMap()));
                output.newLine();
                output.flush();
                ClientSubscriptionRegistry.register(request.getActorUsername(), client, output);
                while (input.readLine() != null) {
                    // Keep the socket alive until the client disconnects.
                }
                ClientSubscriptionRegistry.unregister(client);
                return;
            }

            SocketResponse response = new MessageRouter().route(request);
            output.write(JsonCodec.toJson(response.toMap()));
            output.newLine();
            output.flush();
        } catch (Exception ignored) {
            ClientSubscriptionRegistry.unregister(socket);
        }
    }
}
