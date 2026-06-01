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
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
             BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8))) {
            String raw = input.readLine();
            if (raw == null || raw.isBlank()) {
                writeResponse(output, SocketResponse.error("Yeu cau khong hop le."));
                return;
            }

            Object decoded = JsonCodec.fromJson(raw);
            if (!(decoded instanceof Map<?, ?> values)) {
                writeResponse(output, SocketResponse.error("Yeu cau khong hop le."));
                return;
            }

            SocketRequest request = SocketRequest.fromMap(asStringObjectMap(values));
            if ("SUBSCRIBE_EVENTS".equalsIgnoreCase(request.getAction())) {
                handleSubscription(client, input, output, request);
                return;
            }

            SocketResponse response = new MessageRouter().route(request);
            writeResponse(output, response);
        } catch (Exception ex) {
            System.err.println("Server handler failed: " + ex.getMessage());
            ex.printStackTrace(System.err);
        } finally {
            ClientSubscriptionRegistry.unregister(socket);
        }
    }

    private void handleSubscription(Socket client, BufferedReader input, BufferedWriter output, SocketRequest request) throws Exception {
        ClientSubscriptionRegistry.register(request.getActorUsername(), client, output);
        writeResponse(output, SocketResponse.ok("Subscribed", null));

        while (!client.isClosed()) {
            String keepAlive = input.readLine();
            if (keepAlive == null) {
                return;
            }
        }
    }

    private void writeResponse(BufferedWriter output, SocketResponse response) throws Exception {
        output.write(JsonCodec.toJson(response.toMap()));
        output.newLine();
        output.flush();
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
