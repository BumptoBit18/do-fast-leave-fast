package server.model;

import java.io.Serializable;

public class AutoBid implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String bidderUsername;
    private final double maxAmount;
    private final double incrementStep;

    public AutoBid(String bidderUsername, double maxAmount, double incrementStep) {
        this.bidderUsername = bidderUsername;
        this.maxAmount = maxAmount;
        this.incrementStep = incrementStep;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public double getMaxAmount() {
        return maxAmount;
    }

    public double getIncrementStep() {
        return incrementStep;
    }
}
