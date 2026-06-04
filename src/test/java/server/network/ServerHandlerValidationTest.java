package server.network;

import org.junit.jupiter.api.Test;
import shared.json.JsonCodec;
import shared.socket.SocketResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ServerHandlerValidationTest {
    @Test
    void shouldRejectBlankRequestLine() throws Exception {
        try (HandlerFixture fixture = new HandlerFixture()) {
            fixture.writer.write("   ");
            fixture.writer.newLine();
            fixture.writer.flush();

            SocketResponse response = SocketResponse.fromMap(readMap(fixture.reader));

            assertFalse(response.isSuccess());
            assertEquals("Yeu cau khong hop le.", response.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> readMap(BufferedReader reader) throws Exception {
        return (Map<String, Object>) JsonCodec.fromJson(reader.readLine());
    }

    private static final class HandlerFixture implements AutoCloseable {
        private final ServerSocket serverSocket = new ServerSocket(0);
        private final ExecutorService executor = Executors.newSingleThreadExecutor();
        private final Future<?> handlerFuture;
        private final Socket client;
        private final BufferedReader reader;
        private final BufferedWriter writer;

        private HandlerFixture() throws Exception {
            handlerFuture = executor.submit(() -> {
                try {
                    new ServerHandler(serverSocket.accept()).run();
                } catch (Exception ex) {
                    throw new IllegalStateException(ex);
                }
            });
            client = new Socket("localhost", serverSocket.getLocalPort());
            client.setSoTimeout(5_000);
            reader = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
            writer = new BufferedWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8));
        }

        @Override
        public void close() throws Exception {
            client.close();
            serverSocket.close();
            handlerFuture.get(5, TimeUnit.SECONDS);
            executor.shutdownNow();
        }
    }
}
