package server.model.entity;

public abstract class User extends Entity {
    private static final long serialVersionUID = 1L;

    private final String username;
    private String password;
    private final String role;
    private String fullName;
    private double walletBalance;

    protected User(String id, String username, String password, String role, String fullName, double walletBalance) {
        super(id);
        this.username = username;
        this.password = password;
        this.role = role;
        this.fullName = fullName;
        this.walletBalance = walletBalance;
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

    public String getRole() {
        return role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public double getWalletBalance() {
        return walletBalance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nap phai lon hon 0.");
        }
        walletBalance += amount;
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien rut phai lon hon 0.");
        }
        if (amount > walletBalance) {
            throw new IllegalArgumentException("So du khong du.");
        }
        walletBalance -= amount;
    }
}
