package shared.socket;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record AuctionPayload(
        String id,
        String sellerUsername,
        String title,
        String category,
        String description,
        double startPrice,
        double currentPrice,
        LocalDateTime endTime,
        String imageHint,
        boolean cancelled,
        boolean paid,
        boolean antiSnipeTriggered,
        boolean closeNotified,
        List<BidPayload> bidHistory,
        List<AutoBidPayload> autoBidRules
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
