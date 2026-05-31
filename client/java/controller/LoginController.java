package controller;

import app.model.UserRole;
import javafx.fxml.FXML;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.ServerConnection;
import ui.AppUi;
import util.AlertUtil;
import util.SceneManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginController {
    private final SceneManager sceneManager;
    private final ServerConnection serverConnection;
    @FXML
    private StackPane root;
    private final ExecutorService authExecutor = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "auth-worker");
        thread.setDaemon(true);
        return thread;
    });

    public LoginController(SceneManager sceneManager, ServerConnection serverConnection) {
        this.sceneManager = sceneManager;
        this.serverConnection = serverConnection;
    }

    @FXML
    private void initialize() {
        root.getChildren().setAll(buildView());
    }

    public Parent getView() {
        return root;
    }

    private Parent buildView() {
        VBox hero = new VBox(16);
        hero.getStyleClass().add("hero-panel");
        hero.setPadding(new Insets(36));
        hero.setPrefWidth(540);
        hero.setMaxWidth(Double.MAX_VALUE);
        hero.setMinHeight(720);

        Label brand = new Label("AUCTION APP");
        brand.getStyleClass().add("hero-title");

        Label subtitle = new Label("Ứng dụng đấu giá trực tuyến chuyên nghiệp, uy tín");
        subtitle.getStyleClass().add("hero-subtitle");

        HBox badges = new HBox(8,
                AppUi.badge("BIDDER"),
                AppUi.badge("SELLER"),
                AppUi.badge("ADMIN")
        );

        VBox featureList = new VBox(
                10,
                infoRow("BIDDER", "Duyệt danh sách, xem chi tiết và đặt giá theo thời gian thực"),
                infoRow("SELLER", "Tạo phiên đấu giá, quản lý lô đang bán và theo dõi giá"),
                infoRow("ADMIN", "Giám sát dữ liệu hệ thống, tài khoản và toàn bộ phiên đấu giá")
        );

        hero.getChildren().addAll(brand, subtitle, badges, featureList);

        TabPane form = new TabPane();
        form.getStyleClass().addAll("card", "auth-card");
        form.setPrefWidth(560);
        form.setMaxWidth(Double.MAX_VALUE);
        form.setMinHeight(720);
        form.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);

        VBox loginForm = new VBox(14);
        loginForm.setPadding(new Insets(28));

        Label loginTitle = new Label("Đăng nhập hệ thống");
        loginTitle.getStyleClass().add("section-title");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Ví dụ: bidder");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Nhập mật khẩu");

        ComboBox<UserRole> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(UserRole.BIDDER, UserRole.SELLER, UserRole.ADMIN);
        roleBox.setValue(UserRole.BIDDER);
        roleBox.setMaxWidth(Double.MAX_VALUE);

        Button loginButton = new Button("Đăng nhập");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        VBox registerForm = new VBox(14);
        registerForm.setPadding(new Insets(28));

        Label registerTitle = new Label("Tạo tài khoản mới");
        registerTitle.getStyleClass().add("section-title");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Ví dụ: Nguyễn Văn A");

        TextField registerUserField = new TextField();
        registerUserField.setPromptText("Tạo tên đăng nhập mới");

        PasswordField registerPasswordField = new PasswordField();
        registerPasswordField.setPromptText("Ít nhất 6 ký tự");

        ComboBox<UserRole> registerRoleBox = new ComboBox<>();
        registerRoleBox.getItems().addAll(UserRole.BIDDER, UserRole.SELLER);
        registerRoleBox.setValue(UserRole.BIDDER);
        registerRoleBox.setMaxWidth(Double.MAX_VALUE);

        Button registerButton = new Button("Đăng ký");
        registerButton.getStyleClass().add("primary-button");
        registerButton.setMaxWidth(Double.MAX_VALUE);

        loginButton.setOnAction(event -> {
            setBusy(true, loginButton, registerButton);
            authExecutor.submit(() -> {
                try {
                    serverConnection.getService().login(
                            usernameField.getText().trim(),
                            passwordField.getText(),
                            roleBox.getValue()
                    );
                    Platform.runLater(() -> {
                        setBusy(false, loginButton, registerButton);
                        switch (roleBox.getValue()) {
                            case SELLER -> sceneManager.showSellerDashboard();
                            case ADMIN -> sceneManager.showAdminPanel();
                            default -> sceneManager.showAuctionList();
                        }
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setBusy(false, loginButton, registerButton);
                        AlertUtil.error("Đăng nhập thất bại", ex.getMessage());
                    });
                }
            });
        });

        registerButton.setOnAction(event -> {
            setBusy(true, loginButton, registerButton);
            authExecutor.submit(() -> {
                try {
                    serverConnection.getService().register(
                            registerUserField.getText().trim(),
                            registerPasswordField.getText(),
                            fullNameField.getText().trim(),
                            registerRoleBox.getValue()
                    );
                    Platform.runLater(() -> {
                        setBusy(false, loginButton, registerButton);
                        AlertUtil.info("Đăng ký thành công", "Tài khoản mới đã được tạo. Bạn có thể đăng nhập ngay.");
                        usernameField.setText(registerUserField.getText().trim());
                        passwordField.setText(registerPasswordField.getText());
                        roleBox.setValue(registerRoleBox.getValue());
                        form.getSelectionModel().select(0);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setBusy(false, loginButton, registerButton);
                        AlertUtil.error("Đăng ký thất bại", ex.getMessage());
                    });
                }
            });
        });

        loginForm.getChildren().addAll(
                loginTitle,
                AppUi.fieldGroup("Tên đăng nhập", "Dùng tên tài khoản đã được cấp hoặc đã đăng ký trước đó.", usernameField),
                AppUi.fieldGroup("Mật khẩu", "Mật khẩu phải đúng với tài khoản tương ứng trong hệ thống.", passwordField),
                AppUi.fieldGroup("Loại tài khoản", "Chọn đúng vai trò để vào khu vực Người mua, Người bán hoặc Quản trị.", roleBox),
                loginButton
        );

        registerForm.getChildren().addAll(
                registerTitle,
                AppUi.fieldGroup("Họ và tên", "Nhập họ tên đầy đủ để hệ thống hiển thị đúng thông tin người dùng.", fullNameField),
                AppUi.fieldGroup("Tên đăng nhập mới", "Tên này sẽ dùng để đăng nhập sau này và không được trùng với người khác.", registerUserField),
                AppUi.fieldGroup("Mật khẩu", "Mật khẩu nên dễ nhớ với bạn nhưng đủ an toàn, tối thiểu 6 ký tự.", registerPasswordField),
                AppUi.fieldGroup("Loại tài khoản", "Người mua dùng để đấu giá, Người bán dùng để đăng lô đấu giá.", registerRoleBox),
                registerButton
        );

        form.getTabs().addAll(
                new Tab("Đăng nhập", loginForm),
                new Tab("Đăng ký", registerForm)
        );

        HBox content = new HBox(28, hero, form);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new Insets(28));
        HBox.setHgrow(hero, Priority.ALWAYS);
        HBox.setHgrow(form, Priority.ALWAYS);
        content.setMaxWidth(1240);
        content.getStyleClass().add("auth-layout");

        StackPane container = new StackPane(content);
        container.getStyleClass().addAll("app-shell", "auth-root");
        container.setPadding(new Insets(24));
        container.setMinSize(1180, 760);
        return container;
    }

    private void setBusy(boolean busy, Button loginButton, Button registerButton) {
        loginButton.setDisable(busy);
        registerButton.setDisable(busy);
        loginButton.setText(busy ? "Đang xử lý..." : "Đăng nhập");
        registerButton.setText(busy ? "Đang xử lý..." : "Đăng ký");
    }

    private HBox infoRow(String title, String description) {
        Label tag = new Label(title);
        tag.getStyleClass().add("tag");

        Label text = new Label(description);
        text.setWrapText(true);

        VBox body = new VBox(4, tag, text);
        HBox row = new HBox(body);
        row.getStyleClass().add("feature-row");
        return row;
    }
}
