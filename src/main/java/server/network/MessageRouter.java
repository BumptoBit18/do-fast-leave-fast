package server.network;

import app.model.UserRole;
import server.ServerMain;
import server.model.*;
import server.model.entity.User;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Dinh tuyen request va tra ve Map<String,Object> thay vi Java record,
 * de JsonCodec co the serialize duoc.
 */
public class MessageRouter {
    private final ServerMain server = ServerMain.getInstance();

    public shared.socket.SocketResponse route(shared.socket.SocketRequest request) {
        try {
            syncForAction(request.getAction());
            return switch (request.getAction()) {

                case "LOGIN" -> ok(userMap(
                        server.getUserController().login(
                                request.getUsername(), request.getPassword(), request.getRole())));

                case "REGISTER" -> ok(userMap(
                        server.getUserController().register(
                                request.getUsername(), request.getPassword(),
                                request.getFullName(), request.getRole())));

                case "GET_CURRENT_USER" -> ok(userMap(requireUser(request.getActorUsername())));

                case "GET_AUCTIONS" ->
                        ok(server.getAuctionController().listAuctions().stream().map(this::auctionMap).toList());

                case "GET_AUCTION_BY_ID" ->
                        ok(auctionMap(server.getAuctionController().getAuctionById(request.getAuctionId())));

                case "SEARCH_AUCTIONS" ->
                        ok(server.getAuctionController()
                                .searchAuctions(request.getKeyword(), request.getCategory())
                                .stream().map(this::auctionMap).toList());

                case "GET_USERS" ->
                        ok(server.getUsersDirect().stream().map(this::userMap).toList());

                case "GET_AUCTIONS_FOR_SELLER" ->
                        ok(server.getAuctionController()
                                .getAuctionsForSeller(request.getUsername())
                                .stream().map(this::auctionMap).toList());

                case "GET_AUCTIONS_FOR_BIDDER" ->
                        ok(server.getAuctionController()
                                .getAuctionsForBidder(request.getUsername())
                                .stream().map(this::auctionMap).toList());

                case "GET_WON_AUCTIONS" ->
                        ok(server.getAuctionController()
                                .getWonAuctions(request.getUsername())
                                .stream().map(this::auctionMap).toList());

                case "CREATE_AUCTION" ->
                        ok(auctionMap(server.getAuctionController().createAuction(
                                request.getUsername(), request.getTitle(), request.getCategory(),
                                request.getDescription(), request.getStartPrice(),
                                request.getDurationHours(), request.getImageHint())));

                case "PLACE_BID" ->
                        ok(auctionMap(server.getAuctionController().placeBid(
                                request.getAuctionId(), request.getActorUsername(), request.getAmount())));

                case "ENABLE_AUTO_BID" ->
                        ok(auctionMap(server.getAutoBidController().enableAutoBid(
                                request.getAuctionId(), request.getActorUsername(),
                                request.getMaxAmount(), request.getIncrementStep())));

                case "SUBMIT_TOP_UP" ->
                        ok(topUpMap(server.getUserController().submitTopUpRequest(
                                request.getActorUsername(), request.getAmount(),
                                request.getBankName(), request.getAccountName(), request.getAccountNumber())));

                case "APPROVE_TOP_UP" -> {
                    server.getUserController().approveTopUpRequest(
                            request.getRequestId(), request.getActorUsername());
                    yield ok(userMap(requireUser(request.getActorUsername())));
                }

                case "GET_TOP_UP_REQUESTS" ->
                        ok(server.getTopUpRequestsDirect().stream().map(this::topUpMap).toList());

                case "PAY_AUCTION" ->
                        ok(auctionMap(server.getAuctionController().payAuction(
                                request.getAuctionId(), request.getActorUsername())));

                case "CANCEL_AUCTION" ->
                        ok(auctionMap(server.getAuctionController().cancelAuction(
                                request.getAuctionId(), request.getActorUsername())));

                case "GET_NOTIFICATIONS" ->
                        ok(server.getNotificationsDirect().stream().map(this::notifMap).toList());

                case "GET_NOTIFICATIONS_FOR_USER" ->
                        ok(server.getNotificationsForUser(request.getActorUsername())
                                .stream().map(this::notifMap).toList());

                case "GET_PAYMENTS" ->
                        ok(server.getPaymentsDirect().stream().map(this::paymentMap).toList());

                case "GET_TRANSACTIONS" ->
                        ok(server.getTransactionsDirect().stream().map(this::transactionMap).toList());

                default -> shared.socket.SocketResponse.error("Action khong duoc ho tro: " + request.getAction());
            };
        } catch (Exception ex) {
            String msg = ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName();
            return shared.socket.SocketResponse.error(msg);
        }
    }

    // ── sync helpers ─────────────────────────────────────────

