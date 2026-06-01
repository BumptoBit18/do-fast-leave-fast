package server.network;

import server.exception.AuthenticationException;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class SessionRegistry {
    private static final Map<String, String> USERNAMES_BY_TOKEN = new ConcurrentHashMap<>();

    private SessionRegistry() {
    }

    public static String create(String username) {
        String token = UUID.randomUUID().toString();
        USERNAMES_BY_TOKEN.put(token, username);
        return token;
    }

    public static String requireUsername(String token) {
        if (token == null || token.isBlank()) {
            throw new AuthenticationException("Phien dang nhap khong hop le. Hay dang nhap lai.");
        }
        String username = USERNAMES_BY_TOKEN.get(token);
        if (username == null) {
            throw new AuthenticationException("Phien dang nhap da het han. Hay dang nhap lai.");
        }
        return username;
    }

    public static void invalidate(String token) {
        if (token != null) {
            USERNAMES_BY_TOKEN.remove(token);
        }
    }

    public static void invalidateUser(String username) {
        USERNAMES_BY_TOKEN.entrySet().removeIf(entry -> entry.getValue().equalsIgnoreCase(username));
    }
}
