package server.model.item;

import java.time.LocalDateTime;

public class GenericItem extends Item {
    private static final long serialVersionUID = 1L;

    private final String category;

    public GenericItem(String category, String id, String name, String description, double startingPrice, LocalDateTime endTime, String imageHint) {
        super(id, name, description, startingPrice, endTime, imageHint);
        this.category = category;
    }

    @Override
    public String getCategory() {
        return category;
    }
}
