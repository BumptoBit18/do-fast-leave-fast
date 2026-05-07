package server.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class BidTransaction implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String type;
    private final String actorUsername;
    private final String referenceId;
    private final String description;
    private final double amount;
    private final LocalDateTime time;

    public BidTransaction(String type, String actorUsername, String referenceId, String description, double amount, LocalDateTime time) {
        this.type = type;
        this.actorUsername = actorUsername;
        this.referenceId = referenceId;
        this.description = description;
        this.amount = amount;
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

    public double getAmount() {
        return amount;
    }

    public LocalDateTime getTime() {
        return time;
    }
}
