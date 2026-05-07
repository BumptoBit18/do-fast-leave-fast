package shared.socket;

import java.io.Serializable;
import java.time.LocalDateTime;

public record TransactionPayload(
        String type,
        String actorUsername,
        String referenceId,
        String description,
        LocalDateTime time
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
