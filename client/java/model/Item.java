package model;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    protected String name;
    protected String description;
    protected double startingPrice;
    protected LocalDateTime endTime;

    public Item(String id, String name, String description, double startingPrice, LocalDateTime endTime) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
    }

    public String getName() { return name; }
    public double getStartingPrice() { return startingPrice; }
    public LocalDateTime getEndTime() { return endTime; }
}
