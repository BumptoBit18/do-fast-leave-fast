package shared.socket;

public record RealtimeEvent(
        String type,
        String username,
        String auctionId
) {
}
