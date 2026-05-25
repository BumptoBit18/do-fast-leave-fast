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
             BufferedReader input  = new BufferedReader(new InputStreamReader(client.getInputStream(),  StandardCharsets.UTF_8));
             BufferedWriter output = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8))
        ) {
            handle(input, output, client);
        } catch (Throwable ex) {
            System.err.println("[ServerHandler] Loi xu ly socket: "
                    + ex.getClass().getSimpleName() + " - " + ex.getMessage());
            ClientSubscriptionRegistry.unregister(socket);
        }
    }

    private void handle(BufferedReader input, BufferedWriter output, Socket client) throws IOException {
        // 1. Doc request
        String raw;
        try {
            raw = input.readLine();
        } catch (IOException ex) {
            writeError(output, "Loi doc du lieu: " + ex.getMessage());
            return;
        }

        if (raw == null || raw.isBlank()) {
            writeError(output, "Yeu cau trong.");
            return;
        }

        // 2. Parse JSON
        Object decoded;
        try {
            decoded = JsonCodec.fromJson(raw);
        } catch (Exception ex) {
            writeError(output, "JSON khong hop le: " + ex.getMessage());
            return;
        }

        if (!(decoded instanceof Map<?, ?> values)) {
            writeError(output, "Yeu cau khong phai JSON object.");
            return;
        }

        // 3. Parse SocketRequest
        SocketRequest request;
        try {
            //noinspection unchecked
            request = SocketRequest.fromMap((Map<String, Object>) values);
        } catch (Exception ex) {
            writeError(output, "Khong the doc yeu cau: " + ex.getMessage());
            return;
        }

        // 4. Xu ly SUBSCRIBE_EVENTS rieng (giu ket noi song)
        if ("SUBSCRIBE_EVENTS".equalsIgnoreCase(request.getAction())) {
            writeResponse(output, SocketResponse.ok("Subscribed", null));
            ClientSubscriptionRegistry.register(request.getActorUsername(), client, output);
            try {
                //noinspection StatementWithEmptyBody
                while (input.readLine() != null) { /* giu socket song */ }
            } finally {
                ClientSubscriptionRegistry.unregister(client);
            }
            return;
        }

        // 5. Route va tra response — bat ca Error lan Exception
        SocketResponse response;
        try {
            response = new MessageRouter().route(request);
        } catch (Throwable ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            System.err.println("[ServerHandler] Loi route '" + request.getAction() + "': " + msg);
            writeError(output, "Loi server: " + msg);
            return;
        }

        writeResponse(output, response);
    }

    // ── helpers ──────────────────────────────────────────────

    private void writeResponse(BufferedWriter output, SocketResponse response) throws IOException {
        output.write(JsonCodec.toJson(response.toMap()));
        output.newLine();
        output.flush();
    }

    private void writeError(BufferedWriter output, String message) throws IOException {
        writeResponse(output, SocketResponse.error(message));
    }
}
