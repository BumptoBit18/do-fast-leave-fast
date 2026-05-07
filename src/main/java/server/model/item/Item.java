package server.model.item;

import server.model.entity.Entity;

import java.time.LocalDateTime;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final String description;
    private final double startingPrice;
    private LocalDateTime endTime;
    private final String imageHint;

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

    public String getDescription() {
        return description;
    }

    public double getStartingPrice() {
        return startingPrice;
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

    public String getCategory() {
        return getClass().getSimpleName();
    }
}
