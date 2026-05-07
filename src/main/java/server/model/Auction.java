package server.model;

import server.model.item.Item;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String sellerUsername;
    private final Item item;
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final List<AutoBid> autoBids = new ArrayList<>();
    private boolean cancelled;
    private boolean paid;
    private boolean antiSnipeTriggered;
    private boolean closeNotified;

    public Auction(String id, String sellerUsername, Item item) {
        this.id = id;
        this.sellerUsername = sellerUsername;
        this.item = item;
    }

    public String getId() {
        return id;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public Item getItem() {
        return item;
    }

    public List<BidTransaction> getBidHistory() {
        return bidHistory;
    }

    public List<AutoBid> getAutoBids() {
        return autoBids;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isPaid() {
        return paid;
    }

    public boolean isAntiSnipeTriggered() {
        return antiSnipeTriggered;
    }

    public boolean isCloseNotified() {
        return closeNotified;
    }

    public boolean isClosed() {
        return cancelled || LocalDateTime.now().isAfter(item.getEndTime());
    }

    public double getCurrentPrice() {
        BidTransaction highest = getHighestBid();
        return highest == null ? item.getStartingPrice() : highest.getAmount();
    }

    public double getMinimumBid() {
        return getCurrentPrice() + 100_000;
    }

    public BidTransaction getHighestBid() {
        return bidHistory.stream()
                .max(Comparator.comparingDouble(BidTransaction::getAmount))
                .orElse(null);
    }

    public String getHighestBidder() {
        BidTransaction highest = getHighestBid();
        return highest == null ? "Chưa có người đấu giá" : highest.getActorUsername();
    }

    public String getStatusLabel() {
        if (cancelled) {
            return "Cancelled";
        }
        if (paid) {
            return "Paid";
        }
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            return "Finished";
        }
        return bidHistory.isEmpty() ? "Open" : "Live";
    }

    public String getTimeLeftLabel() {
        if (cancelled) {
            return "Đã hủy";
        }
        Duration duration = Duration.between(LocalDateTime.now(), item.getEndTime());
        if (duration.isNegative() || duration.isZero()) {
            return "Đã kết thúc";
        }
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) {
            return days + " ngày " + hours + " giờ";
        }
        return hours + " giờ " + minutes + " phút";
    }

    public void addBid(BidTransaction bid) {
        bidHistory.add(bid);
    }

    public void addOrReplaceAutoBid(AutoBid autoBid) {
        autoBids.removeIf(rule -> rule.getBidderUsername().equalsIgnoreCase(autoBid.getBidderUsername()));
        autoBids.add(autoBid);
    }

    public void cancel() {
        cancelled = true;
    }

    public void markPaid() {
        paid = true;
    }

    public void extendAuctionSeconds(long seconds) {
        item.setEndTime(item.getEndTime().plusSeconds(seconds));
        antiSnipeTriggered = true;
    }

    public void markCloseNotified() {
        closeNotified = true;
    }
}
