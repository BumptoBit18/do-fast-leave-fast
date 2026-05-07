package app.model;

public class AutoBidRule {
    private final String bidderUsername;
    private final double maxAmount;
    private final double incrementStep;

    public AutoBidRule(String bidderUsername, double maxAmount, double incrementStep) {
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
