package server.network;

import shared.socket.SocketRequest;
import shared.socket.SocketResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ServerHandler implements Runnable {
    private final Socket socket;

    public ServerHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (Socket client = socket;
             ObjectOutputStream output = new ObjectOutputStream(client.getOutputStream());
             ObjectInputStream input = new ObjectInputStream(client.getInputStream())) {
            Object raw = input.readObject();
            if (!(raw instanceof SocketRequest request)) {
                output.writeObject(SocketResponse.error("Yeu cau khong hop le."));
                output.flush();
                return;
            }
            SocketResponse response = new MessageRouter().route(request);
            output.writeObject(response);
            output.flush();
        } catch (Exception ignored) {
        }
    }
}
