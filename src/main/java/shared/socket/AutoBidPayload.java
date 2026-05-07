package shared.socket;

import java.io.Serializable;

public record AutoBidPayload(
        String bidderUsername,
        double maxAmount,
        double incrementStep
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
