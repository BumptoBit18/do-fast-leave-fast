package server.controller;

import server.ServerMain;
import server.dao.AuctionDAO;
import server.dao.BidTransactionDAO;
import server.dao.NotificationDAO;
import server.dao.PaymentDAO;
import server.dao.TopUpRequestDAO;
import server.dao.UserDAO;
import server.model.Auction;
import server.model.AutoBid;
import server.model.BidTransaction;
import server.model.NotificationRecord;
import server.model.PaymentRecord;
import server.model.TopUpRequestRecord;
import server.model.entity.User;
import sun.misc.Unsafe;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

final class ControllerTestSupport {
    private static final Unsafe UNSAFE = initUnsafe();

    private ControllerTestSupport() {
    }

    static ServerMain newServer(
            List<User> users,
            List<Auction> auctions,
            List<BidTransaction> transactions,
            List<PaymentRecord> payments,
            List<NotificationRecord> notifications,
            List<TopUpRequestRecord> topUpRequests
    ) {
        try {
            ServerMain server = (ServerMain) UNSAFE.allocateInstance(ServerMain.class);
            setField(server, "userDAO", new InMemoryUserDAO(users));
            setField(server, "auctionDAO", new InMemoryAuctionDAO(auctions));
            setField(server, "bidTransactionDAO", new InMemoryBidTransactionDAO(transactions));
            setField(server, "paymentDAO", new InMemoryPaymentDAO(payments));
            setField(server, "notificationDAO", new InMemoryNotificationDAO(notifications));
            setField(server, "topUpRequestDAO", new InMemoryTopUpRequestDAO(topUpRequests));
            setField(server, "users", users);
            setField(server, "auctions", auctions);
            setField(server, "transactions", transactions);
            setField(server, "payments", payments);
            setField(server, "notifications", notifications);
            setField(server, "topUpRequests", topUpRequests);
            setField(server, "itemController", new ItemController());
            setField(server, "userController", new UserController(server));
            setField(server, "auctionController", new AuctionController(server));
            setField(server, "autoBidController", new AutoBidController(server));
            return server;
        } catch (InstantiationException ex) {
            throw new IllegalStateException("Khong the tao ServerMain test double.", ex);
        }
    }

