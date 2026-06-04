package app.model;

import app.service.DashboardStats;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppModelRecordsTest {
    @Test
    void shouldExposeAppUserFieldsAndWalletMutations() {
        AppUser user = new AppUser("U-1", "bidder", "secret", UserRole.BIDDER, "Bidder One", 1_000_000);

        user.setPassword("new-secret");
        user.deposit(250_000);
        user.withdraw(100_000);

        assertEquals("U-1", user.getId());
        assertEquals("bidder", user.getUsername());
        assertEquals("new-secret", user.getPassword());
        assertEquals(UserRole.BIDDER, user.getRole());
        assertEquals("Bidder One", user.getFullName());
        assertEquals(1_150_000, user.getWalletBalance());
    }

    @Test
    void shouldExposeClientRecordsAndDashboardStats() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 4, 14, 0);
        BidRecord bid = new BidRecord("bidder", 2_000_000, time);
        AutoBidRule autoBid = new AutoBidRule("bidder", 3_000_000, 200_000);
        NotificationItem notification = new NotificationItem("bidder", "Co bid moi", "Ban vua bi vuot gia", time);
        PaymentRecord payment = new PaymentRecord("AUC-1", "buyer", "seller", 5_000_000, time);
        TopUpRequestRecord topUp = new TopUpRequestRecord(
                "TOPUP-1", "bidder", 2_000_000, "VCB", "Nguyen Van A", "123456", time, "PENDING", null, null, null
        );
        TransactionRecord transaction = new TransactionRecord("BID", "bidder", "AUC-1", "Manual bid", time);
        DashboardStats stats = new DashboardStats(5, 3, 2, 4, 100_000_000, 2, 6, 8);

        assertEquals("bidder", bid.getBidderUsername());
        assertEquals(2_000_000, bid.getAmount());
        assertEquals(time, bid.getTime());

        assertEquals("bidder", autoBid.getBidderUsername());
        assertEquals(3_000_000, autoBid.getMaxAmount());
        assertEquals(200_000, autoBid.getIncrementStep());

        assertEquals("bidder", notification.getUsername());
        assertEquals("Co bid moi", notification.getTitle());
        assertEquals("Ban vua bi vuot gia", notification.getMessage());
        assertEquals(time, notification.getTime());

        assertEquals("AUC-1", payment.getAuctionId());
        assertEquals("buyer", payment.getBuyerUsername());
        assertEquals("seller", payment.getSellerUsername());
        assertEquals(5_000_000, payment.getAmount());
        assertEquals(time, payment.getPaidAt());

        assertEquals("TOPUP-1", topUp.getId());
        assertEquals("bidder", topUp.getUsername());
        assertEquals(2_000_000, topUp.getAmount());
        assertEquals("VCB", topUp.getBankName());
        assertEquals("Nguyen Van A", topUp.getAccountName());
        assertEquals("123456", topUp.getAccountNumber());
        assertEquals(time, topUp.getRequestedAt());
        assertEquals("PENDING", topUp.getStatus());

        assertEquals("BID", transaction.getType());
        assertEquals("bidder", transaction.getActorUsername());
        assertEquals("AUC-1", transaction.getReferenceId());
        assertEquals("Manual bid", transaction.getDescription());
        assertEquals(time, transaction.getTime());

        assertEquals(5, stats.openAuctions());
        assertEquals(3, stats.finishedAuctions());
        assertEquals(2, stats.sellers());
        assertEquals(4, stats.bidders());
        assertEquals(100_000_000, stats.totalVolume());
        assertEquals(2, stats.paidAuctions());
        assertEquals(6, stats.autoBidRules());
        assertEquals(8, stats.notificationCount());
    }
}
