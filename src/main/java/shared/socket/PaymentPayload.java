package shared.socket;

import java.io.Serializable;
import java.time.LocalDateTime;

public record PaymentPayload(
        String auctionId,
        String buyerUsername,
        String sellerUsername,
        double amount,
        LocalDateTime paidAt
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
