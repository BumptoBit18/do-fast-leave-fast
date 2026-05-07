package app.model;

import java.time.LocalDateTime;

public class BidRecord {
    private final String bidderUsername;
    private final double amount;
    private final LocalDateTime time;

    public BidRecord(String bidderUsername, double amount, LocalDateTime time) {
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.time = time;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
