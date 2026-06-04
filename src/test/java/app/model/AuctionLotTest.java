package app.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test
    void shouldReplaceAutoBidRuleCaseInsensitivelyAndTrackCloseNotification() {
        AuctionLot lot = new AuctionLot(
                "LOT-3",
                "seller",
                "Watch",
                "Luxury",
                "Mint condition",
                5000000,
                LocalDateTime.now().plusHours(5),
                "gold"
        );

        lot.addAutoBidRule(new AutoBidRule("bidderA", 5500000, 100000));
        lot.addAutoBidRule(new AutoBidRule("BIDDERA", 6000000, 200000));
        lot.markCloseNotified();

        assertEquals(1, lot.getAutoBidRules().size());
        assertEquals("BIDDERA", lot.getAutoBidRules().get(0).getBidderUsername());
        assertEquals(6000000, lot.getAutoBidRules().get(0).getMaxAmount());
        assertEquals(200000, lot.getAutoBidRules().get(0).getIncrementStep());
        assertTrue(lot.isCloseNotified());
    }

    @Test
    void shouldReportOpenFinishedAndCancelledStates() {
        AuctionLot openLot = new AuctionLot(
                "LOT-4",
                "seller",
                "Camera",
                "Electronics",
                "Compact",
                3000000,
                LocalDateTime.now().plusHours(1),
                ""
        );
        AuctionLot finishedLot = new AuctionLot(
                "LOT-5",
                "seller",
                "Chair",
                "Furniture",
                "Vintage",
                1000000,
                LocalDateTime.now().minusMinutes(1),
                ""
        );

        assertEquals("Open", openLot.getStatusLabel());
        assertFalse(openLot.isClosed());
        assertEquals("Finished", finishedLot.getStatusLabel());
        assertTrue(finishedLot.isClosed());
        assertEquals("Da ket thuc", finishedLot.getTimeLeftLabel());

        openLot.cancel();
        assertTrue(openLot.isCancelled());
        assertEquals("Cancelled", openLot.getStatusLabel());
        assertEquals("Da huy", openLot.getTimeLeftLabel());
        assertTrue(openLot.isClosed());
    }

    @Test
    void shouldExposeHighestBidAndMinimumBidForEmptyLot() {
        AuctionLot lot = new AuctionLot(
                "LOT-6",
                "seller",
                "Book",
                "Collectible",
                "Signed copy",
                900000,
                LocalDateTime.now().plusHours(3),
                ""
        );

        assertNull(lot.getHighestBid());
        assertEquals("Chua co", lot.getHighestBidder());
        assertEquals(1000000, lot.getMinimumBid());
    }
}
