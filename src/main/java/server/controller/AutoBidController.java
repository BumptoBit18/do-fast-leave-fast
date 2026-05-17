package server.controller;

import server.ServerMain;
import server.model.Auction;

// AutoBidController hiện tại chỉ là lớp bọc (wrapper) đơn giản gọi lại AuctionController.
// để MessageRouter có điểm gọi riêng cho auto-bid,
// tách biệt với các hành động đấu giá thường.
// KHÔNG cần "synchronized" ở đây vì AuctionController.enableAutoBid
// đã có synchronized rồi — dùng thêm chỉ gây overhead thừa.
public class AutoBidController {
    private final ServerMain server;

    public AutoBidController(ServerMain server) {
        this.server = server;
    }

    public Auction enableAutoBid(String auctionId, String bidderUsername, double maxAmount, double incrementStep) {
        return server.getAuctionController().enableAutoBid(auctionId, bidderUsername, maxAmount, incrementStep);
    }
}
