package server.network;

import org.junit.jupiter.api.Test;
import shared.json.JsonCodec;
import shared.socket.RealtimeEvent;

import java.io.BufferedWriter;
import java.io.StringWriter;
import java.net.Socket;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClientSubscriptionRegistryTest {
    @Test
    void shouldBroadcastOnlyToMatchingSubscriber() throws Exception {
        Socket bidderSocket = new Socket();
        Socket sellerSocket = new Socket();
        StringWriter bidderBuffer = new StringWriter();
        StringWriter sellerBuffer = new StringWriter();
        BufferedWriter bidderWriter = new BufferedWriter(bidderBuffer);
        BufferedWriter sellerWriter = new BufferedWriter(sellerBuffer);

        try {
            ClientSubscriptionRegistry.register("bidder", bidderSocket, bidderWriter);
            ClientSubscriptionRegistry.register("seller", sellerSocket, sellerWriter);

            ClientSubscriptionRegistry.broadcast(new RealtimeEvent("AUCTION_UPDATED", "bidder", "AUC-100"));

            @SuppressWarnings("unchecked")
            Map<String, Object> bidderEvent = (Map<String, Object>) JsonCodec.fromJson(firstLine(bidderBuffer));
            assertEquals("AUCTION_UPDATED", bidderEvent.get("type"));
            assertEquals("bidder", bidderEvent.get("username"));
            assertEquals("AUC-100", bidderEvent.get("auctionId"));
            assertEquals("", sellerBuffer.toString());
        } finally {
            ClientSubscriptionRegistry.unregister(bidderSocket);
            ClientSubscriptionRegistry.unregister(sellerSocket);
            bidderSocket.close();
            sellerSocket.close();
        }
    }

    @Test
    void shouldBroadcastToAllSubscribersForGlobalEvent() throws Exception {
        Socket firstSocket = new Socket();
        Socket secondSocket = new Socket();
        StringWriter firstBuffer = new StringWriter();
        StringWriter secondBuffer = new StringWriter();
        BufferedWriter firstWriter = new BufferedWriter(firstBuffer);
        BufferedWriter secondWriter = new BufferedWriter(secondBuffer);

        try {
            ClientSubscriptionRegistry.register("bidder", firstSocket, firstWriter);
            ClientSubscriptionRegistry.register("seller", secondSocket, secondWriter);

            ClientSubscriptionRegistry.broadcast(new RealtimeEvent("NOTIFICATION_UPDATED", "ALL", null));

            @SuppressWarnings("unchecked")
            Map<String, Object> firstEvent = (Map<String, Object>) JsonCodec.fromJson(firstLine(firstBuffer));
            @SuppressWarnings("unchecked")
            Map<String, Object> secondEvent = (Map<String, Object>) JsonCodec.fromJson(firstLine(secondBuffer));
            assertEquals("NOTIFICATION_UPDATED", firstEvent.get("type"));
            assertEquals("NOTIFICATION_UPDATED", secondEvent.get("type"));
            assertEquals("ALL", firstEvent.get("username"));
            assertNull(firstEvent.get("auctionId"));
        } finally {
            ClientSubscriptionRegistry.unregister(firstSocket);
            ClientSubscriptionRegistry.unregister(secondSocket);
            firstSocket.close();
            secondSocket.close();
        }
    }

    private String firstLine(StringWriter buffer) {
        return buffer.toString().strip();
    }
}
