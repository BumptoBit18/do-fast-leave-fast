package server.network;

import app.model.UserRole;
import server.ServerMain;
import server.model.Auction;
import server.model.AutoBid;
import server.model.BidTransaction;
import server.model.NotificationRecord;
import server.model.PaymentRecord;
import server.model.TopUpRequestRecord;
import server.model.entity.User;
import shared.socket.*;

import java.util.List;

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
                case "GET_USERS" -> SocketResponse.ok(server.getUsersDirect().stream().map(this::toUserPayload).toList());
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
                    server.getUserController().approveTopUpRequest(request.getRequestId(), request.getActorUsername());
                    yield SocketResponse.ok(toUserPayload(findUser(request.getActorUsername())));
                }
                case "GET_TOP_UP_REQUESTS" -> SocketResponse.ok(server.getTopUpRequestsDirect().stream().map(this::toTopUpRequestPayload).toList());
                case "PAY_AUCTION" -> SocketResponse.ok(toAuctionPayload(server.getAuctionController().payAuction(
                        request.getAuctionId(),
                        request.getActorUsername()
                )));
                case "CANCEL_AUCTION" -> SocketResponse.ok(toAuctionPayload(server.getAuctionController().cancelAuction(
                        request.getAuctionId(),
                        request.getActorUsername()
                )));
                case "GET_NOTIFICATIONS" -> SocketResponse.ok(server.getNotificationsDirect().stream().map(this::toNotificationPayload).toList());
                case "GET_NOTIFICATIONS_FOR_USER" -> SocketResponse.ok(server.getNotificationsForUser(request.getActorUsername()).stream()
                        .map(this::toNotificationPayload)
                        .toList());
                case "GET_PAYMENTS" -> SocketResponse.ok(server.getPaymentsDirect().stream().map(this::toPaymentPayload).toList());
                case "GET_TRANSACTIONS" -> SocketResponse.ok(server.getTransactionsDirect().stream().map(this::toTransactionPayload).toList());
                default -> SocketResponse.error("Action khong duoc ho tro: " + request.getAction());
            };
        } catch (Exception ex) {
            return SocketResponse.error(ex.getMessage() == null ? "Loi server." : ex.getMessage());
        }
    }

    private void syncForAction(String action) {
        switch (action) {
            case "LOGIN", "REGISTER" -> server.reloadUsers();
            case "GET_AUCTIONS", "GET_AUCTION_BY_ID", "SEARCH_AUCTIONS", "GET_AUCTIONS_FOR_SELLER",
                    "GET_AUCTIONS_FOR_BIDDER", "GET_WON_AUCTIONS" -> server.reloadAuctions();
            case "CREATE_AUCTION", "PLACE_BID", "ENABLE_AUTO_BID", "PAY_AUCTION", "CANCEL_AUCTION" -> {
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

    private User findUser(String username) {
        User user = server.findUserByUsername(username);
        if (user == null) {
            throw new IllegalArgumentException("Khong tim thay nguoi dung " + username);
        }
        return user;
    }

    private UserPayload toUserPayload(User user) {
        return new UserPayload(
                user.getId(),
                user.getUsername(),
                user.getPassword(),
                UserRole.valueOf(user.getRole()).name(),
                user.getFullName(),
                user.getWalletBalance()
        );
    }

    private AuctionPayload toAuctionPayload(Auction auction) {
        List<BidPayload> bids = auction.getBidHistory().stream()
                .map(bid -> new BidPayload(bid.getActorUsername(), bid.getAmount(), bid.getTime()))
                .toList();
        List<AutoBidPayload> autoBids = auction.getAutoBids().stream()
                .map(rule -> new AutoBidPayload(rule.getBidderUsername(), rule.getMaxAmount(), rule.getIncrementStep()))
                .toList();
        return new AuctionPayload(
                auction.getId(),
                auction.getSellerUsername(),
                auction.getItem().getName(),
                auction.getItem().getCategory(),
                auction.getItem().getDescription(),
                auction.getItem().getStartingPrice(),
                auction.getCurrentPrice(),
                auction.getItem().getEndTime(),
                auction.getItem().getImageHint(),
                auction.isCancelled(),
                auction.isPaid(),
                auction.isAntiSnipeTriggered(),
                auction.isCloseNotified(),
                bids,
                autoBids
        );
    }

    private NotificationPayload toNotificationPayload(NotificationRecord record) {
        return new NotificationPayload(record.getUsername(), record.getTitle(), record.getMessage(), record.getTime());
    }

    private PaymentPayload toPaymentPayload(PaymentRecord record) {
        return new PaymentPayload(record.getAuctionId(), record.getBuyerUsername(), record.getSellerUsername(), record.getAmount(), record.getPaidAt());
    }

    private TransactionPayload toTransactionPayload(BidTransaction record) {
        return new TransactionPayload(record.getType(), record.getActorUsername(), record.getReferenceId(), record.getDescription(), record.getTime());
    }

    private TopUpRequestPayload toTopUpRequestPayload(TopUpRequestRecord request) {
        return new TopUpRequestPayload(
                request.getId(),
                request.getUsername(),
                request.getAmount(),
                request.getBankName(),
                request.getAccountName(),
                request.getAccountNumber(),
                request.getRequestedAt(),
                request.getStatus(),
                request.getApprovedAt(),
                request.getApprovedBy(),
                request.getCreditedAt()
        );
    }
}
