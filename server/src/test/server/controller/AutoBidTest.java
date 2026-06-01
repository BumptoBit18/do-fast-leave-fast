package server.controller;

import org.junit.jupiter.api.Test;
import server.model.Auction;
import server.model.AutoBid;
import server.model.BidTransaction;
import server.model.item.Item;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AutoBidTest {
    private final ItemController itemController = new ItemController();

    @Test
    void shouldReplaceAutoBidRuleForSameBidder() {
        Item item = itemController.createItem(
                "Electronics",
                "IT-3",
                "Phone",
                "Flagship phone",
                10_000_000,
                LocalDateTime.now().plusHours(4),
                ""
        );
        Auction auction = new Auction("AUC-3", "seller", item);

        auction.addOrReplaceAutoBid(new AutoBid("bidderA", 12_000_000, 200_000));
        auction.addOrReplaceAutoBid(new AutoBid("bidderA", 13_000_000, 300_000));

        assertEquals(1, auction.getAutoBids().size());
        assertEquals(13_000_000, auction.getAutoBids().get(0).getMaxAmount());
        assertEquals(300_000, auction.getAutoBids().get(0).getIncrementStep());
    }

    @Test
    void shouldKeepHighestBidSeparateFromAutoBidRules() {
        Item item = itemController.createItem(
                "Vehicle",
                "IT-4",
                "Scooter",
                "Nice scooter",
                20_000_000,
                LocalDateTime.now().plusHours(8),
                ""
        );
        Auction auction = new Auction("AUC-4", "seller", item);

        auction.addOrReplaceAutoBid(new AutoBid("bidderA", 24_000_000, 500_000));
        auction.addBid(new BidTransaction("BID", "bidderB", auction.getId(), "Manual bid", 21_000_000, LocalDateTime.now()));

        assertEquals(21_000_000, auction.getCurrentPrice());
        assertEquals("bidderB", auction.getHighestBidder());
        assertEquals(1, auction.getAutoBids().size());
    }
}
