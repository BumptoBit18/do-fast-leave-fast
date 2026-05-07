package shared.socket;

import java.io.Serializable;
import java.time.LocalDateTime;

public record TopUpRequestPayload(
        String id,
        String username,
        double amount,
        String bankName,
        String accountName,
        String accountNumber,
        LocalDateTime requestedAt,
        String status,
        LocalDateTime approvedAt,
        String approvedBy,
        LocalDateTime creditedAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
