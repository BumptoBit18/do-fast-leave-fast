package model;

import javafx.beans.property.*;

/**
 * Model chứa dữ liệu đấu giá dùng cho việc hiển thị (View)[cite: 26, 91].
 * Sử dụng JavaFX Properties để tự động cập nhật UI khi dữ liệu thay đổi[cite: 23].
 */
public class AuctionViewModel {
    private final StringProperty itemId;
    private final StringProperty itemName;
    private final DoubleProperty currentPrice;
    private final StringProperty timeLeft;
    private final StringProperty status; // OPEN, RUNNING, FINISHED [cite: 74, 76]

    public AuctionViewModel(String itemId, String itemName, double currentPrice, String timeLeft, String status) {
        this.itemId = new SimpleStringProperty(itemId);
        this.itemName = new SimpleStringProperty(itemName);
        this.currentPrice = new SimpleDoubleProperty(currentPrice);
        this.timeLeft = new SimpleStringProperty(timeLeft);
        this.status = new SimpleStringProperty(status);
    }

    // Getters cho Properties (Cần thiết cho TableView/UI Binding)
    public StringProperty itemIdProperty() { 
        return itemId; }
    public StringProperty itemNameProperty() { return itemName; }
    public DoubleProperty currentPriceProperty() { return currentPrice; }
    public StringProperty timeLeftProperty() { return timeLeft; }
    public StringProperty statusProperty() { return status; }

    // Setters dùng để cập nhật khi nhận được dữ liệu từ Socket [cite: 129]
    public void setCurrentPrice(double price) { this.currentPrice.set(price); }
    public void setStatus(String status) { this.status.set(status); }
    public void setTimeLeft(String time) { this.timeLeft.set(time); }
}