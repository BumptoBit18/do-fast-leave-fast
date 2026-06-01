package server.model.item;

import server.model.entity.Entity;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private String name;
    private String description;
    private double startingPrice;
    private LocalDateTime endTime;
    private String imageHint;

    protected Item(
            String id,
            String name,
            String description,
            double startingPrice,
            LocalDateTime endTime,
            String imageHint
    ) {
        super(id);
        this.name = name;
        this.description = description;
        this.startingPrice = startingPrice;
        this.endTime = endTime;
        this.imageHint = imageHint;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }

    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getImageHint() {
        return imageHint;
    }

    public void setImageHint(String imageHint) {
        this.imageHint = imageHint;
    }

    public String getCategory() {
        return getClass().getSimpleName();
    }
}
