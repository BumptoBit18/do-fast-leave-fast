package shared.socket;

import java.io.Serializable;
import java.time.LocalDateTime;

public record BidPayload(
        String bidderUsername,
        double amount,
        LocalDateTime time
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
