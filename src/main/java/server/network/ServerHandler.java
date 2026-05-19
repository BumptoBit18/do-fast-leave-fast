package server.network;

import shared.json.JsonCodec;
import shared.socket.SocketRequest;
import shared.socket.SocketResponse;

import java.io.*;
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
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            handleRequest(input, output, client);
        } catch (Throwable ex) {
            System.err.println("[ServerHandler] Loi xu ly socket: " + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            ClientSubscriptionRegistry.unregister(socket);
        }
    }

    private void handleRequest(BufferedReader input, BufferedWriter output, Socket client) throws IOException {
        String raw;
        try {
            raw = input.readLine();
        } catch (IOException ex) {
            writeError(output, "Loi doc yeu cau.");
            return;
        }

        if (raw == null || raw.isBlank()) {
            writeError(output, "Yeu cau khong hop le.");
            return;
        }

        Object decoded;
        try {
            decoded = JsonCodec.fromJson(raw);
        } catch (Exception ex) {
            writeError(output, "JSON khong hop le: " + ex.getMessage());
            return;
        }

        if (!(decoded instanceof Map<?, ?> values)) {
            writeError(output, "Yeu cau khong hop le.");
            return;
        }

        SocketRequest request;
        try {
            request = SocketRequest.fromMap((Map<String, Object>) values);
        } catch (Exception ex) {
            writeError(output, "Yeu cau khong the xu ly: " + ex.getMessage());
            return;
        }

        if ("SUBSCRIBE_EVENTS".equalsIgnoreCase(request.getAction())) {
            writeResponse(output, SocketResponse.ok("Subscribed", null));
            ClientSubscriptionRegistry.register(request.getActorUsername(), client, output);
            try {
                while (input.readLine() != null) {
                    // Giu ket noi cho den khi client ngat.
                }
            } finally {
                ClientSubscriptionRegistry.unregister(client);
            }
            return;
        }

        SocketResponse response;
        try {
            response = new MessageRouter().route(request);
        } catch (Throwable ex) {
            // Bat ca Error (VD: ExceptionInInitializerError khi DB fail) lan Exception
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            System.err.println("[ServerHandler] Loi xu ly request '" + request.getAction() + "': " + ex.getClass().getSimpleName() + " - " + msg);
            writeError(output, "Loi server: " + msg);
            return;
        }

        writeResponse(output, response);
    }

    private void writeError(BufferedWriter output, String message) throws IOException {
        writeResponse(output, SocketResponse.error(message));
    }

    private void writeResponse(BufferedWriter output, SocketResponse response) throws IOException {
        output.write(JsonCodec.toJson(response.toMap()));
        output.newLine();
        output.flush();
    }
}
