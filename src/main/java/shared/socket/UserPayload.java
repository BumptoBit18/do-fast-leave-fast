package shared.socket;

import java.io.Serializable;

public record UserPayload(
        String id,
        String username,
        String password,
        String role,
        String fullName,
        double walletBalance
) implements Serializable {
    private static final long serialVersionUID = 1L;
}
