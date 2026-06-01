package controller;

import app.model.AppUser;
import app.model.NotificationItem;
import app.service.AuctionPlatformService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import network.MessageListener;
import network.ServerConnection;
import shared.socket.RealtimeEvent;
import ui.AppUi;
import util.AlertUtil;
import util.SceneManager;

import java.util.List;

public class WalletController implements MessageListener {
    private static final String QR_IMAGE_URL = "https://i.postimg.cc/hvGBx7Zc/778942ef-f12e-4d94-8ef6-21f06a49242d.jpg";
    private static final String BANK_NAME = "MB Bank";
    private static final String ACCOUNT_NAME = "NGUYEN TRONG HUNG";
    private static final String ACCOUNT_NUMBER = "0399858007";
    private static final Image QR_IMAGE = new Image(QR_IMAGE_URL, true);

    private final SceneManager sceneManager;
    private final ServerConnection serverConnection;
    private final AuctionPlatformService service;
    private double lastKnownBalance = Double.NaN;
    private Label balanceValue;
    private Label roleValue;
    private Label userValue;
    private ListView<String> notificationList;
    @FXML
    private StackPane root;

    public WalletController(SceneManager sceneManager, ServerConnection serverConnection) {
        this.sceneManager = sceneManager;
        this.serverConnection = serverConnection;
        this.service = serverConnection.getService();
    }

    @FXML
    private void initialize() {
        serverConnection.addMessageListener(this);
        root.sceneProperty().addListener((observable, oldScene, newScene) -> {
            if (newScene == null) {
                serverConnection.removeMessageListener(this);
            }
        });
        root.getChildren().setAll(buildView());
        refreshWalletView(false);
    }

    private Parent buildView() {
        BorderPane shell = new BorderPane();
        shell.getStyleClass().add("app-shell");
        shell.setPadding(new Insets(24));

        AppUser currentUser = service.getCurrentUser();

        Button marketButton = new Button("Cho dau gia");
        marketButton.setOnAction(event -> sceneManager.showAuctionList());

        Button sellerButton = new Button("Khu nguoi ban");
        sellerButton.getStyleClass().add("secondary-button");
        boolean canOpenSeller = currentUser.getRole().name().equals("SELLER") || currentUser.getRole().name().equals("ADMIN");
        sellerButton.setVisible(canOpenSeller);
        sellerButton.setManaged(canOpenSeller);
        sellerButton.setOnAction(event -> sceneManager.showSellerDashboard());

        Button adminButton = new Button("Quan tri he thong");
        adminButton.getStyleClass().add("secondary-button");
        boolean isAdmin = currentUser.getRole().name().equals("ADMIN");
        adminButton.setVisible(isAdmin);
        adminButton.setManaged(isAdmin);
        adminButton.setOnAction(event -> sceneManager.showAdminPanel());

        Button refreshButton = new Button("Lam moi");
        refreshButton.getStyleClass().add("secondary-button");

        Button logoutButton = new Button("Dang xuat");
        logoutButton.setOnAction(event -> sceneManager.logout());

        balanceValue = new Label();
        balanceValue.getStyleClass().add("stat-value");
        roleValue = new Label();
        roleValue.getStyleClass().add("muted-label");
        userValue = new Label();
        userValue.getStyleClass().add("muted-label");

        notificationList = new ListView<>();
        notificationList.setPrefHeight(180);

        VBox accountPanel = AppUi.panelCard(
                "Vi cua ban",
                "So du duoc cap nhat ngay khi server co thay doi.",
                AppUi.fieldGroup("So du hien tai", "Cap nhat sau khi admin xac nhan yeu cau nap tien.", balanceValue),
                AppUi.fieldGroup("Vai tro", "Quyen truy cap hien tai.", roleValue),
                AppUi.fieldGroup("Nguoi dung", "Chu tai khoan dang dang nhap.", userValue),
                new Label("Thong bao gan day"),
                notificationList,
                refreshButton
        );
        accountPanel.setPrefWidth(360);

        ComboBox<String> paymentMethodBox = new ComboBox<>();
        paymentMethodBox.getItems().add("Chuyen khoan ngan hang");
        paymentMethodBox.setValue("Chuyen khoan ngan hang");
        paymentMethodBox.setMaxWidth(Double.MAX_VALUE);

        TextField amountField = new TextField();
        amountField.setPromptText("Vi du: 500000");

        Button confirmTopUpButton = new Button("Gui yeu cau xac nhan");
        confirmTopUpButton.getStyleClass().add("primary-button");
        confirmTopUpButton.setMaxWidth(Double.MAX_VALUE);
        confirmTopUpButton.setOnAction(event -> {
            try {
                double amount = Double.parseDouble(amountField.getText().trim());
                service.submitTopUpRequest(amount, BANK_NAME, ACCOUNT_NAME, ACCOUNT_NUMBER);
                service.markCurrentNotificationsSeen();
                AlertUtil.info("Da gui yeu cau", "Admin se xac nhan de cong tien vao vi.");
                refreshWalletView(false);
            } catch (NumberFormatException ex) {
                AlertUtil.error("So tien khong hop le", "Hay nhap so tien hop le.");
            } catch (Exception ex) {
                AlertUtil.error("Khong the gui yeu cau", ex.getMessage());
            }
        });

        VBox transferPanel = AppUi.panelCard(
                "Nap tien vao vi",
                "Client nhan cap nhat realtime tu server thay cho polling.",
                AppUi.fieldGroup("Phuong thuc nap", "He thong dang ho tro nap qua chuyen khoan ngan hang.", paymentMethodBox),
                AppUi.fieldGroup("So tien muon nap", "Nhap dung so tien ban muon chuyen.", amountField),
                buildQrView(),
                buildBankInfo(),
                confirmTopUpButton
        );
        HBox.setHgrow(transferPanel, Priority.ALWAYS);

        HBox body = new HBox(20, accountPanel, transferPanel);
        body.setAlignment(Pos.TOP_LEFT);

        refreshButton.setOnAction(event -> refreshWalletView(true));

        shell.setCenter(new VBox(
                18,
                AppUi.pageHeader(
                        "Vi tien",
                        "Nap tien vao tai khoan",
                        "Thong tin vi va thong bao duoc dong bo theo su kien tu server.",
                        AppUi.badge("Realtime"),
                        AppUi.badge("QR ngan hang"),
                        marketButton,
                        sellerButton,
                        adminButton,
                        refreshButton,
                        logoutButton
                ),
                body
        ));
        return shell;
    }

