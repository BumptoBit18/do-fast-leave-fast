package server.controller;

import org.junit.jupiter.api.Test;
import server.ServerMain;
import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.Auction;
import server.model.AutoBid;
import server.model.BidTransaction;
import server.model.NotificationRecord;
import server.model.PaymentRecord;
import server.model.entity.Bidder;
import server.model.entity.Seller;
import server.model.entity.User;
import server.model.item.Item;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

    @Test
    void shouldCreateSearchAndDeleteAuctionThroughController() {
        ArrayList<Auction> auctions = new ArrayList<>();
        ArrayList<BidTransaction> transactions = new ArrayList<>();
        ArrayList<NotificationRecord> notifications = new ArrayList<>();
        ServerMain server = ControllerTestSupport.newServer(
                new ArrayList<>(),
                auctions,
                transactions,
                new ArrayList<>(),
                notifications,
                new ArrayList<>()
        );
        AuctionController controller = new AuctionController(server);

        Auction created = controller.createAuction("sellerA", "Laptop Pro", "Electronics", "Thin laptop", 12_000_000, 24, "");
        assertEquals("sellerA", created.getSellerUsername());
        assertEquals(1, auctions.size());
        assertEquals(1, controller.listAuctions().size());
        assertEquals(1, controller.searchAuctions("laptop", "Tat ca").size());
        assertEquals(1, controller.getAuctionsForSeller("sellerA").size());
        assertEquals("CREATE_AUCTION", transactions.getFirst().getType());

        controller.deleteAuction(created.getId(), "sellerA");
        assertTrue(auctions.isEmpty());
        assertEquals("DELETE_AUCTION", transactions.getFirst().getType());
        assertTrue(notifications.stream().anyMatch(item -> item.getTitle().contains("xoa")));
    }

    @Test
    void shouldPlaceBidTriggerAutoBidAndExtendNearDeadline() {
        ArrayList<Auction> auctions = new ArrayList<>();
        ArrayList<BidTransaction> transactions = new ArrayList<>();
        ArrayList<NotificationRecord> notifications = new ArrayList<>();
        Item item = itemController.createItem(
                "Electronics",
                "IT-9",
                "Console",
                "Near deadline",
                9_000_000,
                LocalDateTime.now().plusSeconds(120),
                ""
        );
        Auction auction = new Auction("AUC-9", "seller", item);
        auction.addOrReplaceAutoBid(new AutoBid("autoBidder", 10_500_000, 200_000));
        auctions.add(auction);

        AuctionController controller = new AuctionController(ControllerTestSupport.newServer(
                new ArrayList<>(),
                auctions,
                transactions,
                new ArrayList<>(),
                notifications,
                new ArrayList<>()
        ));

        Auction updated = controller.placeBid("AUC-9", "manualBidder", 9_500_000);

        assertEquals("autoBidder", updated.getHighestBidder());
        assertEquals(9_700_000, updated.getCurrentPrice());
        assertTrue(updated.isAntiSnipeTriggered());
        assertTrue(updated.getBidHistory().size() >= 2);
        assertTrue(transactions.stream().anyMatch(itemTx -> itemTx.getType().equals("ANTI_SNIPE")));
        assertTrue(notifications.stream().anyMatch(itemNt -> itemNt.getTitle().contains("tu dong")));
    }

    @Test
    void shouldEnableAutoBidCancelAuctionAndRejectInvalidChanges() {
        ArrayList<Auction> auctions = new ArrayList<>();
        ArrayList<BidTransaction> transactions = new ArrayList<>();
        Item item = itemController.createItem(
                "Luxury",
                "IT-3",
                "Watch",
                "Premium watch",
                20_000_000,
                LocalDateTime.now().plusHours(1),
                ""
        );
        Auction auction = new Auction("AUC-3", "seller", item);
        auctions.add(auction);

        AuctionController controller = new AuctionController(ControllerTestSupport.newServer(
                new ArrayList<>(),
                auctions,
                transactions,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        ));

        Auction autoBidAuction = controller.enableAutoBid("AUC-3", "bidder", 21_000_000, 100_000);
        assertEquals(1, autoBidAuction.getAutoBids().size());

        controller.cancelAuction("AUC-3", "seller");
        assertTrue(auction.isCancelled());
        assertThrows(AuctionClosedException.class, () ->
                controller.enableAutoBid("AUC-3", "bidder", 22_000_000, 100_000));

        Auction busyAuction = new Auction("AUC-4", "seller", itemController.createItem(
                "Luxury",
                "IT-4",
                "Bag",
                "Used once",
                5_000_000,
                LocalDateTime.now().plusHours(2),
                ""
        ));
        busyAuction.addBid(new BidTransaction("BID", "bidder", "AUC-4", "Manual bid", 5_100_000, LocalDateTime.now()));
        auctions.add(busyAuction);

        assertThrows(InvalidBidException.class, () ->
                controller.deleteAuction("AUC-4", "seller"));
    }

    @Test
    void shouldPayClosedAuctionAndCloseExpiredAuctions() {
        ArrayList<Auction> auctions = new ArrayList<>();
        ArrayList<BidTransaction> transactions = new ArrayList<>();
        ArrayList<PaymentRecord> payments = new ArrayList<>();
        ArrayList<NotificationRecord> notifications = new ArrayList<>();
        ArrayList<User> users = new ArrayList<>();
        Seller seller = new Seller("U-1", "seller", "seller123", "Seller", 100_000);
        Bidder bidder = new Bidder("U-2", "bidder", "bidder123", "Bidder", 2_000_000);
        users.add(seller);
        users.add(bidder);

        Auction expiredAuction = new Auction("AUC-5", "seller", itemController.createItem(
                "Art",
                "IT-5",
                "Sketch",
                "Finished auction",
                1_000_000,
                LocalDateTime.now().minusMinutes(2),
                ""
        ));
        expiredAuction.addBid(new BidTransaction("BID", "bidder", "AUC-5", "Win bid", 1_300_000, LocalDateTime.now().minusMinutes(3)));
        auctions.add(expiredAuction);

        AuctionController controller = new AuctionController(ControllerTestSupport.newServer(
                users,
                auctions,
                transactions,
                payments,
                notifications,
                new ArrayList<>()
        ));

        controller.closeExpiredAuctions();
        assertTrue(expiredAuction.isCloseNotified());

        Auction paidAuction = controller.payAuction("AUC-5", "bidder");
        assertTrue(paidAuction.isPaid());
        assertEquals(700_000, bidder.getWalletBalance());
        assertEquals(1_400_000, seller.getWalletBalance());
        assertEquals(1, payments.size());
        assertEquals("PAYMENT", transactions.getFirst().getType());
    }

    @Test
    void shouldRejectInvalidAuctionInputs() {
        AuctionController controller = new AuctionController(ControllerTestSupport.newServer(
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        ));

        assertThrows(IllegalArgumentException.class, () ->
                controller.createAuction("seller", "", "Electronics", "Desc", 1_000_000, 4, ""));
        assertThrows(IllegalArgumentException.class, () ->
                controller.createAuction("seller", "Phone", "Electronics", "Desc", 1_000_000, 4, "bad-image"));
    }
}
