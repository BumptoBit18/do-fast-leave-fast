package server.network;

import app.model.UserRole;
import server.ServerMain;
import server.model.*;
import server.model.entity.User;
import shared.socket.SocketRequest;
import shared.socket.SocketResponse;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class MessageRouter {
    private final ServerMain server = ServerMain.getInstance();

    public SocketResponse route(SocketRequest request) {
        try {
            syncForAction(request.getAction());
            return switch (request.getAction()) {
                case "LOGIN" -> SocketResponse.ok(toUserPayload(
                        server.getUserController().login(request.getUsername(), request.getPassword(), request.getRole())
                ));
                case "REGISTER" -> SocketResponse.ok(toUserPayload(
                        server.getUserController().register(request.getUsername(), request.getPassword(), request.getFullName(), request.getRole())
                ));
                case "GET_CURRENT_USER" -> SocketResponse.ok(toUserPayload(findUser(request.getActorUsername())));
                case "GET_AUCTIONS" -> SocketResponse.ok(server.getAuctionController().listAuctions().stream().map(this::toAuctionPayload).toList());
                case "GET_AUCTION_BY_ID" -> SocketResponse.ok(toAuctionPayload(server.getAuctionController().getAuctionById(request.getAuctionId())));
                case "SEARCH_AUCTIONS" -> SocketResponse.ok(server.getAuctionController().searchAuctions(request.getKeyword(), request.getCategory()).stream().map(this::toAuctionPayload).toList());
                case "GET_USERS" -> {
                    requireAdmin(request.getActorUsername());
                    yield SocketResponse.ok(server.getUsersDirect().stream().map(this::toUserPayload).toList());
                }
                case "GET_AUCTIONS_FOR_SELLER" -> SocketResponse.ok(server.getAuctionController().getAuctionsForSeller(request.getUsername()).stream().map(this::toAuctionPayload).toList());
                case "GET_AUCTIONS_FOR_BIDDER" -> SocketResponse.ok(server.getAuctionController().getAuctionsForBidder(request.getUsername()).stream().map(this::toAuctionPayload).toList());
                case "GET_WON_AUCTIONS" -> SocketResponse.ok(server.getAuctionController().getWonAuctions(request.getUsername()).stream().map(this::toAuctionPayload).toList());
                case "CREATE_AUCTION" -> SocketResponse.ok(toAuctionPayload(server.getAuctionController().createAuction(
                        request.getUsername(),
                        request.getTitle(),
                        request.getCategory(),
                        request.getDescription(),
                        request.getStartPrice(),
                        request.getDurationHours(),
                        request.getImageHint()
                )));
                case "UPDATE_AUCTION" -> SocketResponse.ok(toAuctionPayload(server.getAuctionController().updateAuction(
                        request.getAuctionId(),
                        request.getActorUsername(),
                        request.getTitle(),
                        request.getCategory(),
                        request.getDescription(),
                        request.getStartPrice(),
                        request.getDurationHours(),
                        request.getImageHint()
                )));
                case "PLACE_BID" -> SocketResponse.ok(toAuctionPayload(server.getAuctionController().placeBid(
                        request.getAuctionId(),
                        request.getActorUsername(),
                        request.getAmount()
                )));
                case "ENABLE_AUTO_BID" -> SocketResponse.ok(toAuctionPayload(server.getAutoBidController().enableAutoBid(
                        request.getAuctionId(),
                        request.getActorUsername(),
                        request.getMaxAmount(),
                        request.getIncrementStep()
                )));
                case "SUBMIT_TOP_UP" -> SocketResponse.ok(toTopUpRequestPayload(server.getUserController().submitTopUpRequest(
                        request.getActorUsername(),
                        request.getAmount(),
                        request.getBankName(),
                        request.getAccountName(),
                        request.getAccountNumber()
                )));
                case "APPROVE_TOP_UP" -> {
                    requireAdmin(request.getActorUsername());
                    server.getUserController().approveTopUpRequest(request.getRequestId(), request.getActorUsername());
                    yield SocketResponse.ok(toUserPayload(findUser(request.getActorUsername())));
                }
                case "GET_TOP_UP_REQUESTS" -> {
                    requireAdmin(request.getActorUsername());
                    yield SocketResponse.ok(server.getTopUpRequestsDirect().stream().map(this::toTopUpRequestPayload).toList());
                }
                case "PAY_AUCTION" -> SocketResponse.ok(toAuctionPayload(server.getAuctionController().payAuction(
                        request.getAuctionId(),
                        request.getActorUsername()
                )));
                case "CANCEL_AUCTION" -> SocketResponse.ok(toAuctionPayload(server.getAuctionController().cancelAuction(
                        request.getAuctionId(),
                        request.getActorUsername()
                )));
                case "DELETE_AUCTION" -> {
                    server.getAuctionController().deleteAuction(request.getAuctionId(), request.getActorUsername());
                    yield SocketResponse.ok(null);
                }
                case "GET_NOTIFICATIONS" -> {
                    requireAdmin(request.getActorUsername());
                    yield SocketResponse.ok(server.getNotificationsDirect().stream().map(this::toNotificationPayload).toList());
                }
                case "GET_NOTIFICATIONS_FOR_USER" -> SocketResponse.ok(server.getNotificationsForUser(request.getActorUsername()).stream()
                        .map(this::toNotificationPayload)
                        .toList());
                case "GET_PAYMENTS" -> {
                    requireAdmin(request.getActorUsername());
                    yield SocketResponse.ok(server.getPaymentsDirect().stream().map(this::toPaymentPayload).toList());
                }
                case "GET_TRANSACTIONS" -> {
                    requireAdmin(request.getActorUsername());
                    yield SocketResponse.ok(server.getTransactionsDirect().stream().map(this::toTransactionPayload).toList());
                }
                default -> SocketResponse.error("Action khong duoc ho tro: " + request.getAction());
            };
        } catch (Exception ex) {
            System.err.println("Server action failed: " + request.getAction());
            ex.printStackTrace(System.err);
            return SocketResponse.error(ex.getMessage() == null ? "Loi server." : ex.getMessage());
        }
    }

    private void syncForAction(String action) {
        switch (action) {
            case "LOGIN", "REGISTER" -> server.reloadUsers();
            case "GET_AUCTIONS", "GET_AUCTION_BY_ID", "SEARCH_AUCTIONS", "GET_AUCTIONS_FOR_SELLER",
                    "GET_AUCTIONS_FOR_BIDDER", "GET_WON_AUCTIONS" -> server.reloadAuctions();
            case "CREATE_AUCTION", "UPDATE_AUCTION", "DELETE_AUCTION", "PLACE_BID", "ENABLE_AUTO_BID", "PAY_AUCTION", "CANCEL_AUCTION" -> {
                server.reloadAuctions();
                server.reloadUsers();
            }
            case "SUBMIT_TOP_UP", "APPROVE_TOP_UP" -> {
                server.reloadTopUpRequests();
                server.reloadUsers();
            }
            case "GET_CURRENT_USER", "GET_NOTIFICATIONS_FOR_USER", "GET_TOP_UP_REQUESTS", "GET_USERS" -> {
                server.processApprovedTopUpCredits();
            }
            case "GET_NOTIFICATIONS", "GET_PAYMENTS", "GET_TRANSACTIONS" -> {
            }
            default -> server.reloadAllFromDisk();
        }
    }

    private void requireAdmin(String username) {
        User user = findUser(username);
        if (!"ADMIN".equalsIgnoreCase(user.getRole())) {
            throw new server.exception.AuthorizationException("Chi admin moi duoc truy cap chuc nang nay.");
        }
    }

    private User findUser(String username) {
        User user = server.findUserByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Khong tim thay nguoi dung " + username);
        }
        return user;
    }

    private Map<String, Object> toUserPayload(User user) {
        return mapOf(
                "id", user.getId(),
                "username", user.getUsername(),
                "password", null,
                "role", UserRole.valueOf(user.getRole()).name(),
                "fullName", user.getFullName(),
                "walletBalance", user.getWalletBalance()
        );
    }

    private Map<String, Object> toAuctionPayload(Auction auction) {
        List<Map<String, Object>> bids = auction.getBidHistory().stream()
                .map(bid -> mapOf(
                        "bidderUsername", bid.getActorUsername(),
                        "amount", bid.getAmount(),
                        "time", toIso(bid.getTime())
                ))
                .toList();
        List<Map<String, Object>> autoBids = auction.getAutoBids().stream()
                .map(rule -> mapOf(
                        "bidderUsername", rule.getBidderUsername(),
                        "maxAmount", rule.getMaxAmount(),
                        "incrementStep", rule.getIncrementStep()
                ))
                .toList();
        return mapOf(
                "id", auction.getId(),
                "sellerUsername", auction.getSellerUsername(),
                "title", auction.getItem().getName(),
                "category", auction.getItem().getCategory(),
                "description", auction.getItem().getDescription(),
                "startPrice", auction.getItem().getStartingPrice(),
                "currentPrice", auction.getCurrentPrice(),
                "endTime", toIso(auction.getItem().getEndTime()),
                "imageHint", auction.getItem().getImageHint(),
                "cancelled", auction.isCancelled(),
                "paid", auction.isPaid(),
                "antiSnipeTriggered", auction.isAntiSnipeTriggered(),
                "closeNotified", auction.isCloseNotified(),
                "bidHistory", bids,
                "autoBidRules", autoBids
        );
    }

    private Map<String, Object> toNotificationPayload(NotificationRecord record) {
        return mapOf(
                "username", record.getUsername(),
                "title", record.getTitle(),
                "message", record.getMessage(),
                "time", toIso(record.getTime())
        );
    }

    private Map<String, Object> toPaymentPayload(PaymentRecord record) {
        return mapOf(
                "auctionId", record.getAuctionId(),
                "buyerUsername", record.getBuyerUsername(),
                "sellerUsername", record.getSellerUsername(),
                "amount", record.getAmount(),
                "paidAt", toIso(record.getPaidAt())
        );
    }

    private Map<String, Object> toTransactionPayload(BidTransaction record) {
        return mapOf(
                "type", record.getType(),
                "actorUsername", record.getActorUsername(),
                "referenceId", record.getReferenceId(),
                "description", record.getDescription(),
                "time", toIso(record.getTime())
        );
    }

    private Map<String, Object> toTopUpRequestPayload(TopUpRequestRecord request) {
        return mapOf(
                "id", request.getId(),
                "username", request.getUsername(),
                "amount", request.getAmount(),
                "bankName", request.getBankName(),
                "accountName", request.getAccountName(),
                "accountNumber", request.getAccountNumber(),
                "requestedAt", toIso(request.getRequestedAt()),
                "status", request.getStatus(),
                "approvedAt", toIso(request.getApprovedAt()),
                "approvedBy", request.getApprovedBy(),
                "creditedAt", toIso(request.getCreditedAt())
        );
    }

    private String toIso(LocalDateTime value) {
        return value == null ? null : value.toString();
    }

    private Map<String, Object> mapOf(Object... values) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            map.put(String.valueOf(values[index]), values[index + 1]);
        }
        return map;
    }
}
