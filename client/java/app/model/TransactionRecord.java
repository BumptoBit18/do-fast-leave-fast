package app.model;

import java.time.LocalDateTime;

public class TransactionRecord {
    private final String type;
    private final String actorUsername;
    private final String referenceId;
    private final String description;
    private final LocalDateTime time;

    public TransactionRecord(String type, String actorUsername, String referenceId, String description, LocalDateTime time) {
        this.type = type;
        this.actorUsername = actorUsername;
        this.referenceId = referenceId;
        this.description = description;
        this.time = time;
    }

    public String getType() {
        return type;
    }

    public String getActorUsername() {
        return actorUsername;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
