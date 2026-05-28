package server;

import server.controller.AuctionController;
import server.controller.AutoBidController;
import server.controller.ItemController;
import server.controller.UserController;
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
import server.model.entity.Admin;
import server.model.entity.Bidder;
import server.model.entity.Seller;
import server.model.entity.User;
import server.model.item.Item;
import server.network.ClientSubscriptionRegistry;
import server.util.DatabaseManager;
import server.util.ObjectFileStore;
import shared.socket.RealtimeEvent;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ServerMain {
    private static final ServerMain INSTANCE = new ServerMain();

    private final UserDAO userDAO = new UserDAO();
    private final AuctionDAO auctionDAO = new AuctionDAO();
    private final BidTransactionDAO bidTransactionDAO = new BidTransactionDAO();
    private final PaymentDAO paymentDAO = new PaymentDAO();
    private final NotificationDAO notificationDAO = new NotificationDAO();
    private final TopUpRequestDAO topUpRequestDAO = new TopUpRequestDAO();

    private final List<User> users;
    private final List<Auction> auctions;
    private final List<BidTransaction> transactions;
    private final List<PaymentRecord> payments;
    private final List<NotificationRecord> notifications;
    private final List<TopUpRequestRecord> topUpRequests;

    private final ItemController itemController;
    private final UserController userController;
    private final AuctionController auctionController;
    private final AutoBidController autoBidController;

    // Doc file tu o cung moi khi du lieu thuc su bi thay doi
    private boolean auctionsDirty = false;
    private boolean usersDirty = false;
    private boolean TopUpRequestsDirty = false;

    private ServerMain() {
        DatabaseManager.initialize();

        this.users = new ArrayList<>(userDAO.loadAll());
        this.auctions = new ArrayList<>(auctionDAO.loadAll());
        this.transactions = new ArrayList<>(bidTransactionDAO.loadAll());
        this.payments = new ArrayList<>(paymentDAO.loadAll());
        this.notifications = new ArrayList<>(notificationDAO.loadAll());
        this.topUpRequests = new ArrayList<>(topUpRequestDAO.loadAll());

        this.itemController = new ItemController();
        this.userController = new UserController(this);
        this.auctionController = new AuctionController(this);
        this.autoBidController = new AutoBidController(this);

        migrateLegacyDataIfDatabaseEmpty();

        boolean adminAdjusted = ensureFixedAdminAccount();
        if (users.isEmpty() || auctions.isEmpty()) {
            seedData();
            persistAll();
        } else if (adminAdjusted) {
            persistAll();
        }
        userController.processApprovedTopUpCredits();
    }

    public static ServerMain getInstance() {
        return INSTANCE;
    }

    public synchronized void reloadAllFromDisk() {
        users.clear();
        users.addAll(userDAO.loadAll());

        auctions.clear();
        auctions.addAll(auctionDAO.loadAll());

        transactions.clear();
        transactions.addAll(bidTransactionDAO.loadAll());

        payments.clear();
        payments.addAll(paymentDAO.loadAll());

        notifications.clear();
        notifications.addAll(notificationDAO.loadAll());

        topUpRequests.clear();
        topUpRequests.addAll(topUpRequestDAO.loadAll());

        auctionsDirty = false;
        usersDirty = false;
        TopUpRequestsDirty = false;

        userController.processApprovedTopUpCredits();
    }

    public synchronized void markAuctionsDirty(){auctionsDirty = true;}
    public synchronized void markUsersDirty(){usersDirty = true;}
    public synchronized void markTopUpsDirty(){TopUpRequestsDirty = true;}

    public synchronized void reloadAuctionIfNeeded(){
        if (auctionsDirty){
            auctions.clear();
            auctions.addAll(auctionDAO.loadAll());

            auctionsDirty = false;
        }
    }

    public synchronized void reLoadUsersIfNeeded(){
        if (usersDirty){
            users.clear();
            users.addAll(userDAO.loadAll());

            usersDirty = false;
        }
    }

    public synchronized void reloadTopUpRequestIfNeeded(){
        if (TopUpRequestsDirty){
            topUpRequests.clear();
            topUpRequests.addAll(topUpRequestDAO.loadAll());

            TopUpRequestsDirty = false;

            userController.processApprovedTopUpCredits();
        }
    }

    public synchronized void reloadUsers() {
        users.clear();
        users.addAll(userDAO.loadAll());
    }

    public synchronized void reloadAuctions() {
        auctions.clear();
        auctions.addAll(auctionDAO.loadAll());
    }

    public synchronized void reloadTransactions() {
        transactions.clear();
        transactions.addAll(bidTransactionDAO.loadAll());
    }

    public synchronized void reloadPayments() {
        payments.clear();
        payments.addAll(paymentDAO.loadAll());
    }

    public synchronized void reloadNotifications() {
        notifications.clear();
        notifications.addAll(notificationDAO.loadAll());
    }

    public synchronized void reloadTopUpRequests() {
        topUpRequests.clear();
        topUpRequests.addAll(topUpRequestDAO.loadAll());
        userController.processApprovedTopUpCredits();
    }

    public UserController getUserController() {
        return userController;
    }

    public AuctionController getAuctionController() {
        return auctionController;
    }

    public AutoBidController getAutoBidController() {
        return autoBidController;
    }

    public ItemController getItemController() {
        return itemController;
    }

    public synchronized List<User> getUsers() {
        return new ArrayList<>(users);
    }

    public synchronized List<User> getUsersDirect() {
        return userDAO.loadAll();
    }

    public synchronized User findUserByUsername(String username) {
        return userDAO.findByUsername(username);
    }

    public synchronized User findUserByCredentials(String username, String password, String role) {
        return userDAO.findByCredentials(username, password, role);
    }

    public synchronized boolean usernameExists(String username) {
        return userDAO.existsByUsername(username);
    }

    public synchronized List<Auction> getAuctions() {
        return new ArrayList<>(auctions);
    }

    public synchronized Auction findAuctionById(String auctionId) {
        return auctionDAO.findById(auctionId);
    }

    public synchronized List<Auction> searchAuctionsDirect(String keyword, String category) {
        return auctionDAO.search(keyword, category);
    }

    public synchronized List<Auction> getAuctionsForSellerDirect(String sellerUsername) {
        return auctionDAO.loadBySeller(sellerUsername);
    }

    public synchronized List<Auction> getAuctionsForBidderDirect(String bidderUsername) {
        return auctionDAO.loadByBidder(bidderUsername);
    }

    public synchronized List<Auction> getWonAuctionsForBidderDirect(String bidderUsername) {
        return auctionDAO.loadWonByBidder(bidderUsername);
    }

    public synchronized List<BidTransaction> getTransactions() {
        return new ArrayList<>(transactions);
    }

    public synchronized List<BidTransaction> getTransactionsDirect() {
        return bidTransactionDAO.loadAll();
    }

    public synchronized List<PaymentRecord> getPayments() {
        return new ArrayList<>(payments);
    }

    public synchronized List<PaymentRecord> getPaymentsDirect() {
        return paymentDAO.loadAll();
    }

    public synchronized List<NotificationRecord> getNotifications() {
        return new ArrayList<>(notifications);
    }

    public synchronized List<NotificationRecord> getNotificationsDirect() {
        return notificationDAO.loadAll();
    }

    public synchronized List<NotificationRecord> getNotificationsForUser(String username) {
        return notificationDAO.loadForUser(username);
    }

    public synchronized List<TopUpRequestRecord> getTopUpRequests() {
        return new ArrayList<>(topUpRequests);
    }

    public synchronized List<TopUpRequestRecord> getTopUpRequestsDirect() {
        return topUpRequestDAO.loadAll();
    }

    public synchronized void processApprovedTopUpCredits() {
        userController.processApprovedTopUpCredits();
    }

    public synchronized void saveUsers(List<User> values) {
        users.clear();
        users.addAll(values);
        userDAO.saveAll(users);
    }

    public synchronized void addUser(User user) {
        users.add(user);
        userDAO.insert(user);
    }

    public synchronized void updateUserWallet(User user) {
        userDAO.updateWalletBalance(user.getUsername(), user.getWalletBalance());

        markUsersDirty();

        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("USER_UPDATED", user.getUsername(), null));
    }

    public synchronized void saveAuctions(List<Auction> values) {
        auctions.clear();
        auctions.addAll(values);
        auctionDAO.saveAll(auctions);
    }

    public synchronized void addAuction(Auction auction) {
        auctions.add(0, auction);
        auctionDAO.insert(auction);

        markAuctionsDirty();

        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("AUCTION_UPDATED", "ALL", auction.getId()));
    }

    public synchronized void updateAuction(Auction auction) {
        auctionDAO.updateAuction(auction);

        markAuctionsDirty();

        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("AUCTION_UPDATED", "ALL", auction.getId()));
    }

    public synchronized void insertAuctionBid(String auctionId, BidTransaction bid) {
        auctionDAO.insertBid(auctionId, bid);

        markAuctionsDirty();

        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("AUCTION_UPDATED", "ALL", auctionId));
    }

    public synchronized void replaceAuctionAutoBids(Auction auction) {
        auctionDAO.replaceAutoBids(auction.getId(), auction.getAutoBids());

        markAuctionsDirty();

        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("AUCTION_UPDATED", auction.getHighestBidder(), auction.getId()));
    }

    public synchronized void saveTransactions(List<BidTransaction> values) {
        transactions.clear();
        transactions.addAll(values);
        bidTransactionDAO.saveAll(transactions);
    }

    public synchronized void addTransaction(BidTransaction transaction) {
        transactions.add(0, transaction);
        bidTransactionDAO.insert(transaction);
        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("TRANSACTION_UPDATED", transaction.getActorUsername(), transaction.getReferenceId()));
    }

    public synchronized void savePayments(List<PaymentRecord> values) {
        payments.clear();
        payments.addAll(values);
        paymentDAO.saveAll(payments);
    }

    public synchronized void addPayment(PaymentRecord payment) {
        payments.add(0, payment);
        paymentDAO.insert(payment);
        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("PAYMENT_UPDATED", payment.getBuyerUsername(), payment.getAuctionId()));
        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("PAYMENT_UPDATED", payment.getSellerUsername(), payment.getAuctionId()));
    }

    public synchronized void saveNotifications(List<NotificationRecord> values) {
        notifications.clear();
        notifications.addAll(values);
        notificationDAO.saveAll(notifications);
    }

    public synchronized void addNotification(NotificationRecord notification) {
        notifications.add(0, notification);
        notificationDAO.insert(notification);
        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("NOTIFICATION_UPDATED", notification.getUsername(), null));
    }

    public synchronized void saveTopUpRequests(List<TopUpRequestRecord> values) {
        topUpRequests.clear();
        topUpRequests.addAll(values);
        topUpRequestDAO.saveAll(topUpRequests);
    }

    public synchronized void addTopUpRequest(TopUpRequestRecord request) {
        topUpRequests.add(0, request);
        topUpRequestDAO.insert(request);

        markTopUpsDirty();

        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("TOP_UP_UPDATED", request.getUsername(), null));
    }

    public synchronized void markTopUpApproved(TopUpRequestRecord request) {
        topUpRequestDAO.markApproved(request.getId(), request.getApprovedBy(), request.getApprovedAt());

        markUsersDirty();
        markTopUpsDirty();

        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("TOP_UP_UPDATED", request.getUsername(), null));
    }

    public synchronized void markTopUpCredited(TopUpRequestRecord request) {
        topUpRequestDAO.markCredited(request.getId(), request.getCreditedAt());

        markTopUpsDirty();

        ClientSubscriptionRegistry.broadcast(new RealtimeEvent("TOP_UP_UPDATED", request.getUsername(), null));
    }

    public synchronized void persistAll() {
        userDAO.saveAll(users);
        auctionDAO.saveAll(auctions);
        bidTransactionDAO.saveAll(transactions);
        paymentDAO.saveAll(payments);
        notificationDAO.saveAll(notifications);
        topUpRequestDAO.saveAll(topUpRequests);
    }

    @SuppressWarnings("unchecked")
    private void migrateLegacyDataIfDatabaseEmpty() {
        if (!users.isEmpty() || !auctions.isEmpty() || !transactions.isEmpty() || !payments.isEmpty() || !notifications.isEmpty() || !topUpRequests.isEmpty()) {
            return;
        }

        List<User> legacyUsers = ObjectFileStore.readList(Path.of("data", "users.dat"));
        List<Auction> legacyAuctions = ObjectFileStore.readList(Path.of("data", "auctions.dat"));
        List<BidTransaction> legacyTransactions = ObjectFileStore.readList(Path.of("data", "transactions.dat"));
        List<PaymentRecord> legacyPayments = ObjectFileStore.readList(Path.of("data", "payments.dat"));
        List<NotificationRecord> legacyNotifications = ObjectFileStore.readList(Path.of("data", "notifications.dat"));
        List<TopUpRequestRecord> legacyTopUpRequests = ObjectFileStore.readList(Path.of("data", "topup-requests.dat"));

        if (legacyUsers.isEmpty()
                && legacyAuctions.isEmpty()
                && legacyTransactions.isEmpty()
                && legacyPayments.isEmpty()
                && legacyNotifications.isEmpty()
                && legacyTopUpRequests.isEmpty()) {
            return;
        }

        users.clear();
        users.addAll(legacyUsers);
        auctions.clear();
        auctions.addAll(legacyAuctions);
        transactions.clear();
        transactions.addAll(legacyTransactions);
        payments.clear();
        payments.addAll(legacyPayments);
        notifications.clear();
        notifications.addAll(legacyNotifications);
        topUpRequests.clear();
        topUpRequests.addAll(legacyTopUpRequests);

        persistAll();
    }

    private void seedData() {
        users.clear();
        auctions.clear();
        transactions.clear();
        payments.clear();
        notifications.clear();
        topUpRequests.clear();

        
        users.add(new Seller("U-002", "seller", "seller123", "Linh Seller", 15_000_000));
        users.add(new Bidder("U-003", "bidder", "bidder123", "Minh Bidder", 120_000_000));
        users.add(new Seller("U-004", "seller2", "seller123", "An Seller", 18_000_000));
        users.add(new Bidder("U-005", "bidder2", "bidder123", "Trang Bidder", 95_000_000));

        Auction macbook = new Auction(
                "AUC-1001",
                "seller",
                itemController.createItem("Electronics", "IT-1001", "MacBook Pro M3 14 inch", "May con moi 98%, day du hop.", 28_000_000, LocalDateTime.now().plusHours(36), "Laptop premium")
        );
        macbook.addBid(new BidTransaction("BID", "bidder", macbook.getId(), "Manual bid", 29_500_000, LocalDateTime.now().minusHours(4)));
        macbook.addBid(new BidTransaction("BID", "bidder2", macbook.getId(), "Manual bid", 30_400_000, LocalDateTime.now().minusHours(1)));
        macbook.addOrReplaceAutoBid(new AutoBid("bidder", 32_000_000, 300_000));

        Auction vespa = new Auction(
                "AUC-1002",
                "seller2",
                itemController.createItem("Vehicle", "IT-1002", "Vespa Sprint 2022", "Odo 6,000 km, may dep.", 58_000_000, LocalDateTime.now().plusHours(72), "Scooter retro")
        );
        vespa.addBid(new BidTransaction("BID", "bidder", vespa.getId(), "Manual bid", 60_500_000, LocalDateTime.now().minusHours(2)));

        Auction art = new Auction(
                "AUC-1003",
                "seller",
                itemController.createItem("Art", "IT-1003", "Tranh son dau phong canh Da Lat", "Tac pham 90x120 cm.", 12_000_000, LocalDateTime.now().minusHours(2), "Large painting")
        );
        art.addBid(new BidTransaction("BID", "bidder", art.getId(), "Manual bid", 13_500_000, LocalDateTime.now().minusMinutes(15)));

        Auction watch = new Auction(
                "AUC-1004",
                "seller2",
                itemController.createItem("Luxury", "IT-1004", "Dong ho co Longines Heritage", "Automatic, full box.", 26_000_000, LocalDateTime.now().plusHours(48), "Luxury watch")
        );

        Auction collectible = new Auction(
                "AUC-1005",
                "seller",
                itemController.createItem("Collectible", "IT-1005", "Bo mo hinh xe F1 gioi han", "Ti le 1/18, gioi han 500 set.", 8_500_000, LocalDateTime.now().plusHours(18), "Collector item")
        );
        collectible.addOrReplaceAutoBid(new AutoBid("bidder2", 10_000_000, 200_000));

        auctions.add(macbook);
        auctions.add(vespa);
        auctions.add(art);
        auctions.add(watch);
        auctions.add(collectible);

        payments.add(new PaymentRecord("AUC-DEMO01", "bidder2", "seller2", 24_000_000, LocalDateTime.now().minusDays(2)));
        notifications.add(new NotificationRecord("ALL", "He thong san sang", "Backend server va data persistence da khoi tao.", LocalDateTime.now()));
    }

    private boolean ensureFixedAdminAccount() {
        for (User user : users) {
            if ("admin".equalsIgnoreCase(user.getUsername()) && "ADMIN".equalsIgnoreCase(user.getRole())) {
                if (!"admin".equals(user.getPassword())) {
                    user.setPassword("admin");
                    return true;
                }
                return false;
            }
        }

        users.add(0, new Admin("U-ADMIN", "admin", "admin", "System Admin", 0));
        return true;
    }
}
