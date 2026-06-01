package server.model;

import org.junit.jupiter.api.Test;
import server.controller.ItemController;
import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.item.Item;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelTest {
    private final ItemController itemController = new ItemController();

    @Test
    void shouldExposeExplicitAuctionStateTransitions() {
        Auction auction = createAuction(LocalDateTime.now().plusHours(2));

        assertEquals(AuctionStatus.OPEN, auction.getStatus());
        assertEquals("Open", auction.getStatusLabel());

        auction.addBid(new BidTransaction("BID", "bidder", auction.getId(), "Manual bid", 1_100_000, LocalDateTime.now()));
        assertEquals(AuctionStatus.RUNNING, auction.getStatus());
        assertEquals("Live", auction.getStatusLabel());

        auction.markPaid();
        assertEquals(AuctionStatus.PAID, auction.getStatus());
        assertEquals("Paid", auction.getStatusLabel());

        auction.cancel();
        assertEquals(AuctionStatus.CANCELLED, auction.getStatus());
        assertEquals("Cancelled", auction.getStatusLabel());
    }

    @Test
    void shouldReportFinishedAuctionAndCloseNotification() {
        Auction auction = createAuction(LocalDateTime.now().minusMinutes(1));

        assertTrue(auction.isClosed());
        assertEquals(AuctionStatus.FINISHED, auction.getStatus());
        assertEquals("Finished", auction.getStatusLabel());
        assertEquals("Da ket thuc", auction.getTimeLeftLabel());
        assertFalse(auction.isCloseNotified());

        auction.markCloseNotified();
        assertTrue(auction.isCloseNotified());
    }

    @Test
    void shouldExtendAuctionAndReplaceAutoBidRule() {
        Auction auction = createAuction(LocalDateTime.now().plusMinutes(3));
        LocalDateTime originalEndTime = auction.getItem().getEndTime();

        auction.extendAuctionSeconds(180);
        auction.addOrReplaceAutoBid(new AutoBid("bidder", 1_500_000, 100_000));
        auction.addOrReplaceAutoBid(new AutoBid("BIDDER", 1_800_000, 200_000));

        assertTrue(auction.isAntiSnipeTriggered());
        assertEquals(originalEndTime.plusSeconds(180), auction.getItem().getEndTime());
        assertEquals(1, auction.getAutoBids().size());
        assertEquals(1_800_000, auction.getAutoBids().get(0).getMaxAmount());
        assertThrows(UnsupportedOperationException.class, () -> auction.getAutoBids().clear());
        assertThrows(UnsupportedOperationException.class, () -> auction.getBidHistory().clear());
    }

    @Test
    void shouldRejectLowBidAndBidOnClosedAuction() {
        Auction openAuction = createAuction(LocalDateTime.now().plusHours(2));
        Auction closedAuction = createAuction(LocalDateTime.now().minusMinutes(1));

        assertThrows(InvalidBidException.class, () -> openAuction.validateBid(1_000_000));
        assertThrows(AuctionClosedException.class, () -> closedAuction.validateBid(1_100_000));
        openAuction.validateBid(1_100_000);
    }

    private Auction createAuction(LocalDateTime endTime) {
        Item item = itemController.createItem("electronics", "IT-STATE", "Laptop", "Gaming", 1_000_000, endTime, "");
        return new Auction("AUC-STATE", "seller", item);
    }
}
