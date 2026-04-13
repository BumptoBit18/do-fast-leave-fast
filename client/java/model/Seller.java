package model;

public class Seller extends User {
    public Seller(String id, String username, String password) {
        super(id, username, password, "SELLER");
    }

    @Override
    public void printInfo() {
        System.out.println("Người bán: " + username);
    }
}