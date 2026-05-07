package app.model;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class AuctionLot {
    private final String id;
    private final String sellerUsername;
    private final StringProperty title;
    private final StringProperty category;
    private final StringProperty description;
    private final ObjectProperty<Double> startPrice;
    private final ObjectProperty<Double> currentPrice;
    private final ObjectProperty<LocalDateTime> endTime;
    private final StringProperty imageHint;
    private final List<BidRecord> bidHistory;
    private final List<AutoBidRule> autoBidRules;
    private boolean cancelled;
    private boolean paid;
    private boolean antiSnipeTriggered;
    private boolean closeNotified;

    public AuctionLot(
            String id,
            String sellerUsername,
            String title,
            String category,
            String description,
            double startPrice,
            LocalDateTime endTime,
            String imageHint
    ) {
        this.id = id;
        this.sellerUsername = sellerUsername;
        this.title = new SimpleStringProperty(title);
        this.category = new SimpleStringProperty(category);
        this.description = new SimpleStringProperty(description);
        this.startPrice = new SimpleObjectProperty<>(startPrice);
        this.currentPrice = new SimpleObjectProperty<>(startPrice);
        this.endTime = new SimpleObjectProperty<>(endTime);
        this.imageHint = new SimpleStringProperty(imageHint);
        this.bidHistory = new ArrayList<>();
        this.autoBidRules = new ArrayList<>();
    }

    public String getId() {
        return id;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public String getTitle() {
        return title.get();
    }

    public StringProperty titleProperty() {
        return title;
    }

    public String getCategory() {
        return category.get();
    }

    public StringProperty categoryProperty() {
        return category;
    }

    public String getDescription() {
        return description.get();
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public double getStartPrice() {
        return startPrice.get();
    }

    public ObjectProperty<Double> startPriceProperty() {
        return startPrice;
    }

    public double getCurrentPrice() {
        return currentPrice.get();
    }

    public ObjectProperty<Double> currentPriceProperty() {
        return currentPrice;
    }

    public LocalDateTime getEndTime() {
        return endTime.get();
    }

    public ObjectProperty<LocalDateTime> endTimeProperty() {
        return endTime;
    }

    public String getImageHint() {
        return imageHint.get();
    }

    public StringProperty imageHintProperty() {
        return imageHint;
    }

    public List<BidRecord> getBidHistory() {
        return bidHistory;
    }

    public List<AutoBidRule> getAutoBidRules() {
        return autoBidRules;
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
        return cancelled || LocalDateTime.now().isAfter(getEndTime());
    }

    public String getStatusLabel() {
        if (cancelled) {
            return "Cancelled";
        }
        if (paid) {
            return "Paid";
        }
        if (LocalDateTime.now().isAfter(getEndTime())) {
            return "Finished";
        }
        return bidHistory.isEmpty() ? "Open" : "Live";
    }

    public String getTimeLeftLabel() {
        if (cancelled) {
            return "Da huy";
        }

        Duration duration = Duration.between(LocalDateTime.now(), getEndTime());
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

    public double getMinimumBid() {
        return Math.max(getCurrentPrice() + 100_000, getStartPrice());
    }

    public BidRecord getHighestBid() {
        return bidHistory.stream()
                .max(Comparator.comparingDouble(BidRecord::getAmount))
                .orElse(null);
    }

    public String getHighestBidder() {
        BidRecord highest = getHighestBid();
        return highest == null ? "Chua co" : highest.getBidderUsername();
    }

    public void placeBid(BidRecord bidRecord) {
        bidHistory.add(bidRecord);
        currentPrice.set(bidRecord.getAmount());
    }

    public void addAutoBidRule(AutoBidRule autoBidRule) {
        autoBidRules.removeIf(rule -> rule.getBidderUsername().equalsIgnoreCase(autoBidRule.getBidderUsername()));
        autoBidRules.add(autoBidRule);
    }

    public void cancel() {
        this.cancelled = true;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void markPaid() {
        this.paid = true;
    }

    public void setPaid(boolean paid) {
        this.paid = paid;
    }

    public void extendBySeconds(long seconds) {
        endTime.set(getEndTime().plusSeconds(seconds));
        antiSnipeTriggered = true;
    }

    public void setAntiSnipeTriggered(boolean antiSnipeTriggered) {
        this.antiSnipeTriggered = antiSnipeTriggered;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime.set(endTime);
    }

    public void markCloseNotified() {
        closeNotified = true;
    }

    public void setCloseNotified(boolean closeNotified) {
        this.closeNotified = closeNotified;
    }
}