    private void syncForAction(String action) {
        switch (action) {
            case "LOGIN", "REGISTER"
                    -> server.reLoadUsersIfNeeded();
            case "GET_AUCTIONS", "GET_AUCTION_BY_ID", "SEARCH_AUCTIONS",
                 "GET_AUCTIONS_FOR_SELLER", "GET_AUCTIONS_FOR_BIDDER", "GET_WON_AUCTIONS"
                    -> server.reloadAuctionIfNeeded();
            case "CREATE_AUCTION", "PLACE_BID", "ENABLE_AUTO_BID", "PAY_AUCTION", "CANCEL_AUCTION" -> {
                server.reloadAuctions();
                server.reloadUsers();
            }
            case "SUBMIT_TOP_UP", "APPROVE_TOP_UP" -> {
                server.reloadTopUpRequests();
                server.reloadUsers();
            }
            case "GET_CURRENT_USER", "GET_NOTIFICATIONS_FOR_USER",
                 "GET_TOP_UP_REQUESTS", "GET_USERS"
                    -> server.processApprovedTopUpCredits();
            case "GET_NOTIFICATIONS", "GET_PAYMENTS", "GET_TRANSACTIONS" -> { /* chi doc */ }
            default -> server.reloadAllFromDisk();
        }
    }

    private User requireUser(String username) {
        User user = server.findUserByUsername(username);
        if (user == null) throw new IllegalArgumentException("Khong tim thay nguoi dung: " + username);
        return user;
    }

    private static shared.socket.SocketResponse ok(Object payload) {
        return shared.socket.SocketResponse.ok(payload);
    }

    // ── serialize sang Map (JsonCodec chi hieu Map, List, primitives) ──

    private Map<String, Object> userMap(User u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            u.getId());
        m.put("username",      u.getUsername());
        m.put("password",      u.getPassword());
        m.put("role",          UserRole.valueOf(u.getRole()).name());
        m.put("fullName",      u.getFullName());
        m.put("walletBalance", u.getWalletBalance());
        return m;
    }

    private Map<String, Object> auctionMap(Auction a) {
        List<Map<String, Object>> bids = a.getBidHistory().stream().map(b -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bidderUsername", b.getActorUsername());
            m.put("amount",         b.getAmount());
            m.put("time",           str(b.getTime()));
            return m;
        }).toList();

        List<Map<String, Object>> autoBids = a.getAutoBids().stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("bidderUsername", r.getBidderUsername());
            m.put("maxAmount",      r.getMaxAmount());
            m.put("incrementStep",  r.getIncrementStep());
            return m;
        }).toList();

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",                 a.getId());
        m.put("sellerUsername",     a.getSellerUsername());
        m.put("title",              a.getItem().getName());
        m.put("category",           a.getItem().getCategory());
        m.put("description",        a.getItem().getDescription());
        m.put("startPrice",         a.getItem().getStartingPrice());
        m.put("currentPrice",       a.getCurrentPrice());
        m.put("endTime",            str(a.getItem().getEndTime()));
        m.put("imageHint",          a.getItem().getImageHint());
        m.put("cancelled",          a.isCancelled());
        m.put("paid",               a.isPaid());
        m.put("antiSnipeTriggered", a.isAntiSnipeTriggered());
        m.put("closeNotified",      a.isCloseNotified());
        m.put("bidHistory",         bids);
        m.put("autoBidRules",       autoBids);
        return m;
    }

    private Map<String, Object> notifMap(NotificationRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("username", r.getUsername());
        m.put("title",    r.getTitle());
        m.put("message",  r.getMessage());
        m.put("time",     str(r.getTime()));
        return m;
    }

    private Map<String, Object> paymentMap(PaymentRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("auctionId",      r.getAuctionId());
        m.put("buyerUsername",  r.getBuyerUsername());
        m.put("sellerUsername", r.getSellerUsername());
        m.put("amount",         r.getAmount());
        m.put("paidAt",         str(r.getPaidAt()));
        return m;
    }

    private Map<String, Object> transactionMap(BidTransaction r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type",          r.getType());
        m.put("actorUsername", r.getActorUsername());
        m.put("referenceId",   r.getReferenceId());
        m.put("description",   r.getDescription());
        m.put("time",          str(r.getTime()));
        return m;
    }

    private Map<String, Object> topUpMap(TopUpRequestRecord r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id",            r.getId());
        m.put("username",      r.getUsername());
        m.put("amount",        r.getAmount());
        m.put("bankName",      r.getBankName());
        m.put("accountName",   r.getAccountName());
        m.put("accountNumber", r.getAccountNumber());
        m.put("requestedAt",   str(r.getRequestedAt()));
        m.put("status",        r.getStatus());
        m.put("approvedAt",    str(r.getApprovedAt()));
        m.put("approvedBy",    r.getApprovedBy());
        m.put("creditedAt",    str(r.getCreditedAt()));
        return m;
    }

    private static String str(LocalDateTime dt) {
        return dt == null ? null : dt.toString();
    }
}
