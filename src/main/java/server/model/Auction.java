package server.model;

import server.exception.AuctionClosedException;
import server.exception.InvalidBidException;
import server.model.item.Item;

import java.io.Serializable;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

public class Auction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;
    private final String sellerUsername;
    private Item item;
    private final List<BidTransaction> bidHistory = new ArrayList<>();
    private final List<AutoBid> autoBids = new ArrayList<>();
    private final ReentrantLock bidLock = new ReentrantLock();
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

    public void setItem(Item item) {
        this.item = item;
    }

    public List<BidTransaction> getBidHistory() {
        bidLock.lock();
        try {
            return List.copyOf(bidHistory);
        } finally {
            bidLock.unlock();
        }
    }

    public List<AutoBid> getAutoBids() {
        bidLock.lock();
        try {
            return List.copyOf(autoBids);
        } finally {
            bidLock.unlock();
        }
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

    public void validateBid(double amount) {
        if (isClosed()) {
            throw new AuctionClosedException("Phien dau gia da ket thuc.");
        }
        if (amount < getMinimumBid()) {
            throw new InvalidBidException("Gia dat phai cao hon gia hien tai.");
        }
    }

    public double getCurrentPrice() {
        bidLock.lock();
        try {
            BidTransaction highest = getHighestBid();
            return highest == null ? item.getStartingPrice() : highest.getAmount();
        } finally {
            bidLock.unlock();
        }
    }

    public double getMinimumBid() {
        bidLock.lock();
        try {
            return getCurrentPrice() + 100_000;
        } finally {
            bidLock.unlock();
        }
    }

    public BidTransaction getHighestBid() {
        bidLock.lock();
        try {
            return bidHistory.stream()
                    .max(Comparator.comparingDouble(BidTransaction::getAmount))
                    .orElse(null);
        } finally {
            bidLock.unlock();
        }
    }

    public String getHighestBidder() {
        bidLock.lock();
        try {
            BidTransaction highest = getHighestBid();
            return highest == null ? "Chua co nguoi dau gia" : highest.getActorUsername();
        } finally {
            bidLock.unlock();
        }
    }

    public String getStatusLabel() {
        return switch (getStatus()) {
            case OPEN -> "Open";
            case RUNNING -> "Live";
            case FINISHED -> "Finished";
            case PAID -> "Paid";
            case CANCELLED -> "Cancelled";
        };
    }

    public AuctionStatus getStatus() {
        if (cancelled) {
            return AuctionStatus.CANCELLED;
        }
        if (paid) {
            return AuctionStatus.PAID;
        }
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            return AuctionStatus.FINISHED;
        }
        return bidHistory.isEmpty() ? AuctionStatus.OPEN : AuctionStatus.RUNNING;
    }

    public String getTimeLeftLabel() {
        if (cancelled) {
            return "Da huy";
        }
        Duration duration = Duration.between(LocalDateTime.now(), item.getEndTime());
        if (duration.isNegative() || duration.isZero()) {
            return "Da ket thuc";
        }
        long days = duration.toDays();
        long hours = duration.minusDays(days).toHours();
        long minutes = duration.minusDays(days).minusHours(hours).toMinutes();
        if (days > 0) {
            return days + " ngay " + hours + " gio";
        }
        return hours + " gio " + minutes + " phut";
    }

    public void addBid(BidTransaction bid) {
        bidLock.lock();
        try {
            bidHistory.add(bid);
        } finally {
            bidLock.unlock();
        }
    }

    public void addOrReplaceAutoBid(AutoBid autoBid) {
        bidLock.lock();
        try {
            autoBids.removeIf(rule -> rule.getBidderUsername().equalsIgnoreCase(autoBid.getBidderUsername()));
            autoBids.add(autoBid);
        } finally {
            bidLock.unlock();
        }
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
