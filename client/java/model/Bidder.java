package model;

public class Bidder extends User {
    public Bidder(String id, String username, String password) {
        super(id, username, password, "BIDDER");
    }

    @Override
    public void printInfo() {
        System.out.println("Người mua: " + username);
    }
}