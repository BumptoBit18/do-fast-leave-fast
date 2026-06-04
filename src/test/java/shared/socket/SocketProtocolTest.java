package shared.socket;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SocketProtocolTest {
    @Test
    void shouldRoundTripSocketRequestThroughMap() {
        SocketRequest request = new SocketRequest();
        request.setAction("CREATE_AUCTION");
        request.setSessionToken("token-1");
        request.setActorUsername("seller");
        request.setUsername("seller");
        request.setPassword("secret");
        request.setRole("SELLER");
        request.setFullName("Seller One");
        request.setKeyword("laptop");
        request.setCategory("Electronics");
        request.setAuctionId("AUC-9");
        request.setTitle("MacBook");
        request.setDescription("Like new");
        request.setImageHint("data:image/png;base64,abc");
        request.setBankName("VCB");
        request.setAccountName("Seller One");
        request.setAccountNumber("123456");
        request.setRequestId("TOPUP-1");
        request.setAmount(2_500_000);
        request.setMaxAmount(3_000_000);
        request.setIncrementStep(200_000);
        request.setStartPrice(1_500_000);
        request.setDurationHours(24);

        SocketRequest restored = SocketRequest.fromMap(request.toMap());

        assertEquals("CREATE_AUCTION", restored.getAction());
        assertEquals("token-1", restored.getSessionToken());
        assertEquals("seller", restored.getActorUsername());
        assertEquals("seller", restored.getUsername());
        assertEquals("secret", restored.getPassword());
        assertEquals("SELLER", restored.getRole());
        assertEquals("Seller One", restored.getFullName());
        assertEquals("laptop", restored.getKeyword());
        assertEquals("Electronics", restored.getCategory());
        assertEquals("AUC-9", restored.getAuctionId());
        assertEquals("MacBook", restored.getTitle());
        assertEquals("Like new", restored.getDescription());
        assertEquals("data:image/png;base64,abc", restored.getImageHint());
        assertEquals("VCB", restored.getBankName());
        assertEquals("Seller One", restored.getAccountName());
        assertEquals("123456", restored.getAccountNumber());
        assertEquals("TOPUP-1", restored.getRequestId());
        assertEquals(2_500_000, restored.getAmount());
        assertEquals(3_000_000, restored.getMaxAmount());
        assertEquals(200_000, restored.getIncrementStep());
        assertEquals(1_500_000, restored.getStartPrice());
        assertEquals(24, restored.getDurationHours());
    }

    @Test
    void shouldDefaultNumericValuesWhenSocketRequestMapIsMissingNumbers() {
        SocketRequest restored = SocketRequest.fromMap(Map.of(
                "action", "PLACE_BID",
                "amount", "not-a-number"
        ));

        assertEquals("PLACE_BID", restored.getAction());
        assertEquals(0, restored.getAmount());
        assertEquals(0, restored.getMaxAmount());
        assertEquals(0, restored.getIncrementStep());
        assertEquals(0, restored.getStartPrice());
        assertEquals(0, restored.getDurationHours());
    }

    @Test
    void shouldExposeSuccessAndErrorSocketResponses() {
        SocketResponse ok = SocketResponse.ok("Saved", Map.of("id", "AUC-1"));
        SocketResponse restoredOk = SocketResponse.fromMap(ok.toMap());
        SocketResponse error = SocketResponse.error("Forbidden");
        SocketResponse restoredError = SocketResponse.fromMap(error.toMap());

        assertTrue(restoredOk.isSuccess());
        assertEquals("Saved", restoredOk.getMessage());
        assertEquals(Map.of("id", "AUC-1"), restoredOk.getPayload());

        assertFalse(restoredError.isSuccess());
        assertEquals("Forbidden", restoredError.getMessage());
        assertNull(restoredError.getPayload());
    }
}
