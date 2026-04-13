package model;

import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String licensePlate;

    public Vehicle(String id, String name, String desc, double price, LocalDateTime end, String licensePlate) {
        super(id, name, desc, price, end);
        this.licensePlate = licensePlate;
    }

    @Override
    public void printInfo() {
        System.out.println("Xe cộ: " + name + " - Biển số: " + licensePlate);
    }
}