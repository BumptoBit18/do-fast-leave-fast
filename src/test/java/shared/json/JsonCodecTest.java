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
    }
