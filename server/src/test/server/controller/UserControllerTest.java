package server.controller;

import org.junit.jupiter.api.Test;
import server.ServerMain;
import server.exception.AuthenticationException;
import server.exception.AuthorizationException;
import server.model.BidTransaction;
import server.model.NotificationRecord;
import server.model.TopUpRequestRecord;
import server.model.entity.Admin;
import server.model.entity.Bidder;
import server.model.entity.Seller;
import server.model.entity.User;

import java.time.LocalDateTime;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UserControllerTest {
    @Test
    void shouldUpdateWalletBalanceForUserEntities() {
        User bidder = new Bidder("U-1", "bidder", "bidder123", "Bidder", 1_000_000);
        User seller = new Seller("U-2", "seller", "seller123", "Seller", 500_000);

        bidder.deposit(250_000);
        seller.withdraw(100_000);

        assertEquals(1_250_000, bidder.getWalletBalance());
        assertEquals(400_000, seller.getWalletBalance());
    }

    @Test
    void shouldRejectInvalidWalletOperations() {
        User bidder = new Bidder("U-3", "bidder", "bidder123", "Bidder", 1_000_000);

        assertThrows(IllegalArgumentException.class, () -> bidder.deposit(0));
        assertThrows(IllegalArgumentException.class, () -> bidder.deposit(-100_000));
        assertThrows(IllegalArgumentException.class, () -> bidder.withdraw(0));
        assertThrows(IllegalArgumentException.class, () -> bidder.withdraw(1_100_000));
        assertEquals(1_000_000, bidder.getWalletBalance());
    }

    @Test
    void shouldLoginRegisterAndManageUserLifecycle() {
        ArrayList<User> users = new ArrayList<>();
        users.add(new Admin("U-0", "admin", "admin123", "Administrator", 0));
        users.add(new Bidder("U-1", "bidder", "bidder123", "Bidder", 200_000));
        ArrayList<BidTransaction> transactions = new ArrayList<>();
        ArrayList<NotificationRecord> notifications = new ArrayList<>();
        ServerMain server = ControllerTestSupport.newServer(
                users,
                new ArrayList<>(),
                transactions,
                new ArrayList<>(),
                notifications,
                new ArrayList<>()
        );
        UserController controller = new UserController(server);

        User loggedIn = controller.login("bidder", "bidder123", "BIDDER");
        assertEquals("bidder", loggedIn.getUsername());

        User created = controller.register("seller2", "seller123", "Seller Two", "SELLER");
        assertEquals("seller2", created.getUsername());
        assertEquals(3, users.size());
        assertEquals("REGISTER", transactions.getFirst().getType());
        assertEquals("Chao mung", notifications.getFirst().getTitle());

        User updated = controller.updateUser("seller2", "Seller 02", "seller456");
        assertEquals("Seller 02", updated.getFullName());
        assertEquals("seller456", updated.getPassword());

        controller.deleteUser("seller2");
        assertEquals(2, users.size());
        assertTrue(transactions.stream().anyMatch(item -> item.getType().equals("DELETE_USER")));
    }

    @Test
    void shouldRejectInvalidLoginAndRegistrationRequests() {
        ArrayList<User> users = new ArrayList<>();
        users.add(new Admin("U-0", "admin", "admin123", "Administrator", 0));
        users.add(new Bidder("U-1", "bidder", "bidder123", "Bidder", 200_000));
        UserController controller = new UserController(ControllerTestSupport.newServer(
                users,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        ));

        assertThrows(AuthenticationException.class, () -> controller.login("", "pw", "BIDDER"));
        assertThrows(AuthenticationException.class, () -> controller.login("bidder", "wrong", "BIDDER"));
        assertThrows(IllegalArgumentException.class, () -> controller.register("bidder", "bidder123", "Copy", "BIDDER"));
        assertThrows(IllegalArgumentException.class, () -> controller.register("newbie", "123", "New User", "BIDDER"));
        assertThrows(IllegalArgumentException.class, () -> controller.register("newbie", "123456", "", "BIDDER"));
        assertThrows(IllegalArgumentException.class, () -> controller.register("admin2", "123456", "Admin 2", "ADMIN"));
        assertThrows(AuthorizationException.class, () -> controller.deleteUser("admin"));
    }

    @Test
    void shouldSubmitApproveAndCreditTopUpRequests() {
        ArrayList<User> users = new ArrayList<>();
        users.add(new Admin("U-0", "admin", "admin123", "Administrator", 0));
        users.add(new Bidder("U-1", "bidder", "bidder123", "Bidder", 500_000));
        ArrayList<BidTransaction> transactions = new ArrayList<>();
        ArrayList<NotificationRecord> notifications = new ArrayList<>();
        ArrayList<TopUpRequestRecord> requests = new ArrayList<>();
        UserController controller = new UserController(ControllerTestSupport.newServer(
                users,
                new ArrayList<>(),
                transactions,
                new ArrayList<>(),
                notifications,
                requests
        ));

        TopUpRequestRecord request = controller.submitTopUpRequest("bidder", 1_500_000, "VCB", "Bidder", "123456");
        assertNotNull(request.getId());
        assertEquals("PENDING", request.getStatus());
        assertEquals(1, requests.size());

        User admin = controller.approveTopUpRequest(request.getId(), "admin");
        assertEquals("admin", admin.getUsername());
        assertEquals("APPROVED", request.getStatus());
        assertTrue(notifications.stream().anyMatch(item -> item.getTitle().contains("duyet")));

        request.approve("admin", LocalDateTime.now().minusSeconds(11));
        controller.processApprovedTopUpCredits();

        User bidder = users.stream()
                .filter(item -> item.getUsername().equals("bidder"))
                .findFirst()
                .orElseThrow();
        assertEquals(2_000_000, bidder.getWalletBalance());
        assertEquals("CREDITED", request.getStatus());
        assertTrue(transactions.stream().anyMatch(item -> item.getType().equals("TOP_UP_CREDITED")));
    }

    @Test
    void shouldWithdrawWalletImmediatelyAndLogTransaction() {
        ArrayList<User> users = new ArrayList<>();
        users.add(new Bidder("U-1", "bidder", "bidder123", "Bidder", 2_500_000));
        ArrayList<BidTransaction> transactions = new ArrayList<>();
        ArrayList<NotificationRecord> notifications = new ArrayList<>();
        UserController controller = new UserController(ControllerTestSupport.newServer(
                users,
                new ArrayList<>(),
                transactions,
                new ArrayList<>(),
                notifications,
                new ArrayList<>()
        ));

        User updated = controller.withdrawWallet("bidder", 700_000, "VCB", "123456789");

        assertEquals(1_800_000, updated.getWalletBalance());
        assertEquals(1_800_000, users.getFirst().getWalletBalance());
        assertTrue(transactions.stream().anyMatch(item -> item.getType().equals("WITHDRAW")));
        assertTrue(notifications.stream().anyMatch(item -> item.getTitle().contains("Rut tien")));
    }

    @Test
    void shouldRejectInvalidWithdrawRequests() {
        ArrayList<User> users = new ArrayList<>();
        users.add(new Bidder("U-1", "bidder", "bidder123", "Bidder", 300_000));
        UserController controller = new UserController(ControllerTestSupport.newServer(
                users,
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>(),
                new ArrayList<>()
        ));

        assertThrows(IllegalArgumentException.class, () -> controller.withdrawWallet("bidder", 0, "VCB", "123"));
        assertThrows(IllegalArgumentException.class, () -> controller.withdrawWallet("bidder", 100_000, "", "123"));
        assertThrows(IllegalArgumentException.class, () -> controller.withdrawWallet("bidder", 100_000, "VCB", ""));
        assertThrows(IllegalArgumentException.class, () -> controller.withdrawWallet("bidder", 500_000, "VCB", "123"));
    }
}
