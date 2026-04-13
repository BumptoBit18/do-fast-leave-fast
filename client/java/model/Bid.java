package model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Bid implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private String bidderUsername; // Người đặt giá
    private double amount;         // Giá đặt
    private LocalDateTime time;    // Thời gian đặt

    public Bid(String bidderUsername, double amount) {
        this.bidderUsername = bidderUsername;
        this.amount = amount;
        this.time = LocalDateTime.now();
    }

    // Getters
    public String getBidderUsername() { return bidderUsername; }
    public double getAmount() { return amount; }
    public LocalDateTime getTime() { return time; }

    @Override
    public String toString() {
        return bidderUsername + " đặt " + amount + " lúc " + time;
    }
}
