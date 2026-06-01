package server.controller;

import org.junit.jupiter.api.Test;
import server.model.entity.Bidder;
import server.model.entity.Seller;
import server.model.entity.User;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserControllerTest {
    @Test
    void shouldUpdateWalletBalanceForUserEntities() {
        User bidder = new Bidder("U-1", "bidder", "bidder123", "Bidder", 1_000_000);
        User seller = new Seller("U-2", "seller", "seller123", "Seller", 500_000);

        bidder.deposit(250_000);
        seller.withdraw(100_000);

        assertEquals(1_250_000, bidder.getWalletBalance());
        assertEquals(400_000, seller.getWalletBalance());
    }
}
