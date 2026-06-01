package app.service;

import app.model.AppUser;
import app.model.AuctionLot;
import app.model.NotificationItem;
import app.model.PaymentRecord;
import app.model.TopUpRequestRecord;
import app.model.TransactionRecord;
import app.model.UserRole;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import network.ServerConnection;
import shared.socket.RealtimeEvent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AuctionPlatformService {
    private static final long USER_CACHE_MS = Duration.ofSeconds(10).toMillis();
    private static final long AUCTION_CACHE_MS = Duration.ofSeconds(12).toMillis();
    private static final long USER_NOTIFICATION_CACHE_MS = Duration.ofSeconds(10).toMillis();
    private static final long COLLECTION_CACHE_MS = Duration.ofSeconds(12).toMillis();

    private final ServerConnection connection;
    private LocalDateTime lastSeenNotificationTime;
    private AppUser cachedCurrentUser;
    private long currentUserFetchedAt;
    private List<AuctionLot> cachedAuctions = List.of();
    private long auctionsFetchedAt;
    private List<NotificationItem> cachedCurrentUserNotifications = List.of();
    private long currentUserNotificationsFetchedAt;
    private List<AppUser> cachedUsers = List.of();
    private long usersFetchedAt;
    private List<NotificationItem> cachedAllNotifications = List.of();
    private long allNotificationsFetchedAt;
    private List<PaymentRecord> cachedPayments = List.of();
    private long paymentsFetchedAt;
    private List<TransactionRecord> cachedTransactions = List.of();
    private long transactionsFetchedAt;
    private List<TopUpRequestRecord> cachedTopUpRequests = List.of();
    private long topUpRequestsFetchedAt;

    public AuctionPlatformService(ServerConnection connection) {
        this.connection = connection;
    }

    public AppUser login(String username, String password, UserRole role) {
        lastSeenNotificationTime = null;
        AppUser user = connection.login(username, password, role);
        cachedCurrentUser = user;
        currentUserFetchedAt = System.currentTimeMillis();
        invalidateCollections();
        return user;
    }

    public void logout() {
        lastSeenNotificationTime = null;
        cachedCurrentUser = null;
        currentUserFetchedAt = 0;
        invalidateCollections();
        connection.logout();
    }

    public AppUser register(String username, String password, String fullName, UserRole role) {
        AppUser user = connection.register(username, password, fullName, role);
        invalidateCollections();
        return user;
    }

    public AppUser getCurrentUser() {
        long now = System.currentTimeMillis();
        if (cachedCurrentUser != null && now - currentUserFetchedAt < USER_CACHE_MS) {
            return cachedCurrentUser;
        }
        cachedCurrentUser = connection.refreshCurrentUser();
        currentUserFetchedAt = now;
        return cachedCurrentUser;
    }

    public ObservableList<AuctionLot> getAuctions() {
        return FXCollections.observableArrayList(getCachedAuctions());
    }

    public AuctionLot getAuctionById(String auctionId) {
        return connection.getAuctionById(auctionId);
    }

    public List<AuctionLot> searchAuctions(String keyword, String category) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase(Locale.ROOT);
        String normalizedCategory = category == null ? "Tat ca" : category;
        return getCachedAuctions().stream()
                .filter(auction -> normalizedKeyword.isBlank()
                        || auction.getTitle().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || auction.getDescription().toLowerCase(Locale.ROOT).contains(normalizedKeyword)
                        || auction.getSellerUsername().toLowerCase(Locale.ROOT).contains(normalizedKeyword))
                .filter(auction -> "Tat ca".equalsIgnoreCase(normalizedCategory)
                        || auction.getCategory().equalsIgnoreCase(normalizedCategory))
                .toList();
    }

    public ObservableList<AppUser> getUsers() {
        long now = System.currentTimeMillis();
        if (cachedUsers.isEmpty() || now - usersFetchedAt >= COLLECTION_CACHE_MS) {
            cachedUsers = List.copyOf(connection.getUsers());
            usersFetchedAt = now;
        }
        return FXCollections.observableArrayList(cachedUsers);
    }

    public ObservableList<NotificationItem> getNotifications() {
        long now = System.currentTimeMillis();
        if (cachedAllNotifications.isEmpty() || now - allNotificationsFetchedAt >= COLLECTION_CACHE_MS) {
            cachedAllNotifications = List.copyOf(connection.getNotifications());
            allNotificationsFetchedAt = now;
        }
        return FXCollections.observableArrayList(cachedAllNotifications);
    }

    public ObservableList<PaymentRecord> getPayments() {
        long now = System.currentTimeMillis();
        if (cachedPayments.isEmpty() || now - paymentsFetchedAt >= COLLECTION_CACHE_MS) {
            cachedPayments = List.copyOf(connection.getPayments());
            paymentsFetchedAt = now;
        }
        return FXCollections.observableArrayList(cachedPayments);
    }

    public ObservableList<TransactionRecord> getTransactions() {
        long now = System.currentTimeMillis();
        if (cachedTransactions.isEmpty() || now - transactionsFetchedAt >= COLLECTION_CACHE_MS) {
            cachedTransactions = List.copyOf(connection.getTransactions());
            transactionsFetchedAt = now;
        }
        return FXCollections.observableArrayList(cachedTransactions);
    }

    public List<AuctionLot> getAuctionsForSeller(String sellerUsername) {
        return connection.getAuctionsForSeller(sellerUsername);
    }

    public List<AuctionLot> getAuctionsForBidder(String bidderUsername) {
        return connection.getAuctionsForBidder(bidderUsername);
    }

    public List<AuctionLot> getWonAuctionsForBidder(String bidderUsername) {
        return connection.getWonAuctions(bidderUsername);
    }

    public List<NotificationItem> getNotificationsForCurrentUser() {
        AppUser user = getCurrentUser();
        if (user == null) {
            return List.of();
        }

        long now = System.currentTimeMillis();
        if (!cachedCurrentUserNotifications.isEmpty() && now - currentUserNotificationsFetchedAt < USER_NOTIFICATION_CACHE_MS) {
            return cachedCurrentUserNotifications;
        }

        cachedCurrentUserNotifications = List.copyOf(connection.getNotificationsForCurrentUser());
        currentUserNotificationsFetchedAt = now;
        return cachedCurrentUserNotifications;
    }

    public void markCurrentNotificationsSeen() {
        lastSeenNotificationTime = getNotificationsForCurrentUser().stream()
                .map(NotificationItem::getTime)
                .max(LocalDateTime::compareTo)
                .orElse(lastSeenNotificationTime);
    }

    public List<NotificationItem> getNewNotificationsForCurrentUser() {
        List<NotificationItem> notifications = getNotificationsForCurrentUser();
        if (notifications.isEmpty()) {
            return List.of();
        }

        if (lastSeenNotificationTime == null) {
            markCurrentNotificationsSeen();
            return List.of();
        }

        List<NotificationItem> newNotifications = notifications.stream()
                .filter(item -> item.getTime() != null && item.getTime().isAfter(lastSeenNotificationTime))
                .toList();

        LocalDateTime latest = notifications.stream()
                .map(NotificationItem::getTime)
                .max(LocalDateTime::compareTo)
                .orElse(lastSeenNotificationTime);
        lastSeenNotificationTime = latest;
        return newNotifications;
    }

    public AuctionLot createAuction(String sellerUsername, String title, String category, String description, double startPrice, int durationHours, String imageHint) {
        AuctionLot lot = connection.createAuction(sellerUsername, title, category, description, startPrice, durationHours, imageHint);
        invalidateCollections();
        return lot;
    }

    public AuctionLot updateAuction(AuctionLot auction, String title, String category, String description, double startPrice, int durationHours, String imageHint) {
        AuctionLot lot = connection.updateAuction(auction.getId(), title, category, description, startPrice, durationHours, imageHint);
        invalidateCollections();
        return lot;
    }

    public AuctionLot placeBid(AuctionLot auction, double amount) {
        AuctionLot lot = connection.placeBid(auction.getId(), amount);
        invalidateCollections();
        return lot;
    }

    public AuctionLot enableAutoBid(AuctionLot auction, double maxAmount, double incrementStep) {
        AuctionLot lot = connection.enableAutoBid(auction.getId(), maxAmount, incrementStep);
        invalidateCollections();
        return lot;
    }

    public AppUser topUpWallet(double amount) {
        return connection.topUpWallet(amount);
    }

    public TopUpRequestRecord submitTopUpRequest(double amount, String bankName, String accountName, String accountNumber) {
        TopUpRequestRecord record = connection.submitTopUpRequest(amount, bankName, accountName, accountNumber);
        invalidateCollections();
        return record;
    }

    public AppUser approveTopUpRequest(String requestId) {
        AppUser user = connection.approveTopUpRequest(requestId);
        cachedCurrentUser = user;
        currentUserFetchedAt = System.currentTimeMillis();
        invalidateCollections();
        return user;
    }

    public ObservableList<TopUpRequestRecord> getTopUpRequests() {
        long now = System.currentTimeMillis();
        if (cachedTopUpRequests.isEmpty() || now - topUpRequestsFetchedAt >= COLLECTION_CACHE_MS) {
            cachedTopUpRequests = List.copyOf(connection.getTopUpRequests());
            topUpRequestsFetchedAt = now;
        }
        return FXCollections.observableArrayList(cachedTopUpRequests);
    }

    public AuctionLot payForAuction(AuctionLot auction) {
        AuctionLot lot = connection.payForAuction(auction.getId());
        invalidateCollections();
        return lot;
    }

    public AuctionLot cancelAuction(AuctionLot auction) {
        AuctionLot lot = connection.cancelAuction(auction.getId());
        invalidateCollections();
        return lot;
    }

    public void deleteAuction(AuctionLot auction) {
        connection.deleteAuction(auction.getId());
        invalidateCollections();
    }

    public DashboardStats getStats() {
        List<AuctionLot> auctions = getCachedAuctions();
        List<AppUser> users = connection.getUsers();
        long openCount = auctions.stream().filter(auction -> !auction.isClosed()).count();
        long finishedCount = auctions.stream().filter(AuctionLot::isClosed).count();
        long sellers = users.stream().filter(user -> user.getRole() == UserRole.SELLER).count();
        long bidders = users.stream().filter(user -> user.getRole() == UserRole.BIDDER).count();
        double totalVolume = auctions.stream()
                .map(AuctionLot::getHighestBid)
                .filter(Objects::nonNull)
                .mapToDouble(bid -> bid.getAmount())
                .sum();
        long paidCount = auctions.stream().filter(AuctionLot::isPaid).count();
        long autoBidCount = auctions.stream().mapToLong(auction -> auction.getAutoBidRules().size()).sum();
        return new DashboardStats(openCount, finishedCount, sellers, bidders, totalVolume, paidCount, autoBidCount, connection.getNotifications().size());
    }

    public List<String> getCategories() {
        return List.of("Tat ca", "Electronics", "Vehicle", "Art", "Collectible", "Luxury");
    }

    public String formatCurrency(double amount) {
        return String.format(Locale.US, "%,.0f VND", amount);
    }

    public void handleServerEvent(RealtimeEvent event) {
        if (event == null || event.type() == null) {
            return;
        }

        switch (event.type()) {
            case "AUCTION_UPDATED" -> {
                cachedAuctions = List.of();
                auctionsFetchedAt = 0;
            }
            case "USER_UPDATED" -> {
                currentUserFetchedAt = 0;
                cachedUsers = List.of();
                usersFetchedAt = 0;
            }
            case "NOTIFICATION_UPDATED" -> {
                cachedCurrentUserNotifications = List.of();
                currentUserNotificationsFetchedAt = 0;
                cachedAllNotifications = List.of();
                allNotificationsFetchedAt = 0;
            }
            case "TOP_UP_UPDATED" -> {
                cachedTopUpRequests = List.of();
                topUpRequestsFetchedAt = 0;
                currentUserFetchedAt = 0;
            }
            case "PAYMENT_UPDATED" -> {
                cachedPayments = List.of();
                paymentsFetchedAt = 0;
                currentUserFetchedAt = 0;
                cachedAuctions = List.of();
                auctionsFetchedAt = 0;
            }
            case "TRANSACTION_UPDATED" -> {
                cachedTransactions = List.of();
                transactionsFetchedAt = 0;
            }
            default -> {
            }
        }
    }

    private List<AuctionLot> getCachedAuctions() {
        long now = System.currentTimeMillis();
        if (!cachedAuctions.isEmpty() && now - auctionsFetchedAt < AUCTION_CACHE_MS) {
            return cachedAuctions;
        }
        cachedAuctions = List.copyOf(connection.getAuctions());
        auctionsFetchedAt = now;
        return cachedAuctions;
    }

    private void invalidateCollections() {
        cachedAuctions = List.of();
        auctionsFetchedAt = 0;
        cachedUsers = List.of();
        usersFetchedAt = 0;
        cachedAllNotifications = List.of();
        allNotificationsFetchedAt = 0;
        cachedPayments = List.of();
        paymentsFetchedAt = 0;
        cachedTransactions = List.of();
        transactionsFetchedAt = 0;
        cachedTopUpRequests = List.of();
        topUpRequestsFetchedAt = 0;
        cachedCurrentUserNotifications = List.of();
        currentUserNotificationsFetchedAt = 0;
    }
}
