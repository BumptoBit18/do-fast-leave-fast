package shared.socket;

import java.io.Serializable;
import java.util.LinkedHashMap;
import java.util.Map;

public class SocketResponse implements Serializable {
    private static final long serialVersionUID = 1L;

    private final boolean success;
    private final String message;
    private final Object payload;

    private SocketResponse(boolean success, String message, Object payload) {
        this.success = success;
        this.message = message;
        this.payload = payload;
    }

    public static SocketResponse ok(Object payload) {
        return new SocketResponse(true, null, payload);
    }

    public static SocketResponse ok(String message, Object payload) {
        return new SocketResponse(true, message, payload);
    }

    public static SocketResponse error(String message) {
        return new SocketResponse(false, message, null);
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public Object getPayload() {
        return payload;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("success", success);
        values.put("message", message);
        values.put("payload", payload);
        return values;
    }

    public static SocketResponse fromMap(Map<String, Object> values) {
        Object payload = values.get("payload");
        String message = values.get("message") == null ? null : String.valueOf(values.get("message"));
        boolean success = values.get("success") instanceof Boolean bool && bool;
        return new SocketResponse(success, message, payload);
    }
}
