package app.model;

import java.time.LocalDateTime;

public class NotificationItem {
    private final String username;
    private final String title;
    private final String message;
    private final LocalDateTime time;

    public NotificationItem(String username, String title, String message, LocalDateTime time) {
        this.username = username;
        this.title = title;
        this.message = message;
        this.time = time;
    }

    public String getUsername() {
        return username;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
