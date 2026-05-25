package server.controller;

import server.ServerMain;
import server.model.BidTransaction;
import server.model.NotificationRecord;
import server.model.TopUpRequestRecord;
import server.model.entity.Bidder;
import server.model.entity.Seller;
import server.model.entity.User;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

public class UserController {
    private final ServerMain server;

    public UserController(ServerMain server) {
        this.server = server;
    }

    public synchronized User login(String username, String password, String role) {
        if (username == null || username.isBlank() || password == null || password.isBlank() || role == null) {
            throw new IllegalArgumentException("Vui long nhap day du thong tin dang nhap.");
        }
        User user = server.findUserByCredentials(username, password, role);
        if (user == null) {
            throw new IllegalArgumentException("Sai username, password hoac role.");
        }
        return user;
    }

    public synchronized User register(String username, String password, String fullName, String role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username khong duoc de trong.");
        }
        if (username.trim().length() < 3) {
            throw new IllegalArgumentException("Username phai tu 3 ky tu tro len.");
        }
        if (!username.matches("^[a-zA-Z0-9 ]*$")) {
            throw new IllegalArgumentException("Username chi duoc chua chu cai hoac so.");
        }
        if (server.usernameExists(username)) {
            throw new IllegalArgumentException("Username da ton tai.");
        }
        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException("Password phai tu 6 ky tu tro len.");
        }
        if (fullName == null || fullName.isBlank()) {
            throw new IllegalArgumentException("Ho ten khong duoc de trong.");
        }

        String normalizedRole = role.toUpperCase(Locale.ROOT);
        User user = switch (normalizedRole) {
            case "SELLER" -> new Seller(buildId(), username, password, fullName, 0);
            case "BIDDER" -> new Bidder(buildId(), username, password, fullName, 0);
            default -> throw new IllegalArgumentException("Chi cho phep dang ky voi vai tro BIDDER hoac SELLER.");
        };

        server.addUser(user);
        appendTransaction("REGISTER", user.getUsername(), user.getId(), "Tao tai khoan moi", 0);
        appendNotification(user.getUsername(), "Chao mung", "Tai khoan cua ban da duoc tao.");
        return user;
    }

    public synchronized User topUpWallet(String username, double amount) {
        throw new IllegalStateException("Nap tien vao vi phai cho admin xac nhan.");
    }

    public synchronized TopUpRequestRecord submitTopUpRequest(
            String username,
            double amount,
            String bankName,
            String accountName,
            String accountNumber
    ) {
        if (amount <= 0) {
            throw new IllegalArgumentException("So tien nap phai lon hon 0.");
        }

        if (server.findUserByUsername(username) == null){
            throw new IllegalArgumentException("Khong tim thay nguoi dung ten " + username);
        }

        TopUpRequestRecord request = new TopUpRequestRecord(
                "TOPUP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT),
                username,
                amount,
                bankName,
                accountName,
                accountNumber,
                LocalDateTime.now(),
                "PENDING",
                null,
                null,
                null
        );

        server.addTopUpRequest(request);
        appendTransaction("TOP_UP_REQUEST", username, request.getId(), "Gui yeu cau nap tien vao vi", amount);
        appendNotification(username, "Yeu cau nap tien da gui", "Yeu cau nap tien cua ban dang cho admin xac nhan.");
        appendNotification("admin", "Co yeu cau nap tien moi", username + " vua gui yeu cau nap " + amount + ".");
        return request;
    }

    public synchronized User approveTopUpRequest(String requestId, String adminUsername) {
        List<TopUpRequestRecord> requests = server.getTopUpRequests();
        TopUpRequestRecord request = requests.stream()
                .filter(item -> item.getId().equalsIgnoreCase(requestId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay yeu cau nap tien " + requestId));

        if (!"PENDING".equalsIgnoreCase(request.getStatus())) {
            throw new IllegalStateException("Yeu cau nay da duoc xu ly.");
        }

        request.approve(adminUsername, LocalDateTime.now());
        server.markTopUpApproved(request);
        appendTransaction("TOP_UP_APPROVED", adminUsername, request.getId(), "Admin xac nhan yeu cau nap tien", request.getAmount());
        appendNotification(request.getUsername(), "Yeu cau da duoc duyet", "Yeu cau nap tien da duoc duyet. He thong se cong tien sau khoang 10 giay.");

        return server.getUsers().stream()
                .filter(candidate -> candidate.getUsername().equalsIgnoreCase(adminUsername))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Khong tim thay admin " + adminUsername));
    }

    public synchronized List<TopUpRequestRecord> getTopUpRequests() {
        return server.getTopUpRequests();
    }

    public synchronized void processApprovedTopUpCredits() {
        LocalDateTime now = LocalDateTime.now();
        List<TopUpRequestRecord> requests = server.getTopUpRequests();
        List<User> users = server.getUsers();

        for (TopUpRequestRecord request : requests) {
            if (!"APPROVED".equalsIgnoreCase(request.getStatus()) || request.getApprovedAt() == null) {
                continue;
            }
            if (request.getApprovedAt().plusSeconds(10).isAfter(now)) {
                continue;
            }

            User user = users.stream()
                    .filter(candidate -> candidate.getUsername().equalsIgnoreCase(request.getUsername()))
                    .findFirst()
                    .orElse(null);
            if (user == null) {
                continue;
            }

            user.deposit(request.getAmount());
            request.markCredited(now);
            server.updateUserWallet(user);
            server.markTopUpCredited(request);
            server.addTransaction(new BidTransaction(
                    "TOP_UP_CREDITED",
                    "SYSTEM",
                    request.getId(),
                    "Cong tien vao vi sau do tre 10 giay",
                    request.getAmount(),
                    now
            ));
            server.addNotification(new NotificationRecord(
                    request.getUsername(),
                    "Nap tien thanh cong",
                    "So du vi cua ban da duoc cong " + request.getAmount() + " sau khi admin duyet.",
                    now
            ));
        }
    }

    private void appendTransaction(String type, String actor, String ref, String description, double amount) {
        server.addTransaction(new BidTransaction(type, actor, ref, description, amount, LocalDateTime.now()));
    }

    private void appendNotification(String username, String title, String message) {
        server.addNotification(new NotificationRecord(username, title, message, LocalDateTime.now()));
    }

    private String buildId() {
        return "U-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(Locale.ROOT);
    }
}
