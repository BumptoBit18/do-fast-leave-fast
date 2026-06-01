package server.controller;

import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.BidTransaction;
import server.model.item.Item;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionControllerTest {
    private final ItemController itemController = new ItemController();

    @Test
    void shouldTrackBidProgressAndWinner() {
        Item item = itemController.createItem(
                "Electronics",
                "IT-1",
                "Laptop",
                "Gaming laptop",
                15_000_000,
                LocalDateTime.now().plusHours(2),
                ""
        );
        Auction auction = new Auction("AUC-1", "seller", item);

        auction.addBid(new BidTransaction("BID", "bidderA", auction.getId(), "Manual bid", 15_500_000, LocalDateTime.now()));
        auction.addBid(new BidTransaction("BID", "bidderB", auction.getId(), "Manual bid", 16_000_000, LocalDateTime.now().plusMinutes(1)));

        assertEquals(16_000_000, auction.getCurrentPrice());
        assertEquals(16_100_000, auction.getMinimumBid());
        assertEquals("bidderB", auction.getHighestBidder());
        assertEquals("Live", auction.getStatusLabel());
        assertFalse(auction.isClosed());
    }

    @Test
    void shouldMarkAuctionClosedPaidAndCancelledStates() {
        Item item = itemController.createItem(
                "Art",
                "IT-2",
                "Painting",
                "Original art",
                8_000_000,
                LocalDateTime.now().plusMinutes(10),
                ""
        );
        Auction auction = new Auction("AUC-2", "seller", item);

        auction.extendAuctionSeconds(180);
        auction.markPaid();

        assertTrue(auction.isAntiSnipeTriggered());
        assertTrue(auction.isPaid());

        auction.cancel();
        assertTrue(auction.isCancelled());
        assertTrue(auction.isClosed());
    }
}
