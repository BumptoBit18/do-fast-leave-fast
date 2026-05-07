package server.controller;

import server.ServerMain;
import server.model.Auction;

public class AutoBidController {
    private final ServerMain server;

    public AutoBidController(ServerMain server) {
        this.server = server;
    }

    public synchronized Auction enableAutoBid(String auctionId, String bidderUsername, double maxAmount, double incrementStep) {
        return server.getAuctionController().enableAutoBid(auctionId, bidderUsername, maxAmount, incrementStep);
    }
}
