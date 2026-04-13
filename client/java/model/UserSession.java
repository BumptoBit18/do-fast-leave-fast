package model;

/**
 * Quản lý phiên đăng nhập của người dùng (Singleton-like pattern).
 */
public class UserSession {
    // Lưu đối tượng User (có thể là Admin, Bidder hoặc Seller) [cite: 34, 115]
    private static User loggedInUser;

    // Thiết lập người dùng khi đăng nhập thành công
    public static void setInstance(User user) {
        loggedInUser = user;
    }

    // Lấy thông tin người dùng hiện tại để kiểm tra quyền hạn [cite: 32]
    public static User getInstance() {
        return loggedInUser;
    }

    // Xóa phiên đăng nhập khi người dùng đăng xuất
    public static void cleanUserSession() {
        loggedInUser = null;
    }

    // Kiểm tra nhanh xem có phải Admin không [cite: 37]
    public static boolean isAdmin() {
        return loggedInUser instanceof Admin;
    }
}