    private static Unsafe initUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Khong the truy cap Unsafe.", ex);
        }
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = ServerMain.class.getDeclaredField(fieldName);
            long offset = UNSAFE.objectFieldOffset(field);
            UNSAFE.putObject(target, offset, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Khong the set field " + fieldName, ex);
        }
    }

    private static final class InMemoryUserDAO extends UserDAO {
        private final List<User> users;

        private InMemoryUserDAO(List<User> users) {
            this.users = users;
        }

        @Override
        public List<User> loadAll() {
            return new ArrayList<>(users);
        }

        @Override
        public User findByUsername(String username) {
            return users.stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(username))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public User findByCredentials(String username, String password, String role) {
            return users.stream()
                    .filter(user -> user.getUsername().equalsIgnoreCase(username))
                    .filter(user -> user.getPassword().equals(password))
                    .filter(user -> user.getRole().equalsIgnoreCase(role))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public boolean existsByUsername(String username) {
            return users.stream().anyMatch(user -> user.getUsername().equalsIgnoreCase(username));
        }

        @Override
        public void insert(User user) {
        }

        @Override
        public void updateWalletBalance(String username, double walletBalance) {
        }

        @Override
        public void updateProfile(User user) {
        }

        @Override
        public void deleteByUsername(String username) {
        }
    }

    private static final class InMemoryAuctionDAO extends AuctionDAO {
        private final List<Auction> auctions;

        private InMemoryAuctionDAO(List<Auction> auctions) {
            this.auctions = auctions;
        }

        @Override
        public List<Auction> loadAll() {
            return new ArrayList<>(auctions);
        }

        @Override
        public Auction findById(String auctionId) {
            return auctions.stream()
                    .filter(auction -> auction.getId().equalsIgnoreCase(auctionId))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public List<Auction> search(String keyword, String category) {
            String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
            String normalizedCategory = category == null ? "Tat ca" : category;
            return auctions.stream()
                    .filter(auction -> normalizedKeyword.isBlank()
                            || auction.getItem().getName().toLowerCase().contains(normalizedKeyword)
                            || auction.getItem().getDescription().toLowerCase().contains(normalizedKeyword)
                            || auction.getSellerUsername().toLowerCase().contains(normalizedKeyword))
                    .filter(auction -> "Tat ca".equalsIgnoreCase(normalizedCategory)
                            || auction.getItem().getCategory().equalsIgnoreCase(normalizedCategory))
                    .toList();
        }

        @Override
        public List<Auction> loadBySeller(String sellerUsername) {
            return auctions.stream()
                    .filter(auction -> auction.getSellerUsername().equalsIgnoreCase(sellerUsername))
                    .toList();
        }

        @Override
        public List<Auction> loadByBidder(String bidderUsername) {
            return auctions.stream()
                    .filter(auction -> auction.getBidHistory().stream()
                            .anyMatch(bid -> bid.getActorUsername().equalsIgnoreCase(bidderUsername)))
                    .toList();
        }

        @Override
        public List<Auction> loadWonByBidder(String bidderUsername) {
            return auctions.stream()
                    .filter(Auction::isClosed)
                    .filter(auction -> !auction.isCancelled())
                    .filter(auction -> auction.getHighestBidder().equalsIgnoreCase(bidderUsername))
                    .toList();
        }

        @Override
        public void insert(Auction auction) {
        }

        @Override
        public void updateAuction(Auction auction) {
        }

        @Override
        public void deleteAuction(String auctionId) {
        }

        @Override
        public void insertBid(String auctionId, BidTransaction bid) {
        }

        @Override
        public void replaceAutoBids(String auctionId, List<AutoBid> autoBids) {
        }
    }

    private static final class InMemoryBidTransactionDAO extends BidTransactionDAO {
        private final List<BidTransaction> transactions;

        private InMemoryBidTransactionDAO(List<BidTransaction> transactions) {
            this.transactions = transactions;
        }

        @Override
        public List<BidTransaction> loadAll() {
            return new ArrayList<>(transactions);
        }

        @Override
        public void insert(BidTransaction transaction) {
        }
    }

    private static final class InMemoryPaymentDAO extends PaymentDAO {
        private final List<PaymentRecord> payments;

        private InMemoryPaymentDAO(List<PaymentRecord> payments) {
            this.payments = payments;
        }

        @Override
        public List<PaymentRecord> loadAll() {
            return new ArrayList<>(payments);
        }

        @Override
        public void insert(PaymentRecord payment) {
        }
    }

    private static final class InMemoryNotificationDAO extends NotificationDAO {
        private final List<NotificationRecord> notifications;

        private InMemoryNotificationDAO(List<NotificationRecord> notifications) {
            this.notifications = notifications;
        }

        @Override
        public List<NotificationRecord> loadAll() {
            return new ArrayList<>(notifications);
        }

        @Override
        public List<NotificationRecord> loadForUser(String username) {
            return notifications.stream()
                    .filter(notification -> notification.getUsername().equalsIgnoreCase(username)
                            || notification.getUsername().equalsIgnoreCase("ALL"))
                    .toList();
        }

        @Override
        public void insert(NotificationRecord notification) {
        }
    }

    private static final class InMemoryTopUpRequestDAO extends TopUpRequestDAO {
        private final List<TopUpRequestRecord> requests;

        private InMemoryTopUpRequestDAO(List<TopUpRequestRecord> requests) {
            this.requests = requests;
        }

        @Override
        public List<TopUpRequestRecord> loadAll() {
            return new ArrayList<>(requests);
        }

        @Override
        public void insert(TopUpRequestRecord request) {
        }

        @Override
        public void markApproved(String requestId, String approvedBy, LocalDateTime approvedAt) {
        }

        @Override
        public void markCredited(String requestId, LocalDateTime creditedAt) {
        }
    }
}
