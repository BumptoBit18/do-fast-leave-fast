package server.model.entity;

public class Bidder extends User {
    private static final long serialVersionUID = 1L;

    public Bidder(String id, String username, String password, String fullName, double walletBalance) {
        super(id, username, password, "BIDDER", fullName, walletBalance);
    }
}
