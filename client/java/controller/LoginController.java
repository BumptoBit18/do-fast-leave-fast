package controller;

import app.model.UserRole;
import javafx.application.Platform;
import javafx.fxml.FXML;
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

        Label subtitle = new Label("Ung dung dau gia truc tuyen chuyen nghiep, uy tin");
        subtitle.getStyleClass().add("hero-subtitle");

        HBox badges = new HBox(8,
                AppUi.badge("BIDDER"),
                AppUi.badge("SELLER"),
                AppUi.badge("ADMIN")
        );

        VBox featureList = new VBox(
                10,
                infoRow("BIDDER", "Duyet danh sach, xem chi tiet va dat gia theo thoi gian thuc"),
                infoRow("SELLER", "Tao phien dau gia, quan ly lo dang ban va theo doi gia"),
                infoRow("ADMIN", "Giam sat du lieu he thong, tai khoan va toan bo phien dau gia")
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

        Label loginTitle = new Label("Dang nhap he thong");
        loginTitle.getStyleClass().add("section-title");

        TextField usernameField = new TextField();
        usernameField.setPromptText("Vi du: bidder");

        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Nhap mat khau");

        ComboBox<UserRole> roleBox = new ComboBox<>();
        roleBox.getItems().addAll(UserRole.BIDDER, UserRole.SELLER, UserRole.ADMIN);
        roleBox.setValue(UserRole.BIDDER);
        roleBox.setMaxWidth(Double.MAX_VALUE);

        Button loginButton = new Button("Dang nhap");
        loginButton.getStyleClass().add("primary-button");
        loginButton.setMaxWidth(Double.MAX_VALUE);

        VBox registerForm = new VBox(14);
        registerForm.setPadding(new Insets(28));

        Label registerTitle = new Label("Tao tai khoan moi");
        registerTitle.getStyleClass().add("section-title");

        TextField fullNameField = new TextField();
        fullNameField.setPromptText("Vi du: Nguyen Van A");

        TextField registerUserField = new TextField();
        registerUserField.setPromptText("Tao ten dang nhap moi");

        PasswordField registerPasswordField = new PasswordField();
        registerPasswordField.setPromptText("It nhat 6 ky tu");

        ComboBox<UserRole> registerRoleBox = new ComboBox<>();
        registerRoleBox.getItems().addAll(UserRole.BIDDER, UserRole.SELLER);
        registerRoleBox.setValue(UserRole.BIDDER);
        registerRoleBox.setMaxWidth(Double.MAX_VALUE);

        Button registerButton = new Button("Dang ky");
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
                        AlertUtil.error("Dang nhap that bai", ex.getMessage());
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
                        AlertUtil.info("Dang ky thanh cong", "Tai khoan moi da duoc tao. Ban co the dang nhap ngay.");
                        usernameField.setText(registerUserField.getText().trim());
                        passwordField.setText(registerPasswordField.getText());
                        roleBox.setValue(registerRoleBox.getValue());
                        form.getSelectionModel().select(0);
                    });
                } catch (Exception ex) {
                    Platform.runLater(() -> {
                        setBusy(false, loginButton, registerButton);
                        AlertUtil.error("Dang ky that bai", ex.getMessage());
                    });
                }
            });
        });

        loginForm.getChildren().addAll(
                loginTitle,
                AppUi.fieldGroup("Ten dang nhap", "Dung ten tai khoan da duoc cap hoac da dang ky truoc do.", usernameField),
                AppUi.fieldGroup("Mat khau", "Mat khau phai dung voi tai khoan tuong ung trong he thong.", passwordField),
                AppUi.fieldGroup("Loai tai khoan", "Chon dung vai tro de vao khu vuc nguoi mua, nguoi ban hoac quan tri.", roleBox),
                loginButton
        );

        registerForm.getChildren().addAll(
                registerTitle,
                AppUi.fieldGroup("Ho va ten", "Nhap ho ten day du de he thong hien thi dung thong tin nguoi dung.", fullNameField),
                AppUi.fieldGroup("Ten dang nhap moi", "Ten nay se dung de dang nhap sau nay va khong duoc trung voi nguoi khac.", registerUserField),
                AppUi.fieldGroup("Mat khau", "Mat khau nen de nho voi ban nhung du an toan, toi thieu 6 ky tu.", registerPasswordField),
                AppUi.fieldGroup("Loai tai khoan", "Nguoi mua dung de dau gia, nguoi ban dung de dang lo dau gia.", registerRoleBox),
                registerButton
        );

        form.getTabs().addAll(
                new Tab("Dang nhap", loginForm),
                new Tab("Dang ky", registerForm)
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
        loginButton.setText(busy ? "Dang xu ly..." : "Dang nhap");
        registerButton.setText(busy ? "Dang xu ly..." : "Dang ky");
    }

    private HBox infoRow(String title, String description) {
        Label tag = new Label(title);
        tag.getStyleClass().add("tag");

        Label text = new Label(description);
        text.getStyleClass().add("hero-list");
        text.setWrapText(true);

        VBox body = new VBox(4, tag, text);
        HBox row = new HBox(body);
        row.getStyleClass().add("feature-row");
        return row;
    }
}
