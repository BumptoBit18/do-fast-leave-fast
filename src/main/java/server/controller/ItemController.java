package server.controller;

import server.model.item.Art;
import server.model.item.Electronics;
import server.model.item.GenericItem;
import server.model.item.Item;
import server.model.item.Vehicle;

import java.time.LocalDateTime;

public class ItemController {
    public Item createItem(
            String category,
            String id,
            String title,
            String description,
            double startPrice,
            LocalDateTime endTime,
            String imageHint
    ) {
        return switch (category.toLowerCase()) {
            case "vehicle" -> new Vehicle(id, title, description, startPrice, endTime, imageHint);
            case "art" -> new Art(id, title, description, startPrice, endTime, imageHint);
            case "electronics" -> new Electronics(id, title, description, startPrice, endTime, imageHint);
            default -> new GenericItem(category, id, title, description, startPrice, endTime, imageHint);
        };
    }
}
