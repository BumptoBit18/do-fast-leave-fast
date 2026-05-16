package network;

import shared.socket.RealtimeEvent;

public interface MessageListener {
    void onMessage(RealtimeEvent event);
}
