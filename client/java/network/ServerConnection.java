package network;

import app.model.AppUser;
import app.model.AutoBidRule;
import app.model.AuctionLot;
import app.model.BidRecord;
import app.model.NotificationItem;
import app.model.PaymentRecord;
import app.model.TopUpRequestRecord;
import app.model.TransactionRecord;
import app.model.UserRole;
import app.service.AuctionPlatformService;
import shared.socket.AutoBidPayload;
import shared.socket.AuctionPayload;
import shared.socket.BidPayload;
import shared.socket.NotificationPayload;
import shared.socket.PaymentPayload;
import shared.socket.SocketRequest;
import shared.socket.SocketResponse;
import shared.socket.TopUpRequestPayload;
import shared.socket.TransactionPayload;
import shared.socket.UserPayload;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;
import java.util.Locale;

public class ServerConnection {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 10_000;

    private final AuctionPlatformService service;
    private final String serverHost;
    private final int serverPort;
    private AppUser currentUser;

    public ServerConnection() {
        this.serverHost = readValue("auction.server.host", "AUCTION_SERVER_HOST", "127.0.0.1");
        this.serverPort = Integer.parseInt(readValue("auction.server.port", "AUCTION_SERVER_PORT", "5050"));
        this.service = new AuctionPlatformService(this);
    }

    public AuctionPlatformService getService() {
        return service;
    }

    public AppUser login(String username, String password, UserRole role) {
        SocketRequest request = new SocketRequest();
        request.setAction("LOGIN");
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(role.name());
        currentUser = mapUser(send(request, UserPayload.class));
        return currentUser;
    }

    public AppUser register(String username, String password, String fullName, UserRole role) {
        SocketRequest request = new SocketRequest();
        request.setAction("REGISTER");
        request.setUsername(username);
        request.setPassword(password);
        request.setFullName(fullName);
        request.setRole(role.name());
        return mapUser(send(request, UserPayload.class));
    }

    public void logout() {
        currentUser = null;
    }

    public AppUser getCurrentUser() {
        if (currentUser == null) {
            return null;
        }
        SocketRequest request = new SocketRequest();
        request.setAction("GET_CURRENT_USER");
        request.setActorUsername(currentUser.getUsername());
        currentUser = mapUser(send(request, UserPayload.class));
        return currentUser;
    }

