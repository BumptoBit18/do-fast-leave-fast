package server.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class TopUpRequestRecordTest {
    @Test
    void shouldApproveRequestWithAdminAndTimestamp() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 4, 13, 0);
        LocalDateTime approvedAt = requestedAt.plusMinutes(5);
        TopUpRequestRecord request = new TopUpRequestRecord(
                "TOPUP-1",
                "bidder",
                2_000_000,
                "VCB",
                "Nguyen Van A",
                "123456",
                requestedAt,
                "PENDING",
                null,
                null,
                null
        );

        request.approve("admin", approvedAt);

        assertEquals("APPROVED", request.getStatus());
        assertEquals("admin", request.getApprovedBy());
        assertEquals(approvedAt, request.getApprovedAt());
        assertNull(request.getCreditedAt());
    }

    @Test
    void shouldMarkApprovedRequestAsCredited() {
        LocalDateTime requestedAt = LocalDateTime.of(2026, 6, 4, 13, 0);
        LocalDateTime approvedAt = requestedAt.plusMinutes(5);
        LocalDateTime creditedAt = approvedAt.plusSeconds(10);
        TopUpRequestRecord request = new TopUpRequestRecord(
                "TOPUP-2",
                "seller",
                5_000_000,
                "ACB",
                "Tran Thi B",
                "654321",
                requestedAt,
                "PENDING",
                null,
                null,
                null
        );

        request.approve("admin", approvedAt);
        request.markCredited(creditedAt);

        assertEquals("CREDITED", request.getStatus());
        assertEquals("admin", request.getApprovedBy());
        assertEquals(approvedAt, request.getApprovedAt());
        assertEquals(creditedAt, request.getCreditedAt());
    }
}