    private void refreshWalletView(boolean showPopupOnApproval) {
        if (balanceValue == null || roleValue == null || userValue == null || notificationList == null) {
            return;
        }
        AppUser refreshedUser = service.getCurrentUser();
        double previousBalance = lastKnownBalance;
        double currentBalance = refreshedUser.getWalletBalance();

        balanceValue.setText(service.formatCurrency(currentBalance));
        roleValue.setText(refreshedUser.getRole().name());
        userValue.setText(refreshedUser.getFullName());

        List<NotificationItem> notifications = service.getNotificationsForCurrentUser();
        notificationList.getItems().setAll(
                notifications.stream()
                        .limit(6)
                        .map(item -> item.getTitle() + " - " + item.getMessage())
                        .toList()
        );

        List<NotificationItem> newNotifications = service.getNewNotificationsForCurrentUser();
        if (showPopupOnApproval) {
            newNotifications.stream()
                    .filter(this::isApprovalNotification)
                    .findFirst()
                    .ifPresentOrElse(
                            item -> AlertUtil.info("Nap tien thanh cong", item.getMessage()),
                            () -> {
                                if (!Double.isNaN(previousBalance) && currentBalance > previousBalance) {
                                    AlertUtil.info(
                                            "Nap tien thanh cong",
                                            "So du vi cua ban da tang len " + service.formatCurrency(currentBalance) + "."
                                    );
                                }
                            }
                    );
        }

        lastKnownBalance = currentBalance;
    }

    private Parent buildQrView() {
        ImageView imageView = new ImageView(QR_IMAGE);
        imageView.setFitWidth(340);
        imageView.setPreserveRatio(true);
        imageView.getStyleClass().add("qr-image");

        Label qrLabel = new Label("Quet ma QR de chuyen khoan");
        qrLabel.getStyleClass().add("section-title");

        VBox qrBox = new VBox(14, qrLabel, imageView);
        qrBox.getStyleClass().add("qr-panel");
        qrBox.setAlignment(Pos.CENTER);
        return qrBox;
    }

    private VBox buildBankInfo() {
        VBox infoPanel = AppUi.panelCard(
                "Thong tin tai khoan nhan tien",
                "Co the sao chep nhanh thong tin neu khong dung QR."
        );
        infoPanel.getChildren().addAll(
                copyField("Ngan hang", BANK_NAME),
                copyField("Chu tai khoan", ACCOUNT_NAME),
                copyField("So tai khoan", ACCOUNT_NUMBER)
        );
        return infoPanel;
    }

    private HBox copyField(String labelText, String value) {
        Label titleLabel = new Label(labelText);
        titleLabel.getStyleClass().add("field-label");

        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("muted-label");

        VBox textBox = new VBox(4, titleLabel, valueLabel);

        Button copyButton = new Button("Sao chep");
        copyButton.getStyleClass().add("secondary-button");
        copyButton.setOnAction(event -> {
            ClipboardContent content = new ClipboardContent();
            content.putString(value);
            Clipboard.getSystemClipboard().setContent(content);
            AlertUtil.info("Da sao chep", labelText + " da duoc sao chep vao bo nho tam.");
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        HBox row = new HBox(12, textBox, spacer, copyButton);
        row.getStyleClass().add("copy-row");
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }

    private boolean isApprovalNotification(NotificationItem item) {
        String title = item.getTitle() == null ? "" : item.getTitle().toLowerCase();
        String message = item.getMessage() == null ? "" : item.getMessage().toLowerCase();
        return title.contains("duyet")
                || title.contains("nap tien")
                || message.contains("duyet")
                || message.contains("so du")
                || message.contains("cong");
    }

    @Override
    public void onMessage(RealtimeEvent event) {
        if (root != null) {
            Platform.runLater(() -> refreshWalletView(true));
        }
    }
}
