package server.controller;

import org.junit.jupiter.api.Test;
import server.model.item.Art;
import server.model.item.Electronics;
import server.model.item.GenericItem;
import server.model.item.Item;
import server.model.item.Vehicle;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemControllerTest {
    private final ItemController controller = new ItemController();

    @Test
    void shouldCreateSpecializedItemForKnownCategory() {
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 14, 10, 0);

        Item electronics = controller.createItem("electronics", "A1", "Laptop", "Gaming", 1500, endTime, "black");
        Item vehicle = controller.createItem("vehicle", "A2", "Car", "Sedan", 20000, endTime, "white");
        Item art = controller.createItem("art", "A3", "Painting", "Oil", 5000, endTime, "canvas");

        assertInstanceOf(Electronics.class, electronics);
        assertInstanceOf(Vehicle.class, vehicle);
        assertInstanceOf(Art.class, art);
    }

    @Test
    void shouldCreateGenericItemForUnknownCategory() {
        LocalDateTime endTime = LocalDateTime.of(2026, 5, 14, 10, 0);

        Item item = controller.createItem("Collectible", "A4", "Coin", "Rare coin", 1200, endTime, "gold");

        GenericItem genericItem = assertInstanceOf(GenericItem.class, item);
        assertEquals("Collectible", genericItem.getCategory());
    }
}
