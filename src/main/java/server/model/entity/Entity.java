package server.model.entity;

import java.io.Serializable;

public abstract class Entity implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String id;

    protected Entity(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }
}
