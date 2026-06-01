package server.controller;

import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.exception.AuthorizationException;
import server.ServerMain;
import server.model.Auction;
import server.model.AutoBid;
import server.model.BidTransaction;
import server.model.NotificationRecord;
import server.model.PaymentRecord;
import server.model.entity.User;
import server.model.item.Item;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.PriorityQueue;
import java.util.UUID;

public class AuctionController {
    private static final double MIN_BID_STEP = 100_000;
    private static final long ANTI_SNIPE_WINDOW_SECONDS = 300;
    private static final long ANTI_SNIPE_EXTENSION_SECONDS = 180;
    private static final int MAX_IMAGE_PAYLOAD_CHARS = 2_800_100;

    private final ServerMain server;

    public AuctionController(ServerMain server) {
        this.server = server;
    }

    public synchronized List<Auction> listAuctions() {
        closeExpiredAuctions();
        return server.getAuctions();
    }

    public synchronized Auction getAuctionById(String auctionId) {
        closeExpiredAuctions();
        Auction auction = server.findAuctionById(auctionId);
        if (auction == null) {
            throw new IllegalArgumentException("Khong tim thay phien dau gia " + auctionId);
        }
        return auction;
    }

    public synchronized List<Auction> searchAuctions(String keyword, String category) {
        closeExpiredAuctions();
        return server.searchAuctionsDirect(keyword, category);
    }

    public synchronized List<Auction> getAuctionsForSeller(String sellerUsername) {
        return server.getAuctionsForSellerDirect(sellerUsername);
    }

    public synchronized List<Auction> getAuctionsForBidder(String bidderUsername) {
        return server.getAuctionsForBidderDirect(bidderUsername);
    }

    public synchronized List<Auction> getWonAuctions(String bidderUsername) {
        closeExpiredAuctions();
        return server.getWonAuctionsForBidderDirect(bidderUsername);
    }

