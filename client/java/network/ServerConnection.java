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
import shared.json.JsonCodec;
import shared.socket.RealtimeEvent;
import shared.socket.SocketRequest;
import shared.socket.SocketResponse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class ServerConnection implements MessageListener {
    private static final int DEFAULT_CONNECT_TIMEOUT_MS = 5_000;
    private static final int DEFAULT_READ_TIMEOUT_MS = 10_000;

    private final AuctionPlatformService service;
    private final String serverHost;
    private final int serverPort;
    private final ServerEventClient eventClient;
    private final List<MessageListener> messageListeners = new CopyOnWriteArrayList<>();
    private AppUser currentUser;

    public ServerConnection() {
        this.serverHost = readValue("auction.server.host", "AUCTION_SERVER_HOST", "127.0.0.1");
        this.serverPort = Integer.parseInt(readValue("auction.server.port", "AUCTION_SERVER_PORT", "5050"));
        this.service = new AuctionPlatformService(this);
        this.eventClient = new ServerEventClient(serverHost, serverPort, this);
    }

    public AuctionPlatformService getService() {
        return service;
    }

    public void addMessageListener(MessageListener listener) {
        messageListeners.add(listener);
    }

    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }

    @Override
    public void onMessage(RealtimeEvent event) {
        service.handleServerEvent(event);
        for (MessageListener listener : new ArrayList<>(messageListeners)) {
            listener.onMessage(event);
        }
    }

    public AppUser login(String username, String password, UserRole role) {
        SocketRequest request = new SocketRequest();
        request.setAction("LOGIN");
        request.setUsername(username);
        request.setPassword(password);
        request.setRole(role.name());
        currentUser = mapUser(sendObject(request));
        eventClient.connect(currentUser.getUsername());
        return currentUser;
    }

    public AppUser register(String username, String password, String fullName, UserRole role) {
        SocketRequest request = new SocketRequest();
        request.setAction("REGISTER");
        request.setUsername(username);
        request.setPassword(password);
        request.setFullName(fullName);
        request.setRole(role.name());
        return mapUser(sendObject(request));
    }

    public void logout() {
        currentUser = null;
        eventClient.disconnect();
    }

    public AppUser getCurrentUser() {
        if (currentUser == null) {
            return null;
        }
        SocketRequest request = new SocketRequest();
        request.setAction("GET_CURRENT_USER");
        request.setActorUsername(currentUser.getUsername());
        currentUser = mapUser(sendObject(request));
        return currentUser;
    }

    public List<AuctionLot> getAuctions() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_AUCTIONS");
        return mapAuctions(sendObjectList(request));
    }

    public AuctionLot getAuctionById(String auctionId) {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_AUCTION_BY_ID");
        request.setAuctionId(auctionId);
        return mapAuction(sendObject(request));
    }

    public List<AuctionLot> searchAuctions(String keyword, String category) {
        SocketRequest request = new SocketRequest();
        request.setAction("SEARCH_AUCTIONS");
        request.setKeyword(keyword);
        request.setCategory(category);
        return mapAuctions(sendObjectList(request));
    }

    public List<AppUser> getUsers() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_USERS");
        return mapUsers(sendObjectList(request));
    }

    public List<AuctionLot> getAuctionsForSeller(String sellerUsername) {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_AUCTIONS_FOR_SELLER");
        request.setUsername(sellerUsername);
        return mapAuctions(sendObjectList(request));
    }

    public List<AuctionLot> getAuctionsForBidder(String bidderUsername) {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_AUCTIONS_FOR_BIDDER");
        request.setUsername(bidderUsername);
        return mapAuctions(sendObjectList(request));
    }

    public List<AuctionLot> getWonAuctions(String bidderUsername) {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_WON_AUCTIONS");
        request.setUsername(bidderUsername);
        return mapAuctions(sendObjectList(request));
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
        return mapAuction(sendObject(request));
    }

    public AuctionLot placeBid(String auctionId, double amount) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("PLACE_BID");
        request.setAuctionId(auctionId);
        request.setActorUsername(currentUser.getUsername());
        request.setAmount(amount);
        return mapAuction(sendObject(request));
    }

    public AuctionLot enableAutoBid(String auctionId, double maxAmount, double incrementStep) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("ENABLE_AUTO_BID");
        request.setAuctionId(auctionId);
        request.setActorUsername(currentUser.getUsername());
        request.setMaxAmount(maxAmount);
        request.setIncrementStep(incrementStep);
        return mapAuction(sendObject(request));
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
        return mapTopUpRequest(sendObject(request));
    }

    public AppUser approveTopUpRequest(String requestId) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("APPROVE_TOP_UP");
        request.setActorUsername(currentUser.getUsername());
        request.setRequestId(requestId);
        currentUser = mapUser(sendObject(request));
        return currentUser;
    }

    public List<TopUpRequestRecord> getTopUpRequests() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_TOP_UP_REQUESTS");
        return mapTopUpRequests(sendObjectList(request));
    }

    public AuctionLot payForAuction(String auctionId) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("PAY_AUCTION");
        request.setAuctionId(auctionId);
        request.setActorUsername(currentUser.getUsername());
        return mapAuction(sendObject(request));
    }

    public AuctionLot cancelAuction(String auctionId) {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("CANCEL_AUCTION");
        request.setAuctionId(auctionId);
        request.setActorUsername(currentUser.getUsername());
        return mapAuction(sendObject(request));
    }

    public List<NotificationItem> getNotifications() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_NOTIFICATIONS");
        return mapNotifications(sendObjectList(request));
    }

    public List<NotificationItem> getNotificationsForCurrentUser() {
        ensureCurrentUser();
        SocketRequest request = new SocketRequest();
        request.setAction("GET_NOTIFICATIONS_FOR_USER");
        request.setActorUsername(currentUser.getUsername());
        return mapNotifications(sendObjectList(request));
    }

    public List<PaymentRecord> getPayments() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_PAYMENTS");
        return mapPayments(sendObjectList(request));
    }

    public List<TransactionRecord> getTransactions() {
        SocketRequest request = new SocketRequest();
        request.setAction("GET_TRANSACTIONS");
        return mapTransactions(sendObjectList(request));
    }

    public AppUser refreshCurrentUser() {
        return getCurrentUser();
    }

    private Map<String, Object> sendObject(SocketRequest request) {
        SocketResponse response = sendInternal(request);
        Object payload = response.getPayload();
        return payload == null ? null : castMap(payload);
    }

    private List<Map<String, Object>> sendObjectList(SocketRequest request) {
        SocketResponse response = sendInternal(request);
        Object payload = response.getPayload();
        return payload == null ? List.of() : castList(payload);
    }

    @SuppressWarnings("unchecked")
    private SocketResponse sendInternal(SocketRequest request) {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(serverHost, serverPort), DEFAULT_CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(DEFAULT_READ_TIMEOUT_MS);

            try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                 BufferedReader input = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                output.write(JsonCodec.toJson(request.toMap()));
                output.newLine();
                output.flush();

                String raw = input.readLine();
                if (raw == null || raw.isBlank()) {
                    throw new IllegalStateException("Phan hoi tu server khong hop le.");
                }

                Object decoded = JsonCodec.fromJson(raw);
                if (!(decoded instanceof Map<?, ?> values)) {
                    throw new IllegalStateException("Phan hoi tu server khong hop le.");
                }

                SocketResponse response = SocketResponse.fromMap((Map<String, Object>) values);
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

    private AppUser mapUser(Map<String, Object> user) {
        return new AppUser(
                stringValue(user.get("id")),
                stringValue(user.get("username")),
                stringValue(user.get("password")),
                UserRole.valueOf(stringValue(user.get("role")).toUpperCase(Locale.ROOT)),
                stringValue(user.get("fullName")),
                doubleValue(user.get("walletBalance"))
        );
    }

    private List<AppUser> mapUsers(List<Map<String, Object>> users) {
        return users.stream().map(this::mapUser).toList();
    }

    private AuctionLot mapAuction(Map<String, Object> auction) {
        AuctionLot lot = new AuctionLot(
                stringValue(auction.get("id")),
                stringValue(auction.get("sellerUsername")),
                stringValue(auction.get("title")),
                stringValue(auction.get("category")),
                stringValue(auction.get("description")),
                doubleValue(auction.get("startPrice")),
                parseTime(auction.get("endTime")),
                stringValue(auction.get("imageHint"))
        );
        for (Map<String, Object> bid : castList(auction.get("bidHistory"))) {
            lot.placeBid(new BidRecord(
                    stringValue(bid.get("bidderUsername")),
                    doubleValue(bid.get("amount")),
                    parseTime(bid.get("time"))
            ));
        }
        for (Map<String, Object> rule : castList(auction.get("autoBidRules"))) {
            lot.addAutoBidRule(new AutoBidRule(
                    stringValue(rule.get("bidderUsername")),
                    doubleValue(rule.get("maxAmount")),
                    doubleValue(rule.get("incrementStep"))
            ));
        }
        lot.setCancelled(booleanValue(auction.get("cancelled")));
        lot.setPaid(booleanValue(auction.get("paid")));
        lot.setAntiSnipeTriggered(booleanValue(auction.get("antiSnipeTriggered")));
        lot.setCloseNotified(booleanValue(auction.get("closeNotified")));
        lot.setEndTime(parseTime(auction.get("endTime")));
        return lot;
    }

    private List<AuctionLot> mapAuctions(List<Map<String, Object>> auctions) {
        return auctions.stream().map(this::mapAuction).toList();
    }

    private NotificationItem mapNotification(Map<String, Object> payload) {
        return new NotificationItem(
                stringValue(payload.get("username")),
                stringValue(payload.get("title")),
                stringValue(payload.get("message")),
                parseTime(payload.get("time"))
        );
    }

    private List<NotificationItem> mapNotifications(List<Map<String, Object>> payloads) {
        return payloads.stream().map(this::mapNotification).toList();
    }

    private PaymentRecord mapPayment(Map<String, Object> payload) {
        return new PaymentRecord(
                stringValue(payload.get("auctionId")),
                stringValue(payload.get("buyerUsername")),
                stringValue(payload.get("sellerUsername")),
                doubleValue(payload.get("amount")),
                parseTime(payload.get("paidAt"))
        );
    }

    private List<PaymentRecord> mapPayments(List<Map<String, Object>> payloads) {
        return payloads.stream().map(this::mapPayment).toList();
    }

    private TransactionRecord mapTransaction(Map<String, Object> payload) {
        return new TransactionRecord(
                stringValue(payload.get("type")),
                stringValue(payload.get("actorUsername")),
                stringValue(payload.get("referenceId")),
                stringValue(payload.get("description")),
                parseTime(payload.get("time"))
        );
    }

    private List<TransactionRecord> mapTransactions(List<Map<String, Object>> payloads) {
        return payloads.stream().map(this::mapTransaction).toList();
    }

    private TopUpRequestRecord mapTopUpRequest(Map<String, Object> payload) {
        return new TopUpRequestRecord(
                stringValue(payload.get("id")),
                stringValue(payload.get("username")),
                doubleValue(payload.get("amount")),
                stringValue(payload.get("bankName")),
                stringValue(payload.get("accountName")),
                stringValue(payload.get("accountNumber")),
                parseTime(payload.get("requestedAt")),
                stringValue(payload.get("status")),
                parseTime(payload.get("approvedAt")),
                stringValue(payload.get("approvedBy")),
                parseTime(payload.get("creditedAt"))
        );
    }

    private List<TopUpRequestRecord> mapTopUpRequests(List<Map<String, Object>> payloads) {
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

    @SuppressWarnings("unchecked")
    private Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> castList(Object value) {
        return value == null ? List.of() : (List<Map<String, Object>>) value;
    }

    private String stringValue(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private double doubleValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0;
    }

    private boolean booleanValue(Object value) {
        return value instanceof Boolean bool && bool;
    }

    private LocalDateTime parseTime(Object value) {
        String raw = stringValue(value);
        return raw == null || raw.isBlank() ? null : LocalDateTime.parse(raw);
    }
}
