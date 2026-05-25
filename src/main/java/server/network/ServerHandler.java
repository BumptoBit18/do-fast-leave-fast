package server.network;

import shared.json.JsonCodec;
import shared.socket.SocketRequest;
import shared.socket.SocketResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
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
        BufferedWriter output = null;
        try (Socket client = socket;
             BufferedReader input = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
             BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))) {
            output = writer;
            String raw = input.readLine();
            if (raw == null || raw.isBlank()) {
                writeResponse(writer, SocketResponse.error("Yeu cau khong hop le."));
                return;
            }

            Object decoded = JsonCodec.fromJson(raw);
            if (!(decoded instanceof Map<?, ?> values)) {
                writeResponse(writer, SocketResponse.error("Yeu cau khong hop le."));
                return;
            }

            SocketRequest request = SocketRequest.fromMap((Map<String, Object>) values);
            if ("SUBSCRIBE_EVENTS".equalsIgnoreCase(request.getAction())) {
                writeResponse(writer, SocketResponse.ok("Subscribed", null));
                ClientSubscriptionRegistry.register(request.getActorUsername(), client, writer);
                while (input.readLine() != null) {
                    // Keep the socket alive until the client disconnects.
                }
                ClientSubscriptionRegistry.unregister(client);
                return;
            }

            SocketResponse response = new MessageRouter().route(request);
            writeResponse(writer, response);
        } catch (Exception ex) {
            System.err.println("ServerHandler failed while processing socket request.");
            ex.printStackTrace(System.err);
            writeBestEffortError(output, ex);
            ClientSubscriptionRegistry.unregister(socket);
        }
    }

    private void writeResponse(BufferedWriter output, SocketResponse response) throws IOException {
        output.write(JsonCodec.toJson(response.toMap()));
        output.newLine();
        output.flush();
    }

    private void writeBestEffortError(BufferedWriter output, Exception ex) {
        if (output == null) {
            return;
        }
        try {
            writeResponse(output, SocketResponse.error(ex.getMessage() == null ? "Loi server." : ex.getMessage()));
        } catch (Exception ignored) {
            // If the socket is already broken, there is nothing left to send.
        }
    }
}
