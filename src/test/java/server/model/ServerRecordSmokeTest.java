package server.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ServerRecordSmokeTest {
    @Test
    void shouldExposeNotificationAndPaymentFields() {
        LocalDateTime time = LocalDateTime.of(2026, 6, 4, 14, 30);
        NotificationRecord notification = new NotificationRecord("seller", "Co bid moi", "Bidder vua dat gia", time);
        PaymentRecord payment = new PaymentRecord("AUC-10", "bidder", "seller", 8_000_000, time);

        assertEquals("seller", notification.getUsername());
        assertEquals("Co bid moi", notification.getTitle());
        assertEquals("Bidder vua dat gia", notification.getMessage());
        assertEquals(time, notification.getTime());

        assertEquals("AUC-10", payment.getAuctionId());
        assertEquals("bidder", payment.getBuyerUsername());
        assertEquals("seller", payment.getSellerUsername());
        assertEquals(8_000_000, payment.getAmount());
        assertEquals(time, payment.getPaidAt());
    }
}
