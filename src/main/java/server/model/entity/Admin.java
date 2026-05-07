package server.model.entity;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin(String id, String username, String password, String fullName, double walletBalance) {
        super(id, username, password, "ADMIN", fullName, walletBalance);
    }
}
