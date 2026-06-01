package controller;

import app.model.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import network.ServerConnection;
import util.AlertUtil;
import util.SceneManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginController {
    private final SceneManager sceneManager;
    private final ServerConnection serverConnection;
    private final ExecutorService authExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "auth-worker");
        thread.setDaemon(true);
        return thread;
    });

    @FXML
    private TabPane authTabs;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private ComboBox<UserRole> roleBox;
    @FXML
    private TextField fullNameField;
    @FXML
    private TextField registerUserField;
    @FXML
    private PasswordField registerPasswordField;
    @FXML
    private ComboBox<UserRole> registerRoleBox;
    @FXML
    private Button loginButton;
    @FXML
    private Button registerButton;

    public LoginController(SceneManager sceneManager, ServerConnection serverConnection) {
        this.sceneManager = sceneManager;
        this.serverConnection = serverConnection;
    }

    @FXML
    private void initialize() {
        roleBox.getItems().setAll(UserRole.BIDDER, UserRole.SELLER, UserRole.ADMIN);
        roleBox.setValue(UserRole.BIDDER);
        registerRoleBox.getItems().setAll(UserRole.BIDDER, UserRole.SELLER);
        registerRoleBox.setValue(UserRole.BIDDER);
    }

    @FXML
    private void handleLogin() {
        setBusy(true);
        authExecutor.submit(() -> {
            try {
                serverConnection.getService().login(
                        usernameField.getText().trim(),
                        passwordField.getText(),
                        roleBox.getValue()
                );
                Platform.runLater(() -> {
                    setBusy(false);
                    switch (roleBox.getValue()) {
                        case SELLER -> sceneManager.showSellerDashboard();
                        case ADMIN -> sceneManager.showAdminPanel();
                        default -> sceneManager.showAuctionList();
                    }
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setBusy(false);
                    AlertUtil.error("Dang nhap that bai", ex.getMessage());
                });
            }
        });
    }

    @FXML
    private void handleRegister() {
        setBusy(true);
        authExecutor.submit(() -> {
            try {
                serverConnection.getService().register(
                        registerUserField.getText().trim(),
                        registerPasswordField.getText(),
                        fullNameField.getText().trim(),
                        registerRoleBox.getValue()
                );
                Platform.runLater(() -> {
                    setBusy(false);
                    AlertUtil.info("Dang ky thanh cong", "Tai khoan moi da duoc tao. Ban co the dang nhap ngay.");
                    usernameField.setText(registerUserField.getText().trim());
                    passwordField.setText(registerPasswordField.getText());
                    roleBox.setValue(registerRoleBox.getValue());
                    authTabs.getSelectionModel().select(0);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    setBusy(false);
                    AlertUtil.error("Dang ky that bai", ex.getMessage());
                });
            }
        });
    }

    private void setBusy(boolean busy) {
        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        loginButton.setText(busy ? "Dang xu ly..." : "Dang nhap");
        registerButton.setText(busy ? "Dang xu ly..." : "Dang ky");
    }
}
