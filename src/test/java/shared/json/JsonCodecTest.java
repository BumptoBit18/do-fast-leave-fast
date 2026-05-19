package shared.json;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JsonCodecTest {
    @Test
    void shouldEncodeAndDecodeNestedPayload() {
        Map<String, Object> payload = Map.of(
                "action", "PLACE_BID",
                "amount", 1250000.0,
                "flags", List.of(true, false),
                "user", Map.of("username", "bidder", "role", "BIDDER")
        );

        String json = JsonCodec.toJson(payload);
        Object decoded = JsonCodec.fromJson(json);

        Map<?, ?> decodedMap = assertInstanceOf(Map.class, decoded);
        assertEquals("PLACE_BID", decodedMap.get("action"));
        assertEquals(1250000.0, ((Number) decodedMap.get("amount")).doubleValue());
        assertInstanceOf(List.class, decodedMap.get("flags"));
        assertInstanceOf(Map.class, decodedMap.get("user"));
    }

    @Test
    void shouldEscapeSpecialCharacters() {
        String json = JsonCodec.toJson(Map.of("message", "line1\nline2 \"quoted\""));

        assertTrue(json.contains("\\n"));
        assertTrue(json.contains("\\\"quoted\\\""));

        Map<?, ?> decoded = assertInstanceOf(Map.class, JsonCodec.fromJson(json));
        assertEquals("line1\nline2 \"quoted\"", decoded.get("message"));
    }

    @Test
    void shouldHandleNullValues() {
        // null trong Map phải encode thành JSON "null" và decode trở lại thành null Java — không được crash
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("username", "alice");
        payload.put("auctionId", null); // null value
        payload.put("amount", 0.0);

        String json = JsonCodec.toJson(payload);
        assertTrue(json.contains("null"), "null value phai duoc encode thanh 'null'");

        Map<?, ?> decoded = assertInstanceOf(Map.class, JsonCodec.fromJson(json));
        assertEquals("alice", decoded.get("username"));
        assertEquals(null, decoded.get("auctionId"));
    }

    @Test
    void shouldHandleNegativeAndZeroNumbers() {
        // số âm và số 0 phải encode/decode đúng
        Map<String, Object> payload = Map.of(
                "negative", -500000.0,
                "zero", 0
        );

        String json = JsonCodec.toJson(payload);
        Map<?, ?> decoded = assertInstanceOf(Map.class, JsonCodec.fromJson(json));

        assertEquals(-500000.0, ((Number) decoded.get("negative")).doubleValue());
        assertEquals(0, ((Number) decoded.get("zero")).intValue());
    }
}