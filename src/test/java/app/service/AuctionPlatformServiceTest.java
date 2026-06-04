package app.service;

import app.model.AppUser;
import app.model.AuctionLot;
import app.model.BidRecord;
import app.model.NotificationItem;
import app.model.PaymentRecord;
import app.model.TopUpRequestRecord;
import app.model.TransactionRecord;
import app.model.UserRole;
import network.ServerConnection;
import org.junit.jupiter.api.Test;
import shared.socket.RealtimeEvent;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuctionPlatformServiceTest {
    @Test
    void shouldCacheAuctionsSearchThemAndComputeDashboardStats() {
        FakeServerConnection connection = new FakeServerConnection();
        LocalDateTime now = LocalDateTime.now();

        AuctionLot laptop = new AuctionLot("AUC-1", "sellerA", "Laptop Pro", "Electronics", "Thin and light", 10_000_000, now.plusHours(2), "");
        laptop.placeBid(new BidRecord("bidderA", 11_000_000, now.minusMinutes(10)));
        laptop.addAutoBidRule(new app.model.AutoBidRule("bidderA", 12_000_000, 200_000));

        AuctionLot scooter = new AuctionLot("AUC-2", "sellerB", "City Scooter", "Vehicle", "Red scooter", 20_000_000, now.minusMinutes(5), "");
        scooter.markPaid();

        connection.auctions = List.of(laptop, scooter);
        connection.users = List.of(
                new AppUser("U-1", "sellerA", "pw", UserRole.SELLER, "Seller A", 0),
                new AppUser("U-2", "sellerB", "pw", UserRole.SELLER, "Seller B", 0),
                new AppUser("U-3", "bidderA", "pw", UserRole.BIDDER, "Bidder A", 0)
        );
        connection.notifications = List.of(
                new NotificationItem("sellerA", "Auction update", "Laptop Pro has a new bid", now)
        );

        AuctionPlatformService service = new AuctionPlatformService(connection);

        assertEquals(2, service.getAuctions().size());
        assertEquals(1, connection.getAuctionsCalls);
        assertEquals(2, service.getAuctions().size());
        assertEquals(1, connection.getAuctionsCalls);

        assertEquals(1, service.searchAuctions("laptop", "Tat ca").size());
        assertEquals("AUC-1", service.searchAuctions("laptop", "Tat ca").getFirst().getId());
        assertEquals(1, service.searchAuctions("", "Vehicle").size());
        assertEquals("AUC-2", service.searchAuctions("", "Vehicle").getFirst().getId());

        DashboardStats stats = service.getStats();
        assertEquals(1, stats.openAuctions());
        assertEquals(1, stats.finishedAuctions());
        assertEquals(2, stats.sellers());
        assertEquals(1, stats.bidders());
        assertEquals(11_000_000, stats.totalVolume());
        assertEquals(1, stats.paidAuctions());
        assertEquals(1, stats.autoBidRules());
        assertEquals(1, stats.notificationCount());

        service.handleServerEvent(new RealtimeEvent("AUCTION_UPDATED", "ALL", "AUC-1"));
        assertEquals(2, service.getAuctions().size());
        assertEquals(2, connection.getAuctionsCalls);
        assertEquals("10,000,000 VND", service.formatCurrency(10_000_000));
        assertTrue(service.getCategories().contains("Electronics"));
    }

    @Test
    void shouldTrackOnlyNewNotificationsAfterSeenMarker() {
        FakeServerConnection connection = new FakeServerConnection();
        LocalDateTime now = LocalDateTime.now();
        connection.currentUser = new AppUser("U-9", "bidder", "pw", UserRole.BIDDER, "Bidder", 0);
        connection.currentUserNotifications = new ArrayList<>(List.of(
                new NotificationItem("bidder", "Welcome", "Oldest", now.minusMinutes(3)),
                new NotificationItem("bidder", "Outbid", "Previous", now.minusMinutes(1))
        ));

        AuctionPlatformService service = new AuctionPlatformService(connection);

        assertTrue(service.getNewNotificationsForCurrentUser().isEmpty());
        assertEquals(1, connection.getNotificationsForCurrentUserCalls);

        connection.currentUserNotifications = new ArrayList<>(List.of(
                new NotificationItem("bidder", "Welcome", "Oldest", now.minusMinutes(3)),
                new NotificationItem("bidder", "Outbid", "Previous", now.minusMinutes(1)),
                new NotificationItem("bidder", "Winner", "Newest", now.plusMinutes(1))
        ));
        service.handleServerEvent(new RealtimeEvent("NOTIFICATION_UPDATED", "bidder", null));

        List<NotificationItem> newItems = service.getNewNotificationsForCurrentUser();
        assertEquals(1, newItems.size());
        assertEquals("Winner", newItems.getFirst().getTitle());
        assertEquals(2, connection.getNotificationsForCurrentUserCalls);
    }

    @Test
    void shouldHandleAuthLifecycleAndCurrentUserCaching() {
        FakeServerConnection connection = new FakeServerConnection();
        AppUser bidder = new AppUser("U-1", "bidder", "pw", UserRole.BIDDER, "Bidder", 1_000_000);
        AppUser refreshed = new AppUser("U-1", "bidder", "pw", UserRole.BIDDER, "Bidder", 2_000_000);
        connection.loginResult = bidder;
        connection.registerResult = new AppUser("U-2", "seller", "pw", UserRole.SELLER, "Seller", 0);

        AuctionPlatformService service = new AuctionPlatformService(connection);

        assertEquals("bidder", service.login("bidder", "pw", UserRole.BIDDER).getUsername());
        assertEquals(1, connection.loginCalls);

        assertEquals("Seller", service.register("seller", "pw", "Seller", UserRole.SELLER).getFullName());
        assertEquals(1, connection.registerCalls);

        assertEquals(1_000_000, service.getCurrentUser().getWalletBalance());
        assertEquals(0, connection.refreshCurrentUserCalls);
        assertEquals(1_000_000, service.getCurrentUser().getWalletBalance());
        assertEquals(0, connection.refreshCurrentUserCalls);

        connection.currentUser = refreshed;
        service.handleServerEvent(new RealtimeEvent("USER_UPDATED", "ALL", null));
        assertEquals(2_000_000, service.getCurrentUser().getWalletBalance());
        assertEquals(1, connection.refreshCurrentUserCalls);

        service.logout();
        assertEquals(1, connection.logoutCalls);
        assertNull(service.getCurrentUser());
        assertEquals(2, connection.refreshCurrentUserCalls);
    }

    @Test
    void shouldInvalidateAuctionCacheAfterAuctionMutations() {
        FakeServerConnection connection = new FakeServerConnection();
        LocalDateTime now = LocalDateTime.now();
        AuctionLot original = new AuctionLot("AUC-1", "seller", "Phone", "Electronics", "Original", 5_000_000, now.plusHours(1), "");
        AuctionLot created = new AuctionLot("AUC-2", "seller", "Tablet", "Electronics", "Created", 6_000_000, now.plusHours(2), "");
        AuctionLot updated = new AuctionLot("AUC-1", "seller", "Phone X", "Electronics", "Updated", 5_500_000, now.plusHours(3), "");
        AuctionLot bidPlaced = new AuctionLot("AUC-1", "seller", "Phone X", "Electronics", "Bid", 5_500_000, now.plusHours(3), "");
        bidPlaced.placeBid(new BidRecord("bidder", 6_000_000, now));
        AuctionLot autoBidLot = new AuctionLot("AUC-1", "seller", "Phone X", "Electronics", "Auto", 5_500_000, now.plusHours(3), "");
        autoBidLot.addAutoBidRule(new app.model.AutoBidRule("bidder", 7_000_000, 200_000));
        AuctionLot paidLot = new AuctionLot("AUC-1", "seller", "Phone X", "Electronics", "Paid", 5_500_000, now.minusMinutes(1), "");
        paidLot.markPaid();
        AuctionLot cancelledLot = new AuctionLot("AUC-1", "seller", "Phone X", "Electronics", "Cancelled", 5_500_000, now.plusHours(1), "");
        cancelledLot.cancel();

        connection.auctions = List.of(original);
        connection.createAuctionResult = created;
        connection.updateAuctionResult = updated;
        connection.placeBidResult = bidPlaced;
        connection.enableAutoBidResult = autoBidLot;
        connection.payForAuctionResult = paidLot;
        connection.cancelAuctionResult = cancelledLot;

        AuctionPlatformService service = new AuctionPlatformService(connection);

        assertEquals(1, service.getAuctions().size());
        assertEquals(1, connection.getAuctionsCalls);

        connection.auctions = List.of(original, created);
        service.createAuction("seller", "Tablet", "Electronics", "Created", 6_000_000, 24, "");
        assertEquals(1, connection.createAuctionCalls);
        service.getAuctions();
        assertEquals(2, connection.getAuctionsCalls);

        connection.auctions = List.of(updated, created);
        service.updateAuction(original, "Phone X", "Electronics", "Updated", 5_500_000, 48, "");
        assertEquals(1, connection.updateAuctionCalls);
        service.getAuctions();
        assertEquals(3, connection.getAuctionsCalls);

        connection.auctions = List.of(bidPlaced, created);
        service.placeBid(original, 6_000_000);
        assertEquals(1, connection.placeBidCalls);
        service.getAuctions();
        assertEquals(4, connection.getAuctionsCalls);

        connection.auctions = List.of(autoBidLot, created);
        service.enableAutoBid(original, 7_000_000, 200_000);
        assertEquals(1, connection.enableAutoBidCalls);
        service.getAuctions();
        assertEquals(5, connection.getAuctionsCalls);

        connection.auctions = List.of(paidLot, created);
        service.payForAuction(original);
        assertEquals(1, connection.payForAuctionCalls);
        service.getAuctions();
        assertEquals(6, connection.getAuctionsCalls);

        connection.auctions = List.of(cancelledLot, created);
        service.cancelAuction(original);
        assertEquals(1, connection.cancelAuctionCalls);
        service.getAuctions();
        assertEquals(7, connection.getAuctionsCalls);

        connection.auctions = List.of(created);
        service.deleteAuction(original);
        assertEquals(1, connection.deleteAuctionCalls);
        service.getAuctions();
        assertEquals(8, connection.getAuctionsCalls);
    }

    @Test
    void shouldManageUserTopUpPaymentAndTransactionCollections() {
        FakeServerConnection connection = new FakeServerConnection();
        LocalDateTime now = LocalDateTime.now();
        connection.currentUser = new AppUser("U-9", "admin", "pw", UserRole.ADMIN, "Admin", 0);
        connection.users = List.of(new AppUser("U-1", "bidder", "pw", UserRole.BIDDER, "Bidder", 0));
        connection.topUpRequests = List.of(new TopUpRequestRecord("TOP-1", "bidder", 2_000_000, "VCB", "Bidder", "123", now, "PENDING", null, null, null));
        connection.payments = List.of(new PaymentRecord("AUC-1", "bidder", "seller", 5_000_000, now));
        connection.transactions = List.of(new TransactionRecord("BID", "bidder", "AUC-1", "Manual bid", now));
        connection.notifications = List.of(new NotificationItem("admin", "Hello", "System ready", now));
        connection.updateUserResult = new AppUser("U-1", "bidder", "newpw", UserRole.BIDDER, "Bidder Updated", 0);
        connection.approveTopUpRequestResult = connection.currentUser;

        AuctionPlatformService service = new AuctionPlatformService(connection);

        assertEquals(1, service.getUsers().size());
        assertEquals(1, connection.getUsersCalls);
        assertEquals(1, service.getUsers().size());
        assertEquals(1, connection.getUsersCalls);

        assertEquals(1, service.getTopUpRequests().size());
        assertEquals(1, connection.getTopUpRequestsCalls);
        assertEquals(1, service.getPayments().size());
        assertEquals(1, connection.getPaymentsCalls);
        assertEquals(1, service.getTransactions().size());
        assertEquals(1, connection.getTransactionsCalls);
        assertEquals(1, service.getNotifications().size());
        assertEquals(1, connection.getNotificationsCalls);

        service.updateUser("bidder", "Bidder Updated", "newpw");
        assertEquals(1, connection.updateUserCalls);
        service.getUsers();
        assertEquals(2, connection.getUsersCalls);

        service.approveTopUpRequest("TOP-1");
        assertEquals(1, connection.approveTopUpRequestCalls);
        service.getTopUpRequests();
        assertEquals(2, connection.getTopUpRequestsCalls);

        service.handleServerEvent(new RealtimeEvent("PAYMENT_UPDATED", "bidder", "AUC-1"));
        service.getPayments();
        assertEquals(2, connection.getPaymentsCalls);

        service.handleServerEvent(new RealtimeEvent("TRANSACTION_UPDATED", "bidder", "AUC-1"));
        service.getTransactions();
        assertEquals(2, connection.getTransactionsCalls);
    }

    @Test
    void shouldDelegateLookupAndWalletOperations() {
        FakeServerConnection connection = new FakeServerConnection();
        LocalDateTime now = LocalDateTime.now();
        AuctionLot sellerLot = new AuctionLot("AUC-10", "seller", "Camera", "Electronics", "Seller lot", 8_000_000, now.plusHours(5), "");
        AuctionLot bidderLot = new AuctionLot("AUC-11", "seller", "Watch", "Luxury", "Bidder lot", 12_000_000, now.plusHours(6), "");
        AuctionLot wonLot = new AuctionLot("AUC-12", "seller", "Painting", "Art", "Won lot", 15_000_000, now.minusMinutes(10), "");
        TopUpRequestRecord topUpRecord = new TopUpRequestRecord("TOP-2", "bidder", 1_500_000, "ACB", "Bidder", "456", now, "PENDING", null, null, null);

        connection.auctionByIdResult = sellerLot;
        connection.auctionsForSeller = List.of(sellerLot);
        connection.auctionsForBidder = List.of(bidderLot);
        connection.wonAuctions = List.of(wonLot);
        connection.topUpWalletResult = new AppUser("U-3", "bidder", "pw", UserRole.BIDDER, "Bidder", 3_500_000);
        connection.submitTopUpRequestResult = topUpRecord;

        AuctionPlatformService service = new AuctionPlatformService(connection);

        assertEquals("AUC-10", service.getAuctionById("AUC-10").getId());
        assertEquals(1, connection.getAuctionByIdCalls);

        assertEquals(1, service.getAuctionsForSeller("seller").size());
        assertEquals(1, connection.getAuctionsForSellerCalls);
        assertEquals(1, service.getAuctionsForBidder("bidder").size());
        assertEquals(1, connection.getAuctionsForBidderCalls);
        assertEquals(1, service.getWonAuctionsForBidder("bidder").size());
        assertEquals(1, connection.getWonAuctionsCalls);

        assertEquals(3_500_000, service.topUpWallet(1_500_000).getWalletBalance());
        assertEquals(1, connection.topUpWalletCalls);

        assertEquals("TOP-2", service.submitTopUpRequest(1_500_000, "ACB", "Bidder", "456").getId());
        assertEquals(1, connection.submitTopUpRequestCalls);

        service.deleteUser("bidder");
        assertEquals(1, connection.deleteUserCalls);
    }

    private static final class FakeServerConnection extends ServerConnection {
        private List<AuctionLot> auctions = List.of();
        private List<AppUser> users = List.of();
        private List<NotificationItem> notifications = List.of();
        private List<NotificationItem> currentUserNotifications = List.of();
        private List<PaymentRecord> payments = List.of();
        private List<TransactionRecord> transactions = List.of();
        private List<TopUpRequestRecord> topUpRequests = List.of();
        private List<AuctionLot> auctionsForSeller = List.of();
        private List<AuctionLot> auctionsForBidder = List.of();
        private List<AuctionLot> wonAuctions = List.of();
        private AppUser currentUser;
        private AppUser loginResult;
        private AppUser registerResult;
        private AppUser updateUserResult;
        private AppUser approveTopUpRequestResult;
        private AppUser topUpWalletResult;
        private AuctionLot createAuctionResult;
        private AuctionLot updateAuctionResult;
        private AuctionLot placeBidResult;
        private AuctionLot enableAutoBidResult;
        private AuctionLot payForAuctionResult;
        private AuctionLot cancelAuctionResult;
        private AuctionLot auctionByIdResult;
        private TopUpRequestRecord submitTopUpRequestResult;
        private int getAuctionsCalls;
        private int getAuctionByIdCalls;
        private int getUsersCalls;
        private int getNotificationsCalls;
        private int getPaymentsCalls;
        private int getTransactionsCalls;
        private int getTopUpRequestsCalls;
        private int getAuctionsForSellerCalls;
        private int getAuctionsForBidderCalls;
        private int getWonAuctionsCalls;
        private int getNotificationsForCurrentUserCalls;
        private int refreshCurrentUserCalls;
        private int loginCalls;
        private int logoutCalls;
        private int registerCalls;
        private int createAuctionCalls;
        private int updateAuctionCalls;
        private int placeBidCalls;
        private int enableAutoBidCalls;
        private int payForAuctionCalls;
        private int cancelAuctionCalls;
        private int deleteAuctionCalls;
        private int topUpWalletCalls;
        private int submitTopUpRequestCalls;
        private int updateUserCalls;
        private int deleteUserCalls;
        private int approveTopUpRequestCalls;

        @Override
        public List<AuctionLot> getAuctions() {
            getAuctionsCalls++;
            return auctions;
        }

        @Override
        public AuctionLot getAuctionById(String auctionId) {
            getAuctionByIdCalls++;
            return auctionByIdResult;
        }

        @Override
        public List<AppUser> getUsers() {
            getUsersCalls++;
            return users;
        }

        @Override
        public List<NotificationItem> getNotifications() {
            getNotificationsCalls++;
            return notifications;
        }

        @Override
        public AppUser refreshCurrentUser() {
            refreshCurrentUserCalls++;
            return currentUser;
        }

        @Override
        public List<NotificationItem> getNotificationsForCurrentUser() {
            getNotificationsForCurrentUserCalls++;
            return currentUserNotifications;
        }

        @Override
        public AppUser login(String username, String password, UserRole role) {
            loginCalls++;
            currentUser = loginResult;
            return loginResult;
        }

        @Override
        public void logout() {
            logoutCalls++;
            currentUser = null;
        }

        @Override
        public AppUser register(String username, String password, String fullName, UserRole role) {
            registerCalls++;
            return registerResult;
        }

        @Override
        public List<AuctionLot> getAuctionsForSeller(String sellerUsername) {
            getAuctionsForSellerCalls++;
            return auctionsForSeller;
        }

        @Override
        public List<AuctionLot> getAuctionsForBidder(String bidderUsername) {
            getAuctionsForBidderCalls++;
            return auctionsForBidder;
        }

        @Override
        public List<AuctionLot> getWonAuctions(String bidderUsername) {
            getWonAuctionsCalls++;
            return wonAuctions;
        }

        @Override
        public AuctionLot createAuction(String sellerUsername, String title, String category, String description, double startPrice, int durationHours, String imageHint) {
            createAuctionCalls++;
            return createAuctionResult;
        }

        @Override
        public AuctionLot updateAuction(String auctionId, String title, String category, String description, double startPrice, int durationHours, String imageHint) {
            updateAuctionCalls++;
            return updateAuctionResult;
        }

        @Override
        public AuctionLot placeBid(String auctionId, double amount) {
            placeBidCalls++;
            return placeBidResult;
        }

        @Override
        public AuctionLot enableAutoBid(String auctionId, double maxAmount, double incrementStep) {
            enableAutoBidCalls++;
            return enableAutoBidResult;
        }

        @Override
        public AuctionLot payForAuction(String auctionId) {
            payForAuctionCalls++;
            return payForAuctionResult;
        }

        @Override
        public AuctionLot cancelAuction(String auctionId) {
            cancelAuctionCalls++;
            return cancelAuctionResult;
        }

        @Override
        public void deleteAuction(String auctionId) {
            deleteAuctionCalls++;
        }

        @Override
        public AppUser topUpWallet(double amount) {
            topUpWalletCalls++;
            return topUpWalletResult;
        }

        @Override
        public TopUpRequestRecord submitTopUpRequest(double amount, String bankName, String accountName, String accountNumber) {
            submitTopUpRequestCalls++;
            return submitTopUpRequestResult;
        }

        @Override
        public AppUser updateUser(String username, String fullName, String password) {
            updateUserCalls++;
            return updateUserResult;
        }

        @Override
        public void deleteUser(String username) {
            deleteUserCalls++;
        }

        @Override
        public AppUser approveTopUpRequest(String requestId) {
            approveTopUpRequestCalls++;
            return approveTopUpRequestResult;
        }

        @Override
        public List<TopUpRequestRecord> getTopUpRequests() {
            getTopUpRequestsCalls++;
            return topUpRequests;
        }

        @Override
        public List<PaymentRecord> getPayments() {
            getPaymentsCalls++;
            return payments;
        }

        @Override
        public List<TransactionRecord> getTransactions() {
            getTransactionsCalls++;
            return transactions;
        }
    }
}
