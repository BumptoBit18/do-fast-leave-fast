package server.model.item;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private static final long serialVersionUID = 1L;

    public Vehicle(String id, String name, String description, double startingPrice, LocalDateTime endTime, String imageHint) {
        super(id, name, description, startingPrice, endTime, imageHint);
    }
}
