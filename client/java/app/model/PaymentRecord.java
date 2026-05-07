package app.model;

import java.time.LocalDateTime;

public class PaymentRecord {
    private final String auctionId;
    private final String buyerUsername;
    private final String sellerUsername;
    private final double amount;
    private final LocalDateTime paidAt;

    public PaymentRecord(String auctionId, String buyerUsername, String sellerUsername, double amount, LocalDateTime paidAt) {
        this.auctionId = auctionId;
        this.buyerUsername = buyerUsername;
        this.sellerUsername = sellerUsername;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public String getAuctionId() {
        return auctionId;
    }

    public String getBuyerUsername() {
        return buyerUsername;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getPaidAt() {
        return paidAt;
    }
}