    public List<AuctionLot> getAuctions() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_AUCTIONS");
        return mapAuctions(sendList(request));
    }

    public AuctionLot getAuctionById(String auctionId) {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_AUCTION_BY_ID");
        request.setAuctionId(auctionId);
        return mapAuction(send(request, AuctionPayload.class));
    }

    public List<AuctionLot> searchAuctions(String keyword, String category) {
        SocketRequest request = new SocketRequest();
        request.setAction("SEARCH_AUCTIONS");
        request.setKeyword(keyword);
        request.setCategory(category);
        return mapAuctions(sendList(request));
    }

    public List<AppUser> getUsers() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_USERS");
        return mapUsers(sendList(request));
    }

    public List<AuctionLot> getAuctionsForSeller(String sellerUsername) {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_AUCTIONS_FOR_SELLER");
        request.setUsername(sellerUsername);
        return mapAuctions(sendList(request));
    }

    public List<AuctionLot> getAuctionsForBidder(String bidderUsername) {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_AUCTIONS_FOR_BIDDER");
        request.setUsername(bidderUsername);
        return mapAuctions(sendList(request));
    }

    public List<AuctionLot> getWonAuctions(String bidderUsername) {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_WON_AUCTIONS");
        request.setUsername(bidderUsername);
        return mapAuctions(sendList(request));
    }

    public AuctionLot createAuction(String sellerUsername, String title, String category, String description, double startPrice, int durationHours, String imageHint) {
        SocketRequest request = new SocketRequest();
        request.setAction("CREATE_AUCTION");
        request.setUsername(sellerUsername);
        request.setTitle(title);
        request.setCategory(category);
        request.setDescription(description);
        request.setStartPrice(startPrice);
        request.setDurationHours(durationHours);
        request.setImageHint(imageHint);
        return mapAuction(send(request, AuctionPayload.class));
    }

    public AuctionLot placeBid(String auctionId, double amount) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("PLACE_BID");
        request.setAuctionId(auctionId);
        request.setActorUsername(currentUser.getUsername());
        request.setAmount(amount);
        return mapAuction(send(request, AuctionPayload.class));
    }

    public AuctionLot enableAutoBid(String auctionId, double maxAmount, double incrementStep) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("ENABLE_AUTO_BID");
        request.setAuctionId(auctionId);
        request.setActorUsername(currentUser.getUsername());
        request.setMaxAmount(maxAmount);
        request.setIncrementStep(incrementStep);
        return mapAuction(send(request, AuctionPayload.class));
    }

    public AppUser topUpWallet(double amount) {
        throw new IllegalStateException("Nap tien vao vi can admin xac nhan.");
    }

    public TopUpRequestRecord submitTopUpRequest(double amount, String bankName, String accountName, String accountNumber) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("SUBMIT_TOP_UP");
        request.setActorUsername(currentUser.getUsername());
        request.setAmount(amount);
        request.setBankName(bankName);
        request.setAccountName(accountName);
        request.setAccountNumber(accountNumber);
        return mapTopUpRequest(send(request, TopUpRequestPayload.class));
    }

    public AppUser approveTopUpRequest(String requestId) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("APPROVE_TOP_UP");
        request.setActorUsername(currentUser.getUsername());
        request.setRequestId(requestId);
        currentUser = mapUser(send(request, UserPayload.class));
        return currentUser;
    }

    public List<TopUpRequestRecord> getTopUpRequests() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_TOP_UP_REQUESTS");
        return mapTopUpRequests(sendList(request));
    }

    public AuctionLot payForAuction(String auctionId) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("PAY_AUCTION");
        request.setAuctionId(auctionId);
        request.setActorUsername(currentUser.getUsername());
        return mapAuction(send(request, AuctionPayload.class));
    }

    public AuctionLot cancelAuction(String auctionId) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("CANCEL_AUCTION");
        request.setAuctionId(auctionId);
        request.setActorUsername(currentUser.getUsername());
        return mapAuction(send(request, AuctionPayload.class));
    }

    public List<NotificationItem> getNotifications() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_NOTIFICATIONS");
        return mapNotifications(sendList(request));
    }

    public List<NotificationItem> getNotificationsForCurrentUser() {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("GET_NOTIFICATIONS_FOR_USER");
        request.setActorUsername(currentUser.getUsername());
        return mapNotifications(sendList(request));
    }

    public List<PaymentRecord> getPayments() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_PAYMENTS");
        return mapPayments(sendList(request));
    }

    public List<TransactionRecord> getTransactions() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_TRANSACTIONS");
        return mapTransactions(sendList(request));
    }

    public AppUser refreshCurrentUser() {
        return getCurrentUser();
    }

    private <T> T send(SocketRequest request, Class<T> payloadType) {
        SocketResponse response = sendInternal(request);
        Object payload = response.getPayload();
        if (payload == null) {
            return null;
        }
        return payloadType.cast(payload);
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> sendList(SocketRequest request) {
        SocketResponse response = sendInternal(request);
        Object payload = response.getPayload();
        return payload == null ? List.of() : (List<T>) payload;
    }

    private SocketResponse sendInternal(SocketRequest request) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverHost, serverPort), DEFAULT_CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(DEFAULT_READ_TIMEOUT_MS);

            try (ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                 ObjectInputStream input = new ObjectInputStream(socket.getInputStream())) {
                output.writeObject(request);
                output.flush();

                Object raw = input.readObject();
                if (!(raw instanceof SocketResponse response)) {
                    throw new IllegalStateException("Phan hoi tu server khong hop le.");
                }
                if (!response.isSuccess()) {
                    throw new IllegalStateException(response.getMessage());
                }
                return response;
            }
        } catch (IllegalStateException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalStateException(
                    "Khong ket noi duoc toi auction server %s:%d. Hay kiem tra server da chay va client dang tro dung host/port."
                            .formatted(serverHost, serverPort),
                    ex
            );
        }
    }

    private AppUser mapUser(UserPayload user) {
        return new AppUser(
                user.id(),
                user.username(),
                user.password(),
                UserRole.valueOf(user.role().toUpperCase(Locale.ROOT)),
                user.fullName(),
                user.walletBalance()
        );
    }

    private List<AppUser> mapUsers(List<UserPayload> users) {
        return users.stream().map(this::mapUser).toList();
    }

    private AuctionLot mapAuction(AuctionPayload auction) {
        AuctionLot lot = new AuctionLot(
                auction.id(),
                auction.sellerUsername(),
                auction.title(),
                auction.category(),
                auction.description(),
                auction.startPrice(),
                auction.endTime(),
                auction.imageHint()
        );
        for (BidPayload bid : auction.bidHistory()) {
            lot.placeBid(new BidRecord(bid.bidderUsername(), bid.amount(), bid.time()));
        }
        for (AutoBidPayload rule : auction.autoBidRules()) {
            lot.addAutoBidRule(new AutoBidRule(rule.bidderUsername(), rule.maxAmount(), rule.incrementStep()));
        }
        lot.setCancelled(auction.cancelled());
        lot.setPaid(auction.paid());
        lot.setAntiSnipeTriggered(auction.antiSnipeTriggered());
        lot.setCloseNotified(auction.closeNotified());
        lot.setEndTime(auction.endTime());
        return lot;
    }

    private List<AuctionLot> mapAuctions(List<AuctionPayload> auctions) {
        return auctions.stream().map(this::mapAuction).toList();
    }

    private NotificationItem mapNotification(NotificationPayload payload) {
        return new NotificationItem(payload.username(), payload.title(), payload.message(), payload.time());
    }

    private List<NotificationItem> mapNotifications(List<NotificationPayload> payloads) {
        return payloads.stream().map(this::mapNotification).toList();
    }

    private PaymentRecord mapPayment(PaymentPayload payload) {
        return new PaymentRecord(payload.auctionId(), payload.buyerUsername(), payload.sellerUsername(), payload.amount(), payload.paidAt());
    }

    private List<PaymentRecord> mapPayments(List<PaymentPayload> payloads) {
        return payloads.stream().map(this::mapPayment).toList();
    }

    private TransactionRecord mapTransaction(TransactionPayload payload) {
        return new TransactionRecord(payload.type(), payload.actorUsername(), payload.referenceId(), payload.description(), payload.time());
    }

    private List<TransactionRecord> mapTransactions(List<TransactionPayload> payloads) {
        return payloads.stream().map(this::mapTransaction).toList();
    }

    private TopUpRequestRecord mapTopUpRequest(TopUpRequestPayload payload) {
        return new TopUpRequestRecord(
                payload.id(),
                payload.username(),
                payload.amount(),
                payload.bankName(),
                payload.accountName(),
                payload.accountNumber(),
                payload.requestedAt(),
                payload.status(),
                payload.approvedAt(),
                payload.approvedBy(),
                payload.creditedAt()
        );
    }

    private List<TopUpRequestRecord> mapTopUpRequests(List<TopUpRequestPayload> payloads) {
        return payloads.stream().map(this::mapTopUpRequest).toList();
    }

    private void ensureCurrentUser() {
        if (currentUser == null) {
            throw new IllegalStateException("Chua dang nhap.");
        }
    }

    private String readValue(String propertyKey, String envKey, String defaultValue) {
        String propertyValue = System.getProperty(propertyKey);
        if (propertyValue != null && !propertyValue.isBlank()) {
            return propertyValue.trim();
        }

        String envValue = System.getenv(envKey);
        if (envValue != null && !envValue.isBlank()) {
            return envValue.trim();
        }

        return defaultValue;
    }
}
