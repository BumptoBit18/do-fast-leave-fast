package shared.socket;

import java.io.Serializable;
import java.time.LocalDateTime;

public record NotificationPayload(
        String username,
        String title,
        String message,
        LocalDateTime time
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
