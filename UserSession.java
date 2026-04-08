package model;

public class UserSession {
    private static UserSession instance;

    private String username;
    private String role; // Admin, Seller, hoặc Bidder [cite: 18]

    private UserSession(String username, String role) {
        this.username = username;
        this.role = role;
    }

    public static void login(String username, String role) {
        instance = new UserSession(username, role);
    }

    public static UserSession getInstance() {
        return instance;
    }

    public static void logout() {
        instance = null;
    }

    public String getUsername() { return username; }
    public String getRole() { return role; }
}