package server.network;

import org.junit.jupiter.api.Test;
import server.exception.AuthenticationException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionRegistryTest {
    @Test
    void shouldResolveUsernameForActiveToken() {
        String token = SessionRegistry.create("seller");

        assertEquals("seller", SessionRegistry.requireUsername(token));

        SessionRegistry.invalidate(token);
        assertThrows(AuthenticationException.class, () -> SessionRegistry.requireUsername(token));
    }

    @Test
    void shouldInvalidateAllTokensForDeletedUser() {
        String token = SessionRegistry.create("bidder");

        SessionRegistry.invalidateUser("BIDDER");

        assertThrows(AuthenticationException.class, () -> SessionRegistry.requireUsername(token));
    }
}
