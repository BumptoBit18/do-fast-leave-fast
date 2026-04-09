package client.java.controller;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import client.java.network.ServerConnection;
import client.java.model.UserSession;
import client.java.util.SceneManager;

import java.io.IOException;
import java.util.Map;

public class LoginController {

    @FXML
    private TextField txtUsername;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblMessage;

    @FXML
    private Button btnLogin;

    @FXML
    private Button btnRegister;

    @FXML
    public void initialize() {
        lblMessage.setText("");
    }

    @FXML
    private void handleLogin(ActionEvent event) {
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText().trim();

        if (username.isEmpty() || password.isEmpty()) {
            lblMessage.setText("Vui lòng nhập đầy đủ username và password.");
            return;
        }

        try {
            Map<String, Object> response = ServerConnection.getInstance().sendRequest(
                    Map.of(
                            "action", "login",
                            "username", username,
                            "password", password
                    )
            );

            boolean success = (boolean) response.get("success");

            if (success) {
                String userId = (String) response.get("userId");
                String role = (String) response.get("role");
                String fullName = (String) response.get("fullName");

                UserSession.getInstance().setUser(userId, username, fullName, role);

                lblMessage.setText("Đăng nhập thành công!");

                if ("ADMIN".equalsIgnoreCase(role)) {
                    SceneManager.switchScene("/client/view/fxml/admin_panel.fxml");
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    SceneManager.switchScene("/client/view/fxml/seller_dashboard.fxml");
                } else {
                    SceneManager.switchScene("/client/view/fxml/auction_list.fxml");
                }
            } else {
                lblMessage.setText((String) response.get("message"));
            }

        } catch (IOException e) {
            lblMessage.setText("Không thể kết nối tới server.");
            e.printStackTrace();
        }
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        lblMessage.setText("Chức năng đăng ký sẽ làm sau.");
    }
}