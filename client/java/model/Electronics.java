package model;

import java.time.LocalDateTime;

public class Electronics extends Item {
    private String brand;

    public Electronics(String id, String name, String desc, double price, LocalDateTime end, String brand) {
        super(id, name, desc, price, end);
        this.brand = brand;
    }

    @Override
    public void printInfo() {
        System.out.println("Đồ điện tử: " + name + " - Hãng: " + brand);
    }
}