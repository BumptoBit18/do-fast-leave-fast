package model;

import java.util.ArrayList;
import java.util.List;

public class Auction extends Entity{
    private static final long serialVersionUID = 1L;
    
    private Item item;
    private AuctionStatus status;
    private List<Bid> bidHistory;

    public Auction(String id, Item item) {
        super(id);
        this.item = item;
        this.status = AuctionStatus.OPEN; // Mặc định khi tạo là OPEN
        this.bidHistory = new ArrayList<>();
    }

    // Các hàm Getter này là bắt buộc để AuctionViewModel hết bị gạch đỏ
    public Item getItem() {
        return item;
    }

    public AuctionStatus getStatus() {
        return status;
    }

    public void setStatus(AuctionStatus status) {
        this.status = status;
    }

    public List<Bid> getBidHistory() {
        return bidHistory;
    }

    @Override
    public void printInfo() {
        System.out.println("Phiên đấu giá ID: " + id + " - Sản phẩm: " + item.getName());
    }
}
