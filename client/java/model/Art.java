package model;

import java.time.LocalDateTime;

public class Art extends Item {
    public Art(String id, String name, String desc, double price, LocalDateTime end) {
        super(id, name, desc, price, end);
    }

    @Override
    public void printInfo() {
        System.out.println("Tác phẩm nghệ thuật: " + name);
    }
}