    public synchronized Auction createAuction(
            String sellerUsername,
            String title,
            String category,
            String description,
            double startPrice,
            int durationHours,
            String imageHint
    ) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Ten san pham khong duoc de trong.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Mo ta san pham khong duoc de trong.");
        }
        if (startPrice <= 0) {
            throw new IllegalArgumentException("Gia khoi diem phai lon hon 0");
        }
        if (durationHours <= 0) {
            throw new IllegalArgumentException("Thoi luong phien phai lon hon 0.");
        }
        validateImagePayload(imageHint);

        String auctionId = "AUC-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT);
        Item item = server.getItemController().createItem(
                category,
                "IT-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                title,
                description,
                startPrice,
                LocalDateTime.now().plusHours(durationHours),
                imageHint
        );
        Auction auction = new Auction(auctionId, sellerUsername, item);
        server.addAuction(auction);
        appendTransaction("CREATE_AUCTION", sellerUsername, auctionId, "Tao phien dau gia moi", startPrice);
        appendNotification("ALL", "Phien dau gia moi", title + " vua duoc tao boi " + sellerUsername + ".");
        return auction;
    }

    public synchronized Auction updateAuction(
            String auctionId,
            String actorUsername,
            String title,
            String category,
            String description,
            double startPrice,
            int durationHours,
            String imageHint
    ) {
        List<Auction> auctions = server.getAuctions();
        Auction auction = findAuctionMutable(auctions, auctionId);
        ensureSellerOwnsAuction(auction, actorUsername);
        ensureAuctionEditable(auction);

        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Ten san pham khong duoc de trong.");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Mo ta san pham khong duoc de trong.");
        }
        if (startPrice <= 0) {
            throw new IllegalArgumentException("Gia khoi diem phai lon hon 0.");
        }
        if (durationHours <= 0) {
            throw new IllegalArgumentException("Thoi luong phien phai lon hon 0.");
        }
        validateImagePayload(imageHint);

        Item updatedItem = server.getItemController().createItem(
                category,
                auction.getItem().getId(),
                title,
                description,
                startPrice,
                LocalDateTime.now().plusHours(durationHours),
                imageHint
        );
        auction.setItem(updatedItem);
        server.updateAuction(auction);
        appendTransaction("UPDATE_AUCTION", actorUsername, auctionId, "Cap nhat phien dau gia", startPrice);
        appendNotification("ALL", "Phien dau gia duoc cap nhat", title + " vua duoc cap nhat boi " + actorUsername + ".");
        return auction;
    }

    public synchronized Auction placeBid(String auctionId, String bidderUsername, double amount) {
        Auction auction = getAuctionById(auctionId);
        auction.validateBid(amount);

        List<Auction> auctions = server.getAuctions();
        Auction persistedAuction = findAuctionMutable(auctions, auctionId);
        applyBid(persistedAuction, bidderUsername, amount, false);
        resolveAutoBid(persistedAuction, bidderUsername);
        server.updateAuction(persistedAuction);
        return persistedAuction;
    }

    public synchronized Auction enableAutoBid(String auctionId, String bidderUsername, double maxAmount, double incrementStep) {
        if (maxAmount <= 0 || incrementStep < MIN_BID_STEP) {
            throw new InvalidBidException("Thong so mua tu dong khong hop le.");
        }
        List<Auction> auctions = server.getAuctions();
        Auction auction = findAuctionMutable(auctions, auctionId);
        if (auction.isClosed()) {
            throw new AuctionClosedException("Phien dau gia da ket thuc.");
        }
        if (maxAmount < auction.getMinimumBid()) {
            throw new InvalidBidException("Muc tran mua tu dong phai tu " + formatCurrency(auction.getMinimumBid()) + " tro len.");
        }
        auction.addOrReplaceAutoBid(new AutoBid(bidderUsername, maxAmount, incrementStep));
        server.replaceAuctionAutoBids(auction);
        server.updateAuction(auction);
        appendTransaction("AUTO_BID", bidderUsername, auctionId, "Bat mua tu dong", maxAmount);
        appendNotification(bidderUsername, "Mua tu dong da bat", "He thong se dau gia toi da " + formatCurrency(maxAmount) + " cho phien " + auction.getItem().getName() + ".");
        return auction;
    }

    public synchronized Auction cancelAuction(String auctionId, String actorUsername) {
        List<Auction> auctions = server.getAuctions();
        Auction auction = findAuctionMutable(auctions, auctionId);
        ensureSellerOwnsAuction(auction, actorUsername);
        if (!auction.getBidHistory().isEmpty()) {
            throw new IllegalStateException("Khong the huy phien dau gia khi da co nguoi ra gia.");
        }
        if (auction.isClosed()) {
            throw new IllegalStateException("Phien dau gia nay da dong hoac da bi huy.");
        }
        auction.cancel();
        server.updateAuction(auction);
        appendTransaction("CANCEL_AUCTION", actorUsername, auctionId, "Huy phien dau gia", 0);
        appendNotification("ALL", "Phien dau gia da bi huy", auction.getItem().getName() + " da bi huy.");
        return auction;
    }

    public synchronized void deleteAuction(String auctionId, String actorUsername) {
        List<Auction> auctions = server.getAuctions();
        Auction auction = findAuctionMutable(auctions, auctionId);
        ensureSellerOwnsAuction(auction, actorUsername);
        ensureAuctionEditable(auction);
        server.deleteAuction(auctionId);
        appendTransaction("DELETE_AUCTION", actorUsername, auctionId, "Xoa phien dau gia", 0);
        appendNotification("ALL", "Phien dau gia da bi xoa", auction.getItem().getName() + " da bi xoa boi " + actorUsername + ".");
    }

    public synchronized Auction payAuction(String auctionId, String bidderUsername) {
        closeExpiredAuctions();
        List<Auction> auctions = server.getAuctions();
        List<User> users = server.getUsers();

        Auction auction = findAuctionMutable(auctions, auctionId);
        if (!auction.isClosed() || auction.isCancelled()) {
            throw new IllegalStateException("Chi co the thanh toan khi phien dau gia da ket thuc.");
        }
        if (auction.isPaid()) {
            throw new IllegalStateException("Phien dau gia nay da duoc thanh toan.");
        }
        if (!auction.getHighestBidder().equalsIgnoreCase(bidderUsername)) {
            throw new IllegalStateException("Chi nguoi thang moi duoc thanh toan.");
        }

        User buyer = findUser(users, bidderUsername);
        User seller = findUser(users, auction.getSellerUsername());
        double amount = auction.getCurrentPrice();
        if (buyer.getWalletBalance() < amount) {
            throw new IllegalStateException("So du khong du de thanh toan.");
        }

        LocalDateTime paidAt = LocalDateTime.now();
        buyer.withdraw(amount);
        seller.deposit(amount);
        auction.markPaid();
        server.updateUserWallet(buyer);
        server.updateUserWallet(seller);
        server.updateAuction(auction);
        server.addPayment(new PaymentRecord(auctionId, buyer.getUsername(), seller.getUsername(), amount, paidAt));
        appendTransaction("PAYMENT", bidderUsername, auctionId, "Thanh toan phien dau gia", amount);
        appendNotification(bidderUsername, "Thanh toan thanh cong", "Ban da thanh toan " + auction.getItem().getName() + ".");
        appendNotification(seller.getUsername(), "Da nhan thanh toan", auction.getItem().getName() + " da duoc thanh toan.");
        return auction;
    }

    public synchronized void closeExpiredAuctions() {
        List<Auction> auctions = server.getAuctions();
        List<Auction> changedAuctions = new java.util.ArrayList<>();
        for (Auction auction : auctions) {
            if (!auction.isCloseNotified() && !auction.isCancelled() && LocalDateTime.now().isAfter(auction.getItem().getEndTime())) {
                if (!auction.getHighestBidder().equalsIgnoreCase("Chua co nguoi dau gia")) {
                    appendNotification(auction.getHighestBidder(), "Ban da thang", "Phien " + auction.getItem().getName() + " da ket thuc. Hay thanh toan tien de nhan hang.");
                    appendNotification(auction.getSellerUsername(), "Phien da ket thuc", "Lot " + auction.getItem().getName() + " da dong.");
                    appendTransaction("AUCTION_CLOSED", "SYSTEM", auction.getId(), "Dong phien dau gia", auction.getCurrentPrice());
                }
                auction.markCloseNotified();
                changedAuctions.add(auction);
            }
        }
        for (Auction auction : changedAuctions) {
            server.updateAuction(auction);
        }
    }

    private void resolveAutoBid(Auction auction, String triggerUsername) {
        boolean changed;
        int safety = 0;
        do {
            changed = false;
            PriorityQueue<AutoBid> candidates = new PriorityQueue<>(
                    java.util.Comparator.comparingDouble(AutoBid::getMaxAmount).reversed()
            );
            auction.getAutoBids().stream()
                    .filter(rule -> !rule.getBidderUsername().equalsIgnoreCase(auction.getHighestBidder()))
                    .filter(rule -> rule.getMaxAmount() >= auction.getMinimumBid())
                    .forEach(candidates::offer);
            AutoBid bestRule = candidates.poll();

            if (bestRule != null) {
                double nextAmount = Math.min(bestRule.getMaxAmount(), auction.getCurrentPrice() + bestRule.getIncrementStep());
                if (nextAmount >= auction.getMinimumBid()) {
                    applyBid(auction, bestRule.getBidderUsername(), nextAmount, true);
                    appendNotification(bestRule.getBidderUsername(), "Dau gia tu dong da hoat dong", "He thong vua dat " + formatCurrency(nextAmount) + " cho phien " + auction.getItem().getName() + ".");
                    changed = true;
                }
            }
            safety++;
        } while (changed && safety < 20);

        if (triggerUsername != null) {
            appendNotification(triggerUsername, "Dat gia da duoc ghi nhan", "Yeu cau dau gia cua ban cho phien " + auction.getItem().getName() + " da duoc xu ly.");
        }
    }

    private void applyBid(Auction auction, String bidderUsername, double amount, boolean autoBid) {
        String previousLeader = auction.getHighestBidder();
        BidTransaction bid = new BidTransaction(
                autoBid ? "AUTO_BID_EXECUTED" : "BID",
                bidderUsername,
                auction.getId(),
                autoBid ? "Auto bid" : "Manual bid",
                amount,
                LocalDateTime.now()
        );
        auction.addBid(bid);
        server.insertAuctionBid(auction.getId(), bid);
        maybeExtendAuction(auction);
        appendTransaction(autoBid ? "AUTO_BID_EXECUTED" : "BID", bidderUsername, auction.getId(), autoBid ? "Auto bid thuc thi" : "Manual bid", amount);
        appendNotification(auction.getSellerUsername(), "Co bid moi", bidderUsername + " vua dat " + formatCurrency(amount) + " cho phien " + auction.getItem().getName() + ".");
        if (!Objects.equals(previousLeader, "Chua co nguoi dau gia") && !previousLeader.equalsIgnoreCase(bidderUsername)) {
            appendNotification(previousLeader, "Ban vua bi vuot gia", "Phien " + auction.getItem().getName() + " vua co muc gia moi " + formatCurrency(amount) + ".");
        }
    }

    private void maybeExtendAuction(Auction auction) {
        long secondsLeft = ChronoUnit.SECONDS.between(LocalDateTime.now(), auction.getItem().getEndTime());
        if (secondsLeft > 0 && secondsLeft <= ANTI_SNIPE_WINDOW_SECONDS) {
            auction.extendAuctionSeconds(ANTI_SNIPE_EXTENSION_SECONDS);
            appendTransaction("ANTI_SNIPE", "SYSTEM", auction.getId(), "Gia han phien them " + ANTI_SNIPE_EXTENSION_SECONDS + " giay", 0);
            appendNotification("ALL", "Anti-snipe kich hoat", "Phien " + auction.getItem().getName() + " da duoc gia han de tranh bid sat gio.");
        }
    }

    private Auction findAuctionMutable(List<Auction> auctions, String auctionId) {
        return auctions.stream()
                .filter(candidate -> candidate.getId().equalsIgnoreCase(auctionId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay phien " + auctionId));
    }

    private void ensureSellerOwnsAuction(Auction auction, String actorUsername) {
        if (!auction.getSellerUsername().equalsIgnoreCase(actorUsername)) {
            throw new AuthorizationException("Chi nguoi dang ban moi co quyen sua phien dau gia nay.");
        }
    }

    private void ensureAuctionEditable(Auction auction) {
        if (auction.isClosed()) {
            throw new AuctionClosedException("Phien dau gia da ket thuc.");
        }
        if (!auction.getBidHistory().isEmpty()) {
            throw new InvalidBidException("Khong the sua hoac xoa phien khi da co bid.");
        }
        if (auction.isPaid()) {
            throw new IllegalStateException("Khong the sua phien da thanh toan.");
        }
    }

    private void validateImagePayload(String imagePayload) {
        if (imagePayload == null || imagePayload.isBlank()) {
            return;
        }
        if (imagePayload.length() > MAX_IMAGE_PAYLOAD_CHARS) {
            throw new IllegalArgumentException("Anh san pham toi da 2 MB.");
        }
        if (!imagePayload.startsWith("data:image/") || !imagePayload.contains(";base64,")) {
            throw new IllegalArgumentException("Anh san pham khong dung dinh dang.");
        }
    }

    private User findUser(List<User> users, String username) {
        return users.stream()
                .filter(user -> user.getUsername().equalsIgnoreCase(username))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay nguoi dung " + username));
    }

    private void appendTransaction(String type, String actor, String ref, String description, double amount) {
        server.addTransaction(new BidTransaction(type, actor, ref, description, amount, LocalDateTime.now()));
    }

    private void appendNotification(String username, String title, String message) {
        server.addNotification(new NotificationRecord(username, title, message, LocalDateTime.now()));
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.US, "%,.0f VND", amount);
    }
}
