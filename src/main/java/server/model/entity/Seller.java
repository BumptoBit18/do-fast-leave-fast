package server.model.entity;

public class Seller extends User {
    private static final long serialVersionUID = 1L;

    public Seller(String id, String username, String password, String fullName, double walletBalance) {
        super(id, username, password, "SELLER", fullName, walletBalance);
    }
}
