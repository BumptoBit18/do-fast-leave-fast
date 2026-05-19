package app.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionLotTest {
    @Test
    void shouldTrackHighestBidAndMinimumBid() {
        AuctionLot lot = new AuctionLot(
                "LOT-1",
                "seller",
                "Laptop",
                "Electronics",
                "Thin and light",
                1000000,
                LocalDateTime.now().plusHours(2),
                "silver"
        );

        lot.placeBid(new BidRecord("bidder-a", 1200000, LocalDateTime.now()));
        lot.placeBid(new BidRecord("bidder-b", 1500000, LocalDateTime.now().plusMinutes(1)));

        assertEquals("bidder-b", lot.getHighestBidder());
        assertEquals(1500000, lot.getCurrentPrice());
        assertEquals(1600000, lot.getMinimumBid());
    }

    @Test
    void shouldMarkAntiSnipeAndPaymentState() {
        AuctionLot lot = new AuctionLot(
                "LOT-2",
                "seller",
                "Painting",
                "Art",
                "Original",
                2000000,
                LocalDateTime.now().plusMinutes(30),
                "frame"
        );

        assertFalse(lot.isAntiSnipeTriggered());
        assertFalse(lot.isPaid());

        lot.extendBySeconds(120);
        lot.markPaid();

        assertTrue(lot.isAntiSnipeTriggered());
        assertTrue(lot.isPaid());
    }
}
