package app.model;

public class AppUser {
    private final String id;
    private final String username;
    private String password;
    private final UserRole role;
    private final String fullName;
    private double walletBalance;

    public AppUser(String id, String username, String password, UserRole role, String fullName, double walletBalance) {
        this.id = id;
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.walletBalance = walletBalance;
    }

    public String getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public UserRole getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void deposit(double amount) {
        walletBalance += amount;
    }

    public void withdraw(double amount) {
        walletBalance -= amount;
    }
}
