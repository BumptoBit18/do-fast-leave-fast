package model;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

/**
 * Lớp hỗ trợ hiển thị dữ liệu Auction lên giao diện JavaFX TableView.
 * Chuyển đổi dữ liệu từ Model sang Property để UI tự động cập nhật.
 */
public class AuctionViewModel {
    private final StringProperty id;
    private final StringProperty itemName;
    private final DoubleProperty currentPrice;
    private final StringProperty timeLeft;
    private final StringProperty status;

    public AuctionViewModel(Auction auction) {
        // Lấy ID từ lớp cha Entity
        this.id = new SimpleStringProperty(auction.getId());
        
        // Lấy tên sản phẩm từ đối tượng Item trong Auction
        this.itemName = new SimpleStringProperty(auction.getItem().getName());
        
        // Lấy giá khởi điểm từ Item
        this.currentPrice = new SimpleDoubleProperty(auction.getItem().getStartingPrice());
        
        // Chuyển đổi Enum AuctionStatus sang String để hiển thị
        this.status = new SimpleStringProperty(auction.getStatus().toString());
        
        // Mặc định cho thời gian còn lại (sẽ được cập nhật sau bởi Timer)
        this.timeLeft = new SimpleStringProperty("Calculating...");
    }

    // --- Các phương thức Getter cho TableView ---

    public StringProperty idProperty() {
        return id;
    }

    public StringProperty itemNameProperty() {
        return itemName;
    }

    public DoubleProperty currentPriceProperty() {
        return currentPrice;
    }

    public StringProperty timeLeftProperty() {
        return timeLeft;
    }

    public StringProperty statusProperty() {
        return status;
    }

    // --- Các phương thức lấy giá trị thuần ---

    public String getId() { return id.get(); }
    public String getItemName() { return itemName.get(); }
    public double getCurrentPrice() { return currentPrice.get(); }
    public String getStatus() { return status.get(); }